import json
import os
import random
from typing import Any, List, Optional, Iterator

import httpx
from langchain_core.callbacks import CallbackManagerForLLMRun
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from langchain_core.outputs import ChatGeneration, ChatResult
from pydantic import Field


class MiniMaxChatModel(BaseChatModel):
    model: str = Field(default="m2.7")
    api_key: str = Field(default_factory=lambda: os.getenv("MINIMAX_API_KEY", ""))
    group_id: str = Field(default_factory=lambda: os.getenv("MINIMAX_GROUP_ID", ""))
    base_url: str = Field(default="https://api.minimax.chat/v1/text/chatcompletion_v2")
    temperature: float = Field(default=0.7)
    max_tokens: int = Field(default=512)
    timeout: float = Field(default=30.0)

    @property
    def _llm_type(self) -> str:
        return "minimax"

    def _convert_messages(self, messages: List[BaseMessage]) -> List[dict]:
        role_map = {
            SystemMessage: "system",
            HumanMessage: "user",
            AIMessage: "assistant",
        }
        result = []
        for msg in messages:
            role = role_map.get(type(msg), "user")
            result.append({"role": role, "content": msg.content})
        return result

    def _mock_response(self) -> AIMessage:
        actions = ["move", "eat", "rest", "talk"]
        targets = ["D", "E", "F", "G", "H", "A", "B", "C"]
        action = random.choice(actions)
        target = random.choice(targets)
        content = json.dumps({
            "action": action,
            "target": target,
            "reasoning": f"[Mock模式] 随机决策: {action} -> {target}"
        }, ensure_ascii=False)
        return AIMessage(content=content)

    def _generate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> ChatResult:
        if not self.api_key:
            ai_msg = self._mock_response()
            return ChatResult(generations=[ChatGeneration(message=ai_msg)])

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": self.model,
            "messages": self._convert_messages(messages),
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
        }

        if stop:
            payload["stop"] = stop

        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    f"{self.base_url}?GroupId={self.group_id}",
                    headers=headers,
                    json=payload,
                )
                if response.status_code == 200:
                    data = response.json()
                    content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                    ai_msg = AIMessage(content=content)
                else:
                    ai_msg = self._mock_response()
        except Exception:
            ai_msg = self._mock_response()

        return ChatResult(generations=[ChatGeneration(message=ai_msg)])


def create_minimax_chat_model(
    model: str = "m2.7",
    temperature: float = 0.7,
    max_tokens: int = 512,
) -> MiniMaxChatModel:
    return MiniMaxChatModel(
        model=model,
        temperature=temperature,
        max_tokens=max_tokens,
    )

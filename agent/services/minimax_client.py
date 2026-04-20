import os
import httpx
import json
from typing import Optional

MINIMAX_API_KEY = os.getenv("MINIMAX_API_KEY", "")
MINIMAX_GROUP_ID = os.getenv("MINIMAX_GROUP_ID", "")
MINIMAX_BASE_URL = "https://api.minimax.chat/v1/text/chatcompletion_v2"


async def call_minimax(
    system_prompt: str,
    user_prompt: str,
    model: str = "m2.7",
    temperature: float = 0.7,
    max_tokens: int = 512,
) -> Optional[str]:
    if not MINIMAX_API_KEY:
        return _mock_response(user_prompt)

    headers = {
        "Authorization": f"Bearer {MINIMAX_API_KEY}",
        "Content-Type": "application/json",
    }

    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": temperature,
        "max_tokens": max_tokens,
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{MINIMAX_BASE_URL}?GroupId={MINIMAX_GROUP_ID}",
                headers=headers,
                json=payload,
            )
            if response.status_code == 200:
                data = response.json()
                return data.get("choices", [{}])[0].get("message", {}).get("content", "")
            else:
                print(f"[MiniMax API] 请求失败: {response.status_code} - {response.text}")
                return _mock_response(user_prompt)
    except Exception as e:
        print(f"[MiniMax API] 调用异常: {e}")
        return _mock_response(user_prompt)


def _mock_response(user_prompt: str) -> str:
    import random
    actions = ["move", "eat", "rest", "talk"]
    targets = ["center", "forest", "river", "mountain", "base"]
    action = random.choice(actions)
    target = random.choice(targets)
    return json.dumps({
        "action": action,
        "target": target,
        "reasoning": f"[Mock模式] 随机决策: {action} -> {target}"
    }, ensure_ascii=False)

from typing import List, Dict, Optional, Any
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage
from pydantic import BaseModel, Field


class InMemoryChatHistory(BaseChatMessageHistory, BaseModel):
    messages: List[BaseMessage] = Field(default_factory=list)
    
    def add_messages(self, messages: List[BaseMessage]) -> None:
        self.messages.extend(messages)
    
    def clear(self) -> None:
        self.messages.clear()


class AgentMemoryManager:
    def __init__(self, agent_id: int, max_ticks: int = 16):
        self.agent_id = agent_id
        self.max_ticks = max_ticks
        self.important_memories: List[Dict] = []
        self.regular_memories: List[Dict] = []
        self.chat_history = InMemoryChatHistory()
    
    def add_memory(self, tick: int, content: str, is_important: bool = False) -> None:
        memory_item = {
            "tick": tick,
            "content": content,
            "agent_id": self.agent_id,
            "is_important": is_important
        }
        
        if is_important:
            self.important_memories.append(memory_item)
        else:
            self.regular_memories.append(memory_item)
            if len(self.regular_memories) > self.max_ticks:
                self.regular_memories.pop(0)
        
        self.chat_history.add_messages([BaseMessage(content=f"[Tick {tick}] {content}", type="human")])
    
    def get_recent_memories(self, count: int = 5) -> List[Dict]:
        all_memories = self.important_memories + self.regular_memories
        all_memories.sort(key=lambda x: x["tick"], reverse=True)
        return all_memories[:count]
    
    def get_all_memories(self) -> List[Dict]:
        return self.important_memories + self.regular_memories
    
    def clear(self) -> None:
        self.important_memories.clear()
        self.regular_memories.clear()
        self.chat_history.clear()


class GlobalMemoryStore:
    _instance: Optional['GlobalMemoryStore'] = None
    _memories: Dict[int, AgentMemoryManager] = {}
    
    @classmethod
    def get_instance(cls) -> 'GlobalMemoryStore':
        if cls._instance is None:
            cls._instance = GlobalMemoryStore()
        return cls._instance
    
    def get_or_create_memory(self, agent_id: int) -> AgentMemoryManager:
        if agent_id not in self._memories:
            self._memories[agent_id] = AgentMemoryManager(agent_id)
        return self._memories[agent_id]
    
    def get_memory(self, agent_id: int) -> Optional[AgentMemoryManager]:
        return self._memories.get(agent_id)
    
    def clear_all(self) -> None:
        self._memories.clear()
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Any, TypedDict
from langgraph.graph import add_messages

@dataclass
class AgentState:
    agent_id: int
    agent_name: str
    agent_faction: str
    personality: str
    
    stamina: int = 100
    satiety: int = 100
    health: int = 90
    current_node: str = "center"
    carried_food: int = 0
    confinement_ticks: int = 0
    
    memory: List[Dict] = field(default_factory=list)
    recent_logs: List[str] = field(default_factory=list)
    tick: int = 0
    
    action: Optional[str] = None
    target: Optional[str] = None
    reasoning: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "agent_id": self.agent_id,
            "agent_name": self.agent_name,
            "agent_faction": self.agent_faction,
            "personality": self.personality,
            "stamina": self.stamina,
            "satiety": self.satiety,
            "health": self.health,
            "current_node": self.current_node,
            "carried_food": self.carried_food,
            "confinement_ticks": self.confinement_ticks,
            "tick": self.tick,
            "memory_size": len(self.memory),
            "recent_logs_size": len(self.recent_logs)
        }


class GraphState(TypedDict):
    messages: List[Any]
    agent_state: AgentState
    action: Optional[str]
    target: Optional[str]
    reasoning: Optional[str]
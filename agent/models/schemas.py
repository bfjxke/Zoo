from pydantic import BaseModel
from typing import Optional, List


class AgentState(BaseModel):
    id: int
    name: str
    faction: str
    role: str
    stamina: int
    satiety: int
    health: int
    current_node: str
    carried_food: int = 0
    confinement_ticks: int = 0


class DecisionRequest(BaseModel):
    tick: int
    agent_count: int
    agents: List[AgentState]


class AgentDecision(BaseModel):
    agent_id: int
    agent_name: str
    action: str
    target: Optional[str] = None
    reasoning: Optional[str] = None


class DecisionResponse(BaseModel):
    tick: int
    decisions: List[AgentDecision]

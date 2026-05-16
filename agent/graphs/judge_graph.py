from langgraph.graph import StateGraph, END
from typing import TypedDict
from .shared_state import AgentState
import random


class JudgeGraphState(TypedDict):
    agent_state: AgentState
    action: str
    approved: bool
    confinement_ticks: int
    reasoning: str


def judge_decide_node(state: JudgeGraphState) -> JudgeGraphState:
    agent_state = state["agent_state"]
    action = state["action"]
    
    approved = random.random() < 0.6
    confinement_ticks = random.randint(1, 5) if not approved else 0
    
    reasoning = f"AI判官裁决：{'批准' if approved else '拒绝'}动作 {action}"
    
    return {
        "approved": approved,
        "confinement_ticks": confinement_ticks,
        "reasoning": reasoning
    }


def create_judge_graph():
    graph = StateGraph(JudgeGraphState)
    
    graph.add_node("judge", judge_decide_node)
    
    graph.set_entry_point("judge")
    graph.add_edge("judge", END)
    
    return graph.compile()


judge_graph = create_judge_graph()
from langgraph.graph import StateGraph, END
from typing import TypedDict
from .shared_state import AgentState


class SoldierGraphState(TypedDict):
    agent_state: AgentState
    action: str
    target: str
    result: str


def soldier_decide_node(state: SoldierGraphState) -> SoldierGraphState:
    agent_state = state["agent_state"]
    
    stamina = agent_state.stamina
    satiety = agent_state.satiety
    carried_food = getattr(agent_state, "carried_food", 0) or 0
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0
    current_node = agent_state.current_node
    
    if confinement_ticks > 0:
        action = "confined"
        target = None
        result = f"{agent_state.agent_name} 围困中({confinement_ticks}轮)，无法行动"
        return {"action": action, "target": target, "result": result}
    
    if satiety < 30:
        if carried_food > 0:
            action = "eat"
            target = None
            result = f"{agent_state.agent_name} 饥饿({satiety})，吃携带食物({carried_food})"
        else:
            action = "claim_food"
            target = None
            result = f"{agent_state.agent_name} 饥饿({satiety})，领取食物"
    elif stamina < 20:
        action = "rest"
        target = None
        result = f"{agent_state.agent_name} 疲劳({stamina})，休息"
    else:
        action = "move"
        target = "center"
        result = f"{agent_state.agent_name} 移动到中心"
    
    return {
        "action": action,
        "target": target,
        "result": result
    }


def create_soldier_graph():
    graph = StateGraph(SoldierGraphState)
    
    graph.add_node("decide", soldier_decide_node)
    
    graph.set_entry_point("decide")
    graph.add_edge("decide", END)
    
    return graph.compile()


soldier_graph = create_soldier_graph()
import json
from typing import TypedDict, Optional

from langgraph.graph import StateGraph, END

from .shared_state import AgentState
from services.minimax_chat_model import create_minimax_chat_model
from prompts.soldier_prompt import get_soldier_prompt
from graphs.memory_manager import GlobalMemoryStore


class SoldierGraphState(TypedDict):
    agent_state: AgentState
    action: str
    target: Optional[str]
    result: str
    reasoning: str


def _build_soldier_context(agent_state: AgentState) -> dict:
    memory_mgr = GlobalMemoryStore.get_instance().get_or_create_memory(agent_state.agent_id)
    recent = memory_mgr.get_recent_memories(3)
    memory_text = "\n".join([
        f"- Tick {m.get('tick', '?')}: {m.get('content', '')}"
        for m in recent
    ]) if recent else "（无记忆）"

    logs_text = "\n".join(agent_state.recent_logs[-3:]) if agent_state.recent_logs else "（无日志）"

    return {
        "personality": agent_state.personality,
        "stamina": agent_state.stamina,
        "satiety": agent_state.satiety,
        "health": agent_state.health,
        "current_node": agent_state.current_node,
        "carried_food": getattr(agent_state, "carried_food", 0) or 0,
        "confinement_ticks": getattr(agent_state, "confinement_ticks", 0) or 0,
        "tick": agent_state.tick,
        "recent_logs": logs_text,
    }


def soldier_decide_node(state: SoldierGraphState) -> SoldierGraphState:
    agent_state = state["agent_state"]
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0

    if confinement_ticks > 0:
        return {
            "action": "confined",
            "target": None,
            "result": f"{agent_state.agent_name} 围困中({confinement_ticks}轮)，无法行动",
            "reasoning": "围困状态",
        }

    ctx = _build_soldier_context(agent_state)
    prompt = get_soldier_prompt(faction=agent_state.agent_faction)
    messages = prompt.format_messages(**ctx)

    llm = create_minimax_chat_model(model="m2-her", temperature=0.8)
    response = llm.invoke(messages)

    try:
        decision = json.loads(response.content)
        action = decision.get("action", "rest")
        target = decision.get("target")
        reasoning = decision.get("reasoning", "")
    except (json.JSONDecodeError, TypeError):
        action = "rest"
        target = None
        reasoning = f"解析失败: {response.content[:100]}"

    memory_mgr = GlobalMemoryStore.get_instance().get_or_create_memory(agent_state.agent_id)
    memory_mgr.add_memory(
        tick=agent_state.tick,
        content=f"决策: {action} -> {target} | {reasoning}",
        is_important=(action in ("steal", "talk")),
    )

    return {
        "action": action,
        "target": target,
        "result": f"{agent_state.agent_name} 决策：{action}",
        "reasoning": reasoning,
    }


def create_soldier_graph():
    graph = StateGraph(SoldierGraphState)

    graph.add_node("decide", soldier_decide_node)

    graph.set_entry_point("decide")
    graph.add_edge("decide", END)

    return graph.compile()


soldier_graph = create_soldier_graph()

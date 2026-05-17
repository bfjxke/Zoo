import json
from typing import TypedDict, Optional, List, Dict, Any

from langgraph.graph import StateGraph, END
from langchain_core.messages import HumanMessage

from .shared_state import AgentState
from services.minimax_chat_model import create_minimax_chat_model
from prompts.leader_prompt import get_leader_prompt
from graphs.memory_manager import GlobalMemoryStore


class LeaderGraphState(TypedDict):
    agent_state: AgentState
    observation: str
    reflection: str
    plan: str
    reasoning: str
    action: str
    target: Optional[str]
    result: str
    memory_text: str
    logs_text: str


def _build_context(agent_state: AgentState) -> Dict[str, Any]:
    memory_mgr = GlobalMemoryStore.get_instance().get_or_create_memory(agent_state.agent_id)
    recent = memory_mgr.get_recent_memories(5)
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
        "memory": memory_text,
        "recent_count": min(5, len(recent)),
        "recent_logs": logs_text,
    }


def observe_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    ctx = _build_context(agent_state)

    observation = (
        f"【观察】{agent_state.agent_name}({agent_state.agent_faction}) "
        f"位置={agent_state.current_node} "
        f"耐力={agent_state.stamina} 饱食={agent_state.satiety} "
        f"健康={agent_state.health} 携带={ctx['carried_food']} "
        f"围困={ctx['confinement_ticks']}轮 回合={agent_state.tick}"
    )

    return {
        "observation": observation,
        "memory_text": ctx["memory"],
        "logs_text": ctx["recent_logs"],
    }


def think_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0

    if confinement_ticks > 0:
        return {
            "reflection": f"围困中({confinement_ticks}轮)，无法行动",
            "plan": "等待围困结束",
            "reasoning": "围困状态，不能做任何决策",
        }

    ctx = _build_context(agent_state)
    prompt = get_leader_prompt()
    messages = prompt.format_messages(**ctx)

    llm = create_minimax_chat_model(model="m2.7", temperature=0.7)
    response = llm.invoke(messages)

    try:
        decision = json.loads(response.content)
        reflection = decision.get("reasoning", "LLM推理完成")
        plan = f"决策: {decision.get('action', 'rest')}"
    except (json.JSONDecodeError, TypeError):
        reflection = f"LLM原始回复: {response.content[:200]}"
        plan = "解析失败，使用降级策略"

    return {
        "reflection": reflection,
        "plan": plan,
    }


def act_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0

    if confinement_ticks > 0:
        return {
            "action": "confined",
            "target": None,
            "result": f"{agent_state.agent_name} 围困中({confinement_ticks}轮)，无法行动",
        }

    ctx = _build_context(agent_state)
    prompt = get_leader_prompt()
    messages = prompt.format_messages(**ctx)

    llm = create_minimax_chat_model(model="m2.7", temperature=0.7)
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
        "reasoning": reasoning,
        "result": f"{agent_state.agent_name} 长链推理后决策：{action}",
    }


def create_leader_graph():
    graph = StateGraph(LeaderGraphState)

    graph.add_node("observe", observe_node)
    graph.add_node("think", think_node)
    graph.add_node("act", act_node)

    graph.set_entry_point("observe")
    graph.add_edge("observe", "think")
    graph.add_edge("think", "act")
    graph.add_edge("act", END)

    return graph.compile()


leader_graph = create_leader_graph()

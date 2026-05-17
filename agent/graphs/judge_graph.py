import json
from typing import TypedDict

from langgraph.graph import StateGraph, END

from .shared_state import AgentState
from services.minimax_chat_model import create_minimax_chat_model
from prompts.judge_prompt import get_judge_prompt


class JudgeGraphState(TypedDict):
    agent_state: AgentState
    action: str
    approved: bool
    confinement_ticks: int
    success_rate: float
    reasoning: str


def judge_decide_node(state: JudgeGraphState) -> JudgeGraphState:
    agent_state = state["agent_state"]
    action = state["action"]

    logs_text = "\n".join(agent_state.recent_logs[-5:]) if agent_state.recent_logs else "（无日志）"

    ctx = {
        "agent_name": agent_state.agent_name,
        "agent_faction": agent_state.agent_faction,
        "current_node": agent_state.current_node,
        "stamina": agent_state.stamina,
        "satiety": agent_state.satiety,
        "tick": agent_state.tick,
        "recent_logs": logs_text,
        "action": action,
    }

    prompt = get_judge_prompt()
    messages = prompt.format_messages(**ctx)

    llm = create_minimax_chat_model(model="m2.7", temperature=0.3)
    response = llm.invoke(messages)

    try:
        decision = json.loads(response.content)
        approved = decision.get("approved", False)
        confinement_ticks = decision.get("confinement_ticks", 0) if not approved else 0
        success_rate = decision.get("success_rate", 0.5)
        reasoning = decision.get("reasoning", "")
    except (json.JSONDecodeError, TypeError):
        approved = False
        confinement_ticks = 0
        success_rate = 0.0
        reasoning = f"解析失败: {response.content[:100]}"

    return {
        "approved": approved,
        "confinement_ticks": confinement_ticks,
        "success_rate": success_rate,
        "reasoning": reasoning,
    }


def create_judge_graph():
    graph = StateGraph(JudgeGraphState)

    graph.add_node("judge", judge_decide_node)

    graph.set_entry_point("judge")
    graph.add_edge("judge", END)

    return graph.compile()


judge_graph = create_judge_graph()

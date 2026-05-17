import json
import os
import asyncio
from typing import List, Dict, Any

from services.rate_limiter import TokenBucketRateLimiter
from services.minimax_chat_model import create_minimax_chat_model
from graphs.leader_graph import leader_graph
from graphs.soldier_graph import soldier_graph
from graphs.judge_graph import judge_graph
from graphs.shared_state import AgentState
from graphs.memory_manager import GlobalMemoryStore

MODEL_STANDARD = os.getenv("MINIMAX_MODEL", "m2.7")
MODEL_ROLEPLAY = os.getenv("MINIMAX_MODEL_ROLEPLAY", "m2-her")


def _to_agent_state(agent) -> AgentState:
    if isinstance(agent, dict):
        return AgentState(
            agent_id=agent.get("id", 0),
            agent_name=agent.get("name", "Unknown"),
            agent_faction=agent.get("faction", "neutral"),
            personality=agent.get("personality", "普通"),
            stamina=agent.get("stamina", 100),
            satiety=agent.get("satiety", 100),
            health=agent.get("health", 90),
            current_node=agent.get("current_node", "center"),
            carried_food=agent.get("carried_food", 0),
            confinement_ticks=agent.get("confinement_ticks", 0),
            tick=agent.get("tick", 0),
            memory=[],
            recent_logs=agent.get("recent_logs", []),
        )
    return AgentState(
        agent_id=getattr(agent, "id", 0),
        agent_name=getattr(agent, "name", "Unknown"),
        agent_faction=getattr(agent, "faction", "neutral"),
        personality=getattr(agent, "personality", "普通"),
        stamina=getattr(agent, "stamina", 100),
        satiety=getattr(agent, "satiety", 100),
        health=getattr(agent, "health", 90),
        current_node=getattr(agent, "current_node", "center"),
        carried_food=getattr(agent, "carried_food", 0),
        confinement_ticks=getattr(agent, "confinement_ticks", 0),
        tick=getattr(agent, "tick", 0),
        memory=[],
        recent_logs=getattr(agent, "recent_logs", []),
    )


def _select_graph_for_role(role: str):
    if role == "leader":
        return leader_graph
    elif role == "judge":
        return judge_graph
    else:
        return soldier_graph


async def make_decision_with_graph(agent) -> Dict[str, Any]:
    agent_state = _to_agent_state(agent)
    role = getattr(agent, "role", None) or (agent.get("role", "soldier") if isinstance(agent, dict) else "soldier")

    graph = _select_graph_for_role(role)

    try:
        if role == "judge":
            pending_action = (
                getattr(agent, "pending_action", "rest")
                or (agent.get("pending_action", "rest") if isinstance(agent, dict) else "rest")
            )
            result = graph.invoke({
                "agent_state": agent_state,
                "action": pending_action,
                "approved": False,
                "confinement_ticks": 0,
                "success_rate": 0.0,
                "reasoning": "",
            })
            return {
                "agent_id": agent_state.agent_id,
                "agent_name": agent_state.agent_name,
                "model_used": MODEL_STANDARD,
                "action": pending_action,
                "target": None,
                "reasoning": result.get("reasoning", ""),
                "approved": result.get("approved", False),
                "confinement_ticks": result.get("confinement_ticks", 0),
                "success_rate": result.get("success_rate", 0.0),
            }
        elif role == "leader":
            result = graph.invoke({
                "agent_state": agent_state,
                "observation": "",
                "reflection": "",
                "plan": "",
                "reasoning": "",
                "action": "",
                "target": None,
                "result": "",
                "memory_text": "",
                "logs_text": "",
            })
            return {
                "agent_id": agent_state.agent_id,
                "agent_name": agent_state.agent_name,
                "model_used": MODEL_STANDARD,
                "action": result.get("action", "rest"),
                "target": result.get("target"),
                "reasoning": result.get("reasoning", ""),
            }
        else:
            result = graph.invoke({
                "agent_state": agent_state,
                "action": "",
                "target": None,
                "result": "",
                "reasoning": "",
            })
            return {
                "agent_id": agent_state.agent_id,
                "agent_name": agent_state.agent_name,
                "model_used": MODEL_ROLEPLAY,
                "action": result.get("action", "rest"),
                "target": result.get("target"),
                "reasoning": result.get("reasoning", ""),
            }
    except Exception as e:
        return {
            "agent_id": agent_state.agent_id,
            "agent_name": agent_state.agent_name,
            "model_used": None,
            "action": "rest",
            "target": None,
            "reasoning": f"LangGraph执行失败: {str(e)}",
        }


async def dispatch_all_agents(agents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    limiter = TokenBucketRateLimiter(rate=1)

    async def call_with_limit(agent):
        await limiter.acquire(timeout=30.0)
        return await make_decision_with_graph(agent)

    tasks = [call_with_limit(agent) for agent in agents]
    decisions = await asyncio.gather(*tasks, return_exceptions=True)

    results = []
    for d in decisions:
        if isinstance(d, Exception):
            results.append({
                "agent_id": None,
                "agent_name": None,
                "model_used": None,
                "action": "rest",
                "target": None,
                "reasoning": f"请求失败: {str(d)}",
            })
        else:
            results.append(d)

    return results

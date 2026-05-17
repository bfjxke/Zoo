from typing import Dict, Any, Optional
from datetime import datetime

from graphs.judge_graph import judge_graph
from graphs.shared_state import AgentState


async def judge_action(
    agent_id: int,
    agent_name: str,
    agent_faction: str,
    action: str,
    target: Optional[str],
    context: str,
    current_tick: int,
    recent_logs: list,
) -> Dict[str, Any]:
    agent_state = AgentState(
        agent_id=agent_id,
        agent_name=agent_name,
        agent_faction=agent_faction,
        personality="",
        stamina=100,
        satiety=100,
        health=90,
        current_node="center",
        tick=current_tick,
        recent_logs=recent_logs,
    )

    try:
        result = judge_graph.invoke({
            "agent_state": agent_state,
            "action": action,
            "approved": False,
            "confinement_ticks": 0,
            "success_rate": 0.0,
            "reasoning": "",
        })

        return {
            "agent_id": agent_id,
            "agent_name": agent_name,
            "action": action,
            "approved": result.get("approved", False),
            "confinement_ticks": result.get("confinement_ticks", 0),
            "success_rate": result.get("success_rate", 0.0),
            "reasoning": result.get("reasoning", ""),
            "timestamp": datetime.now().isoformat(),
        }
    except Exception as e:
        return {
            "agent_id": agent_id,
            "agent_name": agent_name,
            "action": action,
            "approved": False,
            "confinement_ticks": 0,
            "success_rate": 0.0,
            "reasoning": f"判官裁决失败: {str(e)}",
            "timestamp": datetime.now().isoformat(),
        }

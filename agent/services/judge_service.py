import json
import os
import random
from typing import Dict, Any, Optional
from datetime import datetime, timedelta

JUDGE_APPROVAL_RATE = 0.6
JUDGE_CACHE_TICKS = 8

cache = {}
cache_timestamps = {}

def should_approve() -> bool:
    return random.random() < JUDGE_APPROVAL_RATE

def get_confinement_ticks() -> int:
    return random.randint(1, 5)

def get_reasoning_text(action: str, context: str) -> str:
    approved = should_approve()
    if approved:
        return f"动作'{action}'在当前情境'{context}'下合理可行。符合游戏规则和安全标准。"
    else:
        return f"动作'{action}'在当前情境'{context}'下存在风险或不合理。建议调整后再试。"

async def judge_action(
    agent_id: int,
    agent_name: str,
    agent_faction: str,
    action: str,
    target: Optional[str],
    context: str,
    current_tick: int,
    recent_logs: list
) -> Dict[str, Any]:
    cache_key = f"{agent_id}_{action}"
    
    if cache_key in cache:
        cached_tick, cached_result = cache[cache_key]
        if current_tick - cached_tick < JUDGE_CACHE_TICKS:
            return cached_result
    
    approved = should_approve()
    confinement_ticks = get_confinement_ticks() if not approved else 0
    
    result = {
        "agent_id": agent_id,
        "agent_name": agent_name,
        "action": action,
        "approved": approved,
        "confinement_ticks": confinement_ticks,
        "reasoning": get_reasoning_text(action, context),
        "timestamp": datetime.now().isoformat()
    }
    
    cache[cache_key] = (current_tick, result)
    
    return result

def get_cache_size() -> int:
    return len(cache)

def clear_cache():
    global cache
    cache = {}
from langchain_core.tools import tool
from typing import Optional


@tool
def move_tool(target: str) -> dict:
    """移动到指定节点。目标节点必须在当前位置的相邻列表中。

    Args:
        target: 目标节点名称，如 'A', 'B', 'center' 等
    """
    return {
        "action": "move",
        "target": target,
        "description": f"移动到{target}"
    }


@tool
def eat_tool() -> dict:
    """进食，恢复饱食度。优先吃携带的食物，再从营地库存吃。"""
    return {
        "action": "eat",
        "target": None,
        "description": "进食恢复饱食"
    }


@tool
def rest_tool() -> dict:
    """休息，恢复耐力。每个tick恢复一定量的耐力值。"""
    return {
        "action": "rest",
        "target": None,
        "description": "休息恢复耐力"
    }


@tool
def talk_tool(channel: str, message: str) -> dict:
    """在指定频道发言，与同阵营或全局频道通信。

    Args:
        channel: 频道名称，如 'faction'(阵营频道) 或 'global'(全局频道)
        message: 发言内容
    """
    return {
        "action": "talk",
        "target": channel,
        "description": f"在{channel}频道发言: {message[:50]}"
    }


@tool
def claim_food_tool() -> dict:
    """在阵营基地领取食物。必须在阵营基地才能执行。领取后食物进入携带背包。"""
    return {
        "action": "claim_food",
        "target": None,
        "description": "从营地领取食物"
    }


@tool
def pickup_food_tool() -> dict:
    """捡起当前位置的食物掉落。空投食物会掉落在随机节点上。"""
    return {
        "action": "pickup_food",
        "target": None,
        "description": "捡起食物"
    }


@tool
def steal_tool(target_faction: str) -> dict:
    """偷窃对方阵营的食物。必须在对方营地且无人看守时才能成功。被抓住会被围困。

    Args:
        target_faction: 目标阵营名称，如 'lawful', 'aggressive', 'neutral'
    """
    return {
        "action": "steal",
        "target": target_faction,
        "description": f"偷窃{target_faction}阵营的食物"
    }


@tool
def provoke_tool() -> dict:
    """挑衅其他Agent，可能引发冲突。增加对方阵营的敌意值。"""
    return {
        "action": "provoke",
        "target": None,
        "description": "挑衅其他Agent"
    }


ALL_TOOLS = [
    move_tool,
    eat_tool,
    rest_tool,
    talk_tool,
    claim_food_tool,
    pickup_food_tool,
    steal_tool,
    provoke_tool,
]

TOOL_MAP = {t.name: t for t in ALL_TOOLS}

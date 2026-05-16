from langchain_core.tools import tool
from typing import Optional

@tool
def move_tool(target: str) -> dict:
    """移动到指定节点。目标节点必须在当前位置的相邻列表中。"""
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
    """休息，恢复耐力。"""
    return {
        "action": "rest", 
        "target": None,
        "description": "休息恢复耐力"
    }


@tool
def claim_food_tool() -> dict:
    """在阵营基地领取食物。必须在阵营基地才能执行。"""
    return {
        "action": "claim_food",
        "target": None,
        "description": "从营地领取食物"
    }


@tool
def pickup_food_tool() -> dict:
    """捡起当前位置的食物掉落。"""
    return {
        "action": "pickup_food",
        "target": None,
        "description": "捡起食物"
    }


@tool
def steal_tool(target_faction: str) -> dict:
    """偷窃对方阵营的食物。必须在对方营地且无人看守时才能成功。"""
    return {
        "action": "steal",
        "target": target_faction,
        "description": f"偷窃{target_faction}阵营的食物"
    }
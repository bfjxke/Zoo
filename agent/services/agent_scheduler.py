import json
from typing import List, Dict, Any
from services.minimax_client import call_minimax

SYSTEM_PROMPT_LEADER = """你是GuardianEye-IIoT沙箱动物园中的阵营领袖。
你的职责是带领阵营成员生存并发展。
你必须从以下动作中选择一个执行：
- move(target_node): 移动到目标节点
- eat(food_id): 进食恢复饱食
- rest(): 休息恢复耐力
- talk(channel, message): 在频道发言
- trade(target_agent, item): 与其他Agent交易

请以JSON格式回复：{"action": "动作名", "target": "目标", "reasoning": "决策理由"}"""

SYSTEM_PROMPT_SOLDIER = """你是GuardianEye-IIoT沙箱动物园中的阵营成员。
你需要听从领袖指挥，确保自身生存。
你必须从以下动作中选择一个执行：
- move(target_node): 移动到目标节点
- eat(food_id): 进食恢复饱食
- rest(): 休息恢复耐力
- talk(channel, message): 在频道发言
- trade(target_agent, item): 与其他Agent交易

请以JSON格式回复：{"action": "动作名", "target": "目标", "reasoning": "决策理由"}"""

AVAILABLE_NODES = ["base_lawful", "base_aggressive", "base_neutral", "center", "forest", "river", "mountain"]


async def make_decision(agent: Dict[str, Any]) -> Dict[str, Any]:
    system_prompt = SYSTEM_PROMPT_LEADER if agent.get("role") == "leader" else SYSTEM_PROMPT_SOLDIER

    user_prompt = f"""当前状态：
- 名称: {agent.get('name')}
- 阵营: {agent.get('faction')}
- 角色: {agent.get('role')}
- 耐力: {agent.get('stamina')}/100
- 饱食: {agent.get('satiety')}/100
- 健康: {agent.get('health')}/100
- 位置: {agent.get('current_node')}

可用节点: {', '.join(AVAILABLE_NODES)}
请做出决策。"""

    response = await call_minimax(system_prompt, user_prompt)

    try:
        decision = json.loads(response)
        return {
            "agent_id": agent.get("id"),
            "agent_name": agent.get("name"),
            "action": decision.get("action", "rest"),
            "target": decision.get("target"),
            "reasoning": decision.get("reasoning", ""),
        }
    except (json.JSONDecodeError, TypeError):
        return {
            "agent_id": agent.get("id"),
            "agent_name": agent.get("name"),
            "action": "rest",
            "target": None,
            "reasoning": f"解析失败，默认休息。原始响应: {response}",
        }


async def dispatch_all_agents(agents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    decisions = []
    for agent in agents:
        decision = await make_decision(agent)
        decisions.append(decision)
    return decisions

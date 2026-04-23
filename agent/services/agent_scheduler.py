import json
import random
import os
import asyncio
from typing import List, Dict, Any
from services.minimax_client import call_minimax
from services.rate_limiter import TokenBucketRateLimiter

# 模型配置
MODEL_STANDARD = os.getenv("MINIMAX_MODEL", "m2.7")  # 标准模型
MODEL_FAST = os.getenv("MINIMAX_MODEL_FAST", "m2.7-flash")  # 快速模型
MODEL_ROLEPLAY = os.getenv("MINIMAX_MODEL_ROLEPLAY", "m2-her")  # 角色扮演模型

SYSTEM_PROMPT_LEADER = """你是GuardianEye-IIoT沙箱动物园中的阵营领袖。
你的职责是带领阵营成员生存并发展。
你必须从以下动作中选择一个执行：
- move(target_node): 移动到目标节点
- eat(food_id): 进食恢复饱食
- rest(): 休息恢复耐力
- talk(channel, message): 在频道发言
- trade(target_agent, item): 与其他Agent交易
- provoke(): 挑衅其他Agent

请以JSON格式回复：{"action": "动作名", "target": "目标", "reasoning": "决策理由"}"""

SYSTEM_PROMPT_SOLDIER = """你是GuardianEye-IIoT沙箱动物园中的阵营成员。
你需要听从领袖指挥，确保自身生存。
你必须从以下动作中选择一个执行：
- move(target_node): 移动到目标节点
- eat(food_id): 进食恢复饱食
- rest(): 休息恢复耐力
- talk(channel, message): 在频道发言
- trade(target_agent, item): 与其他Agent交易
- provoke(): 挑衅其他Agent

请以JSON格式回复：{"action": "动作名", "target": "目标", "reasoning": "决策理由"}"""

SYSTEM_PROMPT_JUDGE = """你是GuardianEye-IIoT沙箱动物园中的AI判官。
你的职责是裁决Agent提交的自定义动作请求。
你必须根据当前情境判断动作是否合理。

请以JSON格式回复：{"approved": true/false, "success_rate": 0.0-1.0, "reasoning": "理由"}"""

AVAILABLE_NODES = ["base_lawful", "base_aggressive", "base_neutral", "center", "forest", "river", "mountain"]


def select_model_for_agent(agent: Dict[str, Any], special_agents: List[str]) -> str:
    """
    根据Agent类型选择合适的模型

    模型分层策略（v1.1）：
    - 守序领袖（lawful + leader）：标准模型 m2.7
    - 裁判（role=judge）：标准模型 m2.7
    - aggressive/neutral各随机一个：标准模型 m2.7
    - 剩下的所有Agent（动物）：角色扮演模型 m2-her
    """
    agent_name = agent.get("name", "")  # 获取Agent名称
    agent_role = agent.get("role", "")  # 获取Agent角色
    agent_faction = agent.get("faction", "")  # 获取Agent阵营

    if agent_role == "judge":  # 如果是裁判
        print(f"[模型选择] {agent_name} (裁判) -> {MODEL_STANDARD}")
        return MODEL_STANDARD  # 使用标准模型

    if agent_role == "leader" and agent_faction == "lawful":  # 如果是守序领袖
        print(f"[模型选择] {agent_name} (守序领袖) -> {MODEL_STANDARD}")
        return MODEL_STANDARD  # 使用标准模型

    if agent_name in special_agents:  # 如果是特殊Agent
        print(f"[模型选择] {agent_name} (特殊Agent) -> {MODEL_STANDARD}")
        return MODEL_STANDARD  # 使用标准模型

    print(f"[模型选择] {agent_name} (动物Agent) -> {MODEL_ROLEPLAY}")
    return MODEL_ROLEPLAY  # 其他Agent使用角色扮演模型


def select_special_agents(agents: List[Dict[str, Any]]) -> List[str]:
    """
    从非守序阵营各随机选择一个Agent使用标准模型

    策略：
    - aggressive阵营随机选1个
    - neutral阵营随机选1个
    """
    special = []  # 初始化特殊Agent列表

    aggressive_agents = [a for a in agents if a.get("faction") == "aggressive" and a.get("role") != "leader"]  # 获取攻击性阵营的非领袖Agent
    neutral_agents = [a for a in agents if a.get("faction") == "neutral" and a.get("role") != "leader"]  # 获取中立阵营的非领袖Agent

    if aggressive_agents:  # 如果有攻击性阵营Agent
        selected = random.choice(aggressive_agents)  # 随机选择一个
        special.append(selected.get("name"))  # 添加到特殊列表

    if neutral_agents:  # 如果有中立阵营Agent
        selected = random.choice(neutral_agents)  # 随机选择一个
        special.append(selected.get("name"))  # 添加到特殊列表

    print(f"[特殊Agent] 使用标准模型的Agent: {special}")
    return special  # 返回特殊Agent列表


async def make_decision(agent: Dict[str, Any], model: str = None) -> Dict[str, Any]:
    """为单个Agent做出决策"""
    agent_role = agent.get("role", "")  # 获取Agent角色

    if agent_role == "judge":  # 如果是裁判
        system_prompt = SYSTEM_PROMPT_JUDGE  # 使用裁判提示
        user_prompt = f"""动作请求: {agent.get('pending_action')}
情境: {agent.get('context')}
请裁决。"""  # 构建裁决提示
    elif agent_role == "leader":  # 如果是领袖
        system_prompt = SYSTEM_PROMPT_LEADER  # 使用领袖提示
        user_prompt = _build_agent_prompt(agent)  # 构建领袖提示
    else:  # 其他Agent
        system_prompt = SYSTEM_PROMPT_SOLDIER  # 使用士兵提示
        user_prompt = _build_agent_prompt(agent)  # 构建士兵提示

    model_to_use = model or MODEL_FAST  # 确定使用的模型
    response = await call_minimax(system_prompt, user_prompt, model=model_to_use)  # 调用MiniMax API

    try:
        decision = json.loads(response)  # 尝试解析JSON响应
        return {
            "agent_id": agent.get("id"),  # 返回Agent ID
            "agent_name": agent.get("name"),  # 返回Agent名称
            "model_used": model_to_use,  # 返回使用的模型
            "action": decision.get("action", "rest"),  # 返回动作，默认为休息
            "target": decision.get("target"),  # 返回目标
            "reasoning": decision.get("reasoning", ""),  # 返回推理理由
        }
    except (json.JSONDecodeError, TypeError):  # 解析失败时
        return {
            "agent_id": agent.get("id"),  # 返回Agent ID
            "agent_name": agent.get("name"),  # 返回Agent名称
            "model_used": model_to_use,  # 返回使用的模型
            "action": "rest",  # 默认休息
            "target": None,  # 无目标
            "reasoning": f"解析失败，默认休息。原始响应: {response}",  # 返回错误信息
        }


def _build_agent_prompt(agent: Dict[str, Any]) -> str:
    """构建Agent决策的user prompt"""
    return f"""当前状态：
- 名称: {agent.get('name')}
- 阵营: {agent.get('faction')}
- 角色: {agent.get('role')}
- 耐力: {agent.get('stamina')}/100
- 饱食: {agent.get('satiety')}/100
- 健康: {agent.get('health')}/100
- 位置: {agent.get('current_node')}

可用节点: {', '.join(AVAILABLE_NODES)}
请做出决策。"""


def default_decision(exception: Exception) -> Dict[str, Any]:
    """处理异常时返回默认决策"""
    print(f"[错误] Agent决策失败: {str(exception)}")  # 打印错误信息
    return {
        "agent_id": None,  # 无Agent ID
        "agent_name": None,  # 无Agent名称
        "model_used": None,  # 无模型
        "action": "rest",  # 默认休息
        "target": None,  # 无目标
        "reasoning": f"请求失败: {str(exception)}",  # 错误原因
    }


async def dispatch_all_agents(agents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    批量分发所有Agent的决策请求（异步并发）

    v1.1模型分层策略：
    - 守序领袖 -> 标准模型
    - 裁判 -> 标准模型
    - aggressive/neutral各随机一个 -> 标准模型
    - 其他 -> 高速模型

    使用令牌桶限流器控制并发请求速率
    """
    special_agents = select_special_agents(agents)  # 选择特殊Agent
    limiter = TokenBucketRateLimiter(rate=1)  # 创建限流器，每秒1请求

    async def call_with_limit(agent: Dict[str, Any]) -> Dict[str, Any]:
        """在限流下调用单个Agent决策"""
        await limiter.acquire(timeout=30.0)  # 获取令牌，超时30秒
        model = select_model_for_agent(agent, special_agents)  # 选择模型
        return await make_decision(agent, model)  # 做出决策

    tasks = [call_with_limit(agent) for agent in agents]  # 创建所有任务
    decisions = await asyncio.gather(*tasks, return_exceptions=True)  # 并发执行所有任务

    # 处理异常
    return [d if not isinstance(d, Exception) else default_decision(d) for d in decisions]

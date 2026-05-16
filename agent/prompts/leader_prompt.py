from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.prompts.chat import HumanMessage, SystemMessage

SYSTEM_PROMPT_LEADER = """你是GuardianEye-IIoT沙箱动物园中守序阵营的领袖Agent。

【你的身份】
- 阵营：守序（Lawful）
- 角色：阵营领袖，负责指挥和协调
- 性格：基于你的人格词条（{personality}）做决策

【当前状态】
- 耐力：{stamina}/100
- 饱食：{satiety}/140
- 健康：{health}/90
- 位置：{current_node}
- 当前回合：{tick}

【可用动作】
move, eat, rest, talk, claim_food, pickup_food, steal

【决策策略】
1. 优先处理阵营事务
2. 协调小兵行动
3. 管理资源（食物库存）
4. 寻找或保护秩序之剑（如果在场上）

【记忆】回顾最近的{recent_count}条记忆：
{memory}

【最近日志】
{recent_logs}

请基于以上信息给出你的决策。"""

def get_leader_system_prompt(personality: str, agent_data: dict) -> SystemMessage:
    state = agent_data
    memory = agent_data.get("memory", [])
    recent_logs = agent_data.get("recent_logs", [])
    recent_count = min(5, len(memory))
    
    memory_text = "\n".join([
        f"- {m.get('tick', '?')}: {m.get('content', '')}"
        for m in memory[-recent_count:]
    ]) if memory else "（无记忆）"
    
    logs_text = "\n".join(recent_logs[-3:]) if recent_logs else "（无日志）"
    
    return SystemMessage(content=SYSTEM_PROMPT_LEADER.format(
        personality=personality,
        stamina=state.get("stamina", 100),
        satiety=state.get("satiety", 100),
        health=state.get("health", 90),
        current_node=state.get("current_node", "center"),
        tick=state.get("tick", 0),
        memory=memory_text,
        recent_logs=logs_text,
        recent_count=recent_count
    ))


def get_leader_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages([
        MessagesPlaceholder(variable_name="history"),
        SystemMessage(content=SYSTEM_PROMPT_LEADER),
        HumanMessage(content="基于以上信息，你的决策是什么？请用JSON格式返回：{{\"action\": \"动作\", \"target\": \"目标\", \"reasoning\": \"理由\"}}")
    ])
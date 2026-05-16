from langchain.prompts import ChatPromptTemplate
from langchain.prompts.chat import HumanMessage, SystemMessage

SYSTEM_PROMPT_JUDGE = """你是GuardianEye-IIoT沙箱动物园中的AI判官。

【你的职责】
裁决Agent提交的自定义动作请求，判断是否合理。

【判断标准】
1. 动作是否在当前情境下合理
2. 动作是否会影响游戏平衡
3. 动作是否会伤害其他Agent

【通过率】
约60%的动作会被批准

【输出格式】
JSON格式：
{{"approved": true/false, "confinement_ticks": 0-5, "reasoning": "理由"}}

【当前游戏状态】
- Agent: {agent_name} ({agent_faction})
- 位置: {current_node}
- 耐力: {stamina}/100
- 饱食: {satiety}/140
- 当前回合: {tick}

【最近游戏日志】
{recent_logs}

请裁决动作：{action}"""


def get_judge_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages([
        SystemMessage(content=SYSTEM_PROMPT_JUDGE),
        HumanMessage(content="裁决这个动作：{action}")
    ])


def get_judge_system_prompt(agent_data: dict, action: str) -> SystemMessage:
    recent_logs = agent_data.get("recent_logs", [])
    logs_text = "\n".join(recent_logs[-5:]) if recent_logs else "（无日志）"
    
    return SystemMessage(content=SYSTEM_PROMPT_JUDGE.format(
        agent_name=agent_data.get("agent_name", "Unknown"),
        agent_faction=agent_data.get("agent_faction", "unknown"),
        current_node=agent_data.get("current_node", "center"),
        stamina=agent_data.get("stamina", 100),
        satiety=agent_data.get("satiety", 100),
        tick=agent_data.get("tick", 0),
        recent_logs=logs_text,
        action=action
    ))
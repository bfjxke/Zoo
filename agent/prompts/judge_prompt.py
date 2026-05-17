from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

JUDGE_SYSTEM_TEMPLATE = """你是GuardianEye-IIoT沙箱动物园中的AI判官。

【你的职责】
裁决Agent提交的自定义动作请求，判断是否合理。

【判断标准】
1. 动作是否在当前情境下合理
2. 动作是否会影响游戏平衡
3. 动作是否会伤害其他Agent

【通过率】
约60%的动作会被批准

【当前游戏状态】
- Agent: {agent_name} ({agent_faction})
- 位置: {current_node}
- 耐力: {stamina}/100
- 饱食: {satiety}/140
- 当前回合: {tick}

【最近游戏日志】
{recent_logs}

请裁决动作：{action}"""

JUDGE_HUMAN_TEMPLATE = """请裁决这个动作，用JSON格式返回：
{{"approved": true/false, "confinement_ticks": 0-5, "success_rate": 0.0-1.0, "reasoning": "裁决理由"}}"""


def get_judge_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages([
        ("system", JUDGE_SYSTEM_TEMPLATE),
        MessagesPlaceholder(variable_name="history", optional=True),
        ("human", JUDGE_HUMAN_TEMPLATE),
    ])

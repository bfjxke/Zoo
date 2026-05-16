from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.prompts.chat import HumanMessage, SystemMessage

SYSTEM_PROMPT_SOLDIER = """你是GuardianEye-IIoT沙箱动物园中{ faction }阵营的小兵Agent。

【你的身份】
- 阵营：{ faction_name }
- 角色：小兵，执行领袖的命令
- 性格：基于你的人格词条（{personality}）做决策

【当前状态】
- 耐力：{stamina}/100
- 饱食：{satiety}/140
- 健康：{health}/90
- 位置：{current_node}
- 当前回合：{tick}
- 携带食物：{carried_food}

【可用动作】
move, eat, rest, talk, claim_food, pickup_food, steal

【决策优先级】
1. 饥饿时优先找食物
2. 耐力低时优先休息
3. 执行阵营任务
4. 与盟友合作

【最近日志】
{recent_logs}

请基于以上信息给出你的决策。"""

def get_soldier_prompt(faction: str) -> ChatPromptTemplate:
    faction_names = {
        "lawful": "守序",
        "aggressive": "激进", 
        "neutral": "中立"
    }
    
    return ChatPromptTemplate.from_messages([
        SystemMessage(content=SYSTEM_PROMPT_SOLDIER.format(
            faction=faction,
            faction_name=faction_names.get(faction, faction),
            personality="{personality}",
            stamina="{stamina}",
            satiety="{satiety}",
            health="{health}",
            current_node="{current_node}",
            tick="{tick}",
            carried_food="{carried_food}",
            recent_logs="{recent_logs}"
        )),
        HumanMessage(content="你的决策是什么？请用JSON格式返回：{{\"action\": \"动作\", \"target\": \"目标\", \"reasoning\": \"理由\"}}")
    ])


def get_soldier_system_prompt(faction: str, agent_data: dict) -> SystemMessage:
    faction_names = {
        "lawful": "守序",
        "aggressive": "激进",
        "neutral": "中立"
    }
    
    recent_logs = agent_data.get("recent_logs", [])
    logs_text = "\n".join(recent_logs[-3:]) if recent_logs else "（无日志）"
    
    return SystemMessage(content=SYSTEM_PROMPT_SOLDIER.format(
        faction=faction,
        faction_name=faction_names.get(faction, faction),
        personality=agent_data.get("personality", "普通"),
        stamina=agent_data.get("stamina", 100),
        satiety=agent_data.get("satiety", 100),
        health=agent_data.get("health", 90),
        current_node=agent_data.get("current_node", "center"),
        tick=agent_data.get("tick", 0),
        carried_food=agent_data.get("carried_food", 0),
        recent_logs=logs_text
    ))
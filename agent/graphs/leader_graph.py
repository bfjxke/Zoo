from langgraph.graph import StateGraph, END
from typing import TypedDict

class LeaderGraphState(TypedDict):
    agent_state: AgentState
    observation: str
    reflection: str
    plan: str
    reasoning: str
    action: str
    target: str
    result: str


def observe_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    memory = agent_state.memory
    recent_logs = agent_state.recent_logs
    
    carried_food = getattr(agent_state, "carried_food", 0) or 0
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0
    
    observation_text = f"""【观察】当前状态：
- Agent: {agent_state.agent_name} ({agent_state.agent_faction})
- 性格: {agent_state.personality}
- 位置: {agent_state.current_node}
- 耐力: {agent_state.stamina}/100
- 饱食: {agent_state.satiety}/140
- 健康: {agent_state.health}/90
- 携带食物: {carried_food}
- 围困状态: {confinement_ticks}轮

【上下文】
记忆数量: {len(memory)}
最近日志: {len(recent_logs)}
游戏回合: {agent_state.tick}"""
    
    return {"observation": observation_text}


def reason_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    observation = state.get("observation", "")
    
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0
    carried_food = getattr(agent_state, "carried_food", 0) or 0
    
    if confinement_ticks > 0:
        reflection = f"【反思】{agent_state.agent_name}正处于围困状态，无法执行任何动作。必须等待{confinement_ticks}轮后才能行动。"
        plan = "【计划】无行动，等待围困结束。"
        reasoning = f"围困中，不能做任何决策"
        return {"reflection": reflection, "plan": plan, "reasoning": reasoning}
    
    stamina = agent_state.stamina
    satiety = agent_state.satiety
    
    reflection = f"""【反思】
- 耐力状态: {'良好' if stamina > 50 else '中等' if stamina > 20 else '危险'}
- 饱食状态: {'饱餐' if satiety > 100 else '良好' if satiety > 50 else '中等' if satiety > 30 else '危险'}
- 携带食物: {carried_food}份
- 当前位置: {agent_state.current_node}

【战略思考】
基于{agent_state.personality}性格，需要综合考虑当前状态和阵营利益。"""
    
    return {"reflection": reflection}


def plan_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    reflection = state.get("reflection", "")
    observation = state.get("observation", "")
    
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0
    
    if confinement_ticks > 0:
        plan = "【计划】等待围困结束"
        return {"plan": plan}
    
    stamina = agent_state.stamina
    satiety = agent_state.satiety
    carried_food = getattr(agent_state, "carried_food", 0) or 0
    
    if satiety < 30:
        if carried_food > 0:
            plan = "【计划】优先吃携带的食物恢复饱食度"
        else:
            plan = "【计划】前往阵营基地领取食物"
    elif stamina < 20:
        plan = "【计划】休息恢复耐力"
    elif "order_sword" in agent_state.personality.lower():
        plan = "【计划】寻找秩序之剑位置"
    else:
        plan = "【计划】探索中立区域，收集信息"
    
    return {"plan": plan}


def act_node(state: LeaderGraphState) -> LeaderGraphState:
    agent_state = state["agent_state"]
    plan = state.get("plan", "")
    reflection = state.get("reflection", "")
    
    confinement_ticks = getattr(agent_state, "confinement_ticks", 0) or 0
    
    if confinement_ticks > 0:
        action = "confined"
        target = None
        result = f"{agent_state.agent_name} 围困中({confinement_ticks}轮)，无法行动"
        return {"action": action, "target": target, "result": result}
    
    stamina = agent_state.stamina
    satiety = agent_state.satiety
    carried_food = getattr(agent_state, "carried_food", 0) or 0
    
    if satiety < 30:
        if carried_food > 0:
            action = "eat"
            target = None
        else:
            action = "claim_food"
            target = None
    elif stamina < 20:
        action = "rest"
        target = None
    else:
        action = "move"
        target = "center"
    
    reasoning = f"{reflection}\n{plan}\n决策: {action}"
    result = f"{agent_state.agent_name} 长链思考后决策：{action}"
    
    return {
        "action": action,
        "target": target,
        "reasoning": reasoning,
        "result": result
    }


def create_leader_graph():
    graph = StateGraph(LeaderGraphState)
    
    graph.add_node("observe", observe_node)
    graph.add_node("reason", reason_node)
    graph.add_node("plan", plan_node)
    graph.add_node("act", act_node)
    
    graph.set_entry_point("observe")
    graph.add_edge("observe", "reason")
    graph.add_edge("reason", "plan")
    graph.add_edge("plan", "act")
    graph.add_edge("act", END)
    
    return graph.compile()


leader_graph = create_leader_graph()
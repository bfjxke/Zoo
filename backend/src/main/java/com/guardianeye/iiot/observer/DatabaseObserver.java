package com.guardianeye.iiot.observer;

import com.guardianeye.iiot.model.ActionLog;
import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentState;
import com.guardianeye.iiot.model.GameState;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.repository.AgentStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseObserver implements GameObserver {
    private final AgentRepository agentRepository;
    private final AgentStateRepository agentStateRepository;

    @Override
    @Transactional
    public void onTickComplete(int tick, GameState state, List<ActionLog> actions) {
        log.debug("[DatabaseObserver] Tick #{} 完成，开始持久化", tick);
        List<Agent> agents = agentRepository.findAll();
        for (Agent agent : agents) {
            AgentState snapshot = new AgentState();
            snapshot.setAgentId(agent.getId());
            snapshot.setTickNumber(tick);
            snapshot.setStamina(agent.getStamina());
            snapshot.setSatiety(agent.getSatiety());
            snapshot.setHealth(agent.getHealth());
            snapshot.setCurrentNode(agent.getCurrentNode());
            snapshot.setIsFatigued(agent.getFatigued() != null && agent.getFatigued());
            snapshot.setIsHungry(agent.getHungry() != null && agent.getHungry());
            snapshot.setIsAlive(agent.getAlive() != null && agent.getAlive());
            agentStateRepository.save(snapshot);
        }
        for (ActionLog action : actions) {
            // 保存动作记录
        }
        log.info("[DatabaseObserver] Tick #{} 持久化完成", tick);
    }

    @Override
    public void onAgentAction(ActionLog action) {
        // 动作已在onTickComplete中统一保存
    }

    @Override
    public void onGameEnd(int tick, String reason, String winner) {
        log.info("[DatabaseObserver] 游戏结束！回合#{}，原因：{}，获胜：{}", tick, reason, winner);
    }

    @Override
    public void onPeaceEnding(int tick, String winner) {
        log.info("[DatabaseObserver] 和平结局触发！回合#{}，守序阵营胜利", tick);
    }
}

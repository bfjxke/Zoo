package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RespawnPhase implements TickPhase {
    
    private final AgentRepository agentRepository;
    
    @Override
    @Transactional
    public void execute(GameState gameState, int tick) {
        List<Agent> deadAgents = agentRepository.findAll().stream()
                .filter(a -> !a.getAlive())
                .toList();
        
        for (Agent agent : deadAgents) {
            processRespawn(agent, tick);
        }
        
        agentRepository.saveAll(deadAgents);
    }
    
    private void processRespawn(Agent agent, int tick) {
        if (agent.getDeathTicksRemaining() > 0) {
            agent.setDeathTicksRemaining(agent.getDeathTicksRemaining() - 1);
            log.info("[复活倒计时] {} 剩余 {} Tick", agent.getName(), agent.getDeathTicksRemaining());
            return;
        }
        
        if (agent.getDeathTicksRemaining() <= 0) {
            agent.setAlive(true);
            agent.setStamina(GameConstants.STAMINA_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setSatiety(GameConstants.SATIETY_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setHealth(GameConstants.HEALTH_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setFatigued(false);
            agent.setHungry(false);
            String baseNode = GameConstants.getFactionBaseNode(agent.getFaction());
            agent.setCurrentNode(baseNode);
            
            log.info("[复活] {} 复活！状态重置为50%，位置: {}", agent.getName(), baseNode);
        }
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
    
    @Override
    public String getName() {
        return "respawn";
    }
}
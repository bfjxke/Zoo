package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GodModeService {
    
    private final AgentRepository agentRepository;
    
    public int airdropSupplies(String targetNode, int foodAmount) {
        List<Agent> affectedAgents = new ArrayList<>();
        
        for (Agent agent : agentRepository.findByAliveTrue()) {
            if (agent.getCurrentNode().equals(targetNode) || 
                GameConstants.areAdjacent(agent.getCurrentNode(), targetNode)) {
                
                int oldSatiety = agent.getSatiety();
                int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, 
                    oldSatiety + foodAmount);
                agent.setSatiety(newSatiety);
                affectedAgents.add(agent);
                
                log.info("[上帝空投] {} ({}) 饱食度 {} -> {}", 
                    agent.getName(), agent.getCurrentNode(), oldSatiety, newSatiety);
            }
        }
        
        agentRepository.saveAll(affectedAgents);
        return affectedAgents.size();
    }
    
    public int applyPlague(String targetFaction, int staminaPenalty) {
        List<Agent> targetAgents;
        
        if ("all".equals(targetFaction)) {
            targetAgents = agentRepository.findByAliveTrue();
        } else {
            targetAgents = agentRepository.findByAliveTrue().stream()
                .filter(a -> targetFaction.equals(a.getFaction()))
                .toList();
        }
        
        for (Agent agent : targetAgents) {
            int oldStamina = agent.getStamina();
            int newStamina = Math.max(0, oldStamina - staminaPenalty);
            agent.setStamina(newStamina);
            
            log.info("[上帝瘟疫] {} ({}) 耐力 {} -> {}", 
                agent.getName(), agent.getFaction(), oldStamina, newStamina);
        }
        
        agentRepository.saveAll(targetAgents);
        return targetAgents.size();
    }
    
    public int grantAmnesty(String targetAgentName) {
        if ("all".equals(targetAgentName)) {
            List<Agent> deadAgents = agentRepository.findAll().stream()
                .filter(a -> !a.getAlive())
                .toList();
            
            for (Agent agent : deadAgents) {
                agent.setAlive(true);
                agent.setStamina(50);
                agent.setSatiety(50);
                agent.setHealth(50);
                agent.setDeathTicksRemaining(0);
                
                log.info("[上帝赦免] {} 复活，属性重置为50", agent.getName());
            }
            
            agentRepository.saveAll(deadAgents);
            return deadAgents.size();
        } else {
            Optional<Agent> agentOpt = agentRepository.findByName(targetAgentName);
            if (agentOpt.isEmpty()) {
                log.warn("[上帝赦免] 未找到Agent: {}", targetAgentName);
                return 0;
            }
            
            Agent agent = agentOpt.get();
            if (agent.getAlive()) {
                log.warn("[上帝赦免] Agent {} 存活，无需赦免", targetAgentName);
                return 0;
            }
            
            agent.setAlive(true);
            agent.setStamina(50);
            agent.setSatiety(50);
            agent.setHealth(50);
            agent.setDeathTicksRemaining(0);
            agentRepository.save(agent);
            
            log.info("[上帝赦免] {} 复活，属性重置为50", agent.getName());
            return 1;
        }
    }
}
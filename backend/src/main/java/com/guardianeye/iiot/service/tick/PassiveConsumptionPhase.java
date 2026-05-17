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
public class PassiveConsumptionPhase implements TickPhase {
    
    private final AgentRepository agentRepository;
    
    @Override
    @Transactional
    public void execute(GameState gameState, int tick) {
        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        
        for (Agent agent : aliveAgents) {
            applyPassiveConsumption(agent, tick, gameState);
        }
        
        agentRepository.saveAll(aliveAgents);
    }
    
    private void applyPassiveConsumption(Agent agent, int tick, GameState gameState) {
        if (agent.isConfined()) {
            agent.setConfinementTicks(agent.getConfinementTicks() - 1);
            log.info("[被动消耗] {} 禁闭中，剩余{}轮", agent.getName(), agent.getConfinementTicks());
            if (!agent.isConfined()) {
                log.info("[被动消耗] {} 禁闭结束", agent.getName());
            }
            agent.setTickCount(tick);
            return;
        }
        
        if (tick % 3 == 0) {
            agent.setSatiety(Math.max(0, (int)(agent.getSatiety() - GameConstants.SATIETY_BASE_COST)));
        }
        
        if (agent.isHungry()) {
            agent.setHealth(Math.max(0, agent.getHealth() - GameConstants.HEALTH_HUNGER_DAMAGE));
            log.info("[被动消耗] {} 饥饿扣血 -{}", agent.getName(), GameConstants.HEALTH_HUNGER_DAMAGE);
        }
        
        int healthRegen = 0;
        if (agent.getSatiety() > GameConstants.SATIETY_BUFF_THRESHOLD) {
            healthRegen = GameConstants.HEALTH_REGEN_BUFF;
        } else if (agent.getSatiety() > GameConstants.HEALTH_REGEN_SATIETY_THRESHOLD) {
            healthRegen = GameConstants.HEALTH_REGEN_NORMAL;
        }
        
        if (healthRegen > 0) {
            int oldHealth = agent.getHealth();
            agent.setHealth(Math.min(GameConstants.HEALTH_MAX, agent.getHealth() + healthRegen));
        }
        
        if (agent.isDead()) {
            int droppedFood = agent.getCarriedFood();
            String currentNode = agent.getCurrentNode();
            
            if (droppedFood > 0) {
                gameState.dropFood(currentNode, droppedFood);
                log.info("[食物掉落] {} 在 {} 掉落{}份食物", agent.getName(), currentNode, droppedFood);
            }
            
            agent.setCarriedFood(0);
            agent.setAlive(false);
            agent.setDeathTicksRemaining(GameConstants.RESPAWN_TICKS);
            String baseNode = GameConstants.getFactionBaseNode(agent.getFaction());
            agent.setCurrentNode(baseNode);
            
            if (gameState.getOrderSwordHolderId() != null && 
                gameState.getOrderSwordHolderId().equals(agent.getId())) {
                gameState.setOrderSwordLocation(currentNode);
                gameState.setOrderSwordHolderId(null);
                log.info("[秩序之剑] {} 死亡，剑掉落在{}", agent.getName(), currentNode);
            }
            
            log.info("[被动消耗] {} 死亡，复活位置: {}", agent.getName(), baseNode);
        }
        
        agent.setTickCount(tick);
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
    
    @Override
    public String getName() {
        return "passiveConsumption";
    }
}
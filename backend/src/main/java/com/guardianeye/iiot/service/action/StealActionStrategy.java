package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import com.guardianeye.iiot.model.GameStateRepository;
import com.guardianeye.iiot.service.ActionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StealActionStrategy implements ActionStrategy {
    
    private final AgentRepository agentRepository;
    private final GameStateRepository gameStateRepository;
    
    @Override
    public ActionResult execute(Agent agent, String targetFaction) {
        log.info("[策略-STEAL] Agent:{} 偷窃食物", agent.getName());
        
        String agentFaction = agent.getFaction();
        String targetF = targetFaction != null ? targetFaction : getOtherFaction(agentFaction);
        
        if (targetF == null || targetF.equals(agentFaction)) {
            return ActionResult.failure("偷窃目标阵营无效");
        }
        
        String targetBase = GameConstants.getFactionBaseNode(targetF);
        if (!agent.getCurrentNode().equals(targetBase)) {
            return ActionResult.failure("偷窃必须在对方营地，当前在" + agent.getCurrentNode());
        }
        
        List<Agent> agentsHere = agentRepository.findAll().stream()
            .filter(a -> a.getAlive() && a.getCurrentNode().equals(targetBase))
            .toList();
        
        boolean hasEnemy = agentsHere.stream()
            .anyMatch(a -> !a.getFaction().equals(agentFaction) && !a.getFaction().equals(targetF));
        
        if (hasEnemy) {
            agent.setConfinementTicks(GameConstants.STEAL_CATCH_CONFINEMENT);
            agent.setConfinementReason("偷窃被抓");
            log.warn("[策略-STEAL] <<< {} 在{}偷窃被抓，关禁闭{}轮", 
                agent.getName(), targetBase, GameConstants.STEAL_CATCH_CONFINEMENT);
            return ActionResult.failure("偷窃被抓，关禁闭" + GameConstants.STEAL_CATCH_CONFINEMENT + "轮");
        }
        
        Optional<GameState> gsOptional = getGameState();
        if (gsOptional.isEmpty() || gsOptional.get().getFactionFood(targetF) <= 0) {
            return ActionResult.failure("目标营地库存为空");
        }
        
        GameState gs = gsOptional.get();
        int stealAmount = Math.min(GameConstants.STEAL_AMOUNT, gs.getFactionFood(targetF));
        gs.consumeFactionFood(targetF, stealAmount);
        agent.setCarriedFood(Math.min(GameConstants.MAX_CARRIED_FOOD, agent.getCarriedFood() + stealAmount));
        
        log.info("[策略-STEAL] <<< {} 从{}偷窃{}份食物成功", agent.getName(), targetF, stealAmount);
        return ActionResult.success("偷窃" + targetF + "阵营" + stealAmount + "份食物，携带量:" + agent.getCarriedFood());
    }
    
    @Override
    public String getActionName() {
        return "steal";
    }
    
    private String getOtherFaction(String faction) {
        return switch (faction) {
            case "lawful" -> "aggressive";
            case "aggressive" -> "lawful";
            case "neutral" -> "lawful";
            default -> null;
        };
    }
    
    private Optional<GameState> getGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (states.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(states.get(0));
    }
}
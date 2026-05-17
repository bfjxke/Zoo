package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
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
public class ClaimFoodActionStrategy implements ActionStrategy {
    
    private final GameStateRepository gameStateRepository;
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-CLAIM_FOOD] Agent:{} 从营地领取食物", agent.getName());
        
        String baseNode = GameConstants.getFactionBaseNode(agent.getFaction());
        if (!agent.getCurrentNode().equals(baseNode)) {
            return ActionResult.failure("领取食物必须在阵营基地，当前在" + agent.getCurrentNode());
        }
        
        if (agent.getCarriedFood() >= GameConstants.MAX_CARRIED_FOOD) {
            return ActionResult.failure("携带食物已达上限(" + GameConstants.MAX_CARRIED_FOOD + ")");
        }
        
        Optional<GameState> gsOptional = getGameState();
        if (gsOptional.isEmpty() || gsOptional.get().getFactionFood(agent.getFaction()) <= 0) {
            return ActionResult.failure("营地库存为空");
        }
        
        gsOptional.get().consumeFactionFood(agent.getFaction(), 1);
        agent.setCarriedFood(agent.getCarriedFood() + 1);
        
        log.info("[策略-CLAIM_FOOD] <<< 从营地领取1份食物，携带量:" + agent.getCarriedFood());
        return ActionResult.success("从营地领取1份食物，携带量:" + agent.getCarriedFood());
    }
    
    @Override
    public String getActionName() {
        return "claim_food";
    }
    
    private Optional<GameState> getGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (states.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(states.get(0));
    }
}
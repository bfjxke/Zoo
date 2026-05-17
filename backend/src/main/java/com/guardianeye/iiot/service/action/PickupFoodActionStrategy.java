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
public class PickupFoodActionStrategy implements ActionStrategy {
    
    private final GameStateRepository gameStateRepository;
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-PICKUP_FOOD] Agent:{} 捡起食物", agent.getName());
        
        int foodAtNode = 0;
        Optional<GameState> gsOptional = getGameState();
        if (gsOptional.isPresent()) {
            foodAtNode = gsOptional.get().getFoodAtNode(agent.getCurrentNode());
        }
        
        if (foodAtNode <= 0) {
            return ActionResult.failure("当前位置没有食物");
        }
        
        if (agent.getCarriedFood() >= GameConstants.MAX_CARRIED_FOOD) {
            return ActionResult.failure("携带食物已达上限(" + GameConstants.MAX_CARRIED_FOOD + ")");
        }
        
        if (gsOptional.isPresent()) {
            gsOptional.get().pickupFood(agent.getCurrentNode(), 1);
        }
        agent.setCarriedFood(agent.getCarriedFood() + 1);
        
        log.info("[策略-PICKUP_FOOD] <<< 捡起1份食物，携带量:" + agent.getCarriedFood());
        return ActionResult.success("捡起1份食物，携带量:" + agent.getCarriedFood());
    }
    
    @Override
    public String getActionName() {
        return "pickup_food";
    }
    
    private Optional<GameState> getGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (states.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(states.get(0));
    }
}
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
public class EatActionStrategy implements ActionStrategy {
    
    private final GameStateRepository gameStateRepository;
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-EAT] Agent:{} 进食，当前饱食:{}, 携带:{}", 
            agent.getName(), agent.getSatiety(), agent.getCarriedFood());
        
        if (agent.getSatiety() >= GameConstants.SATIETY_MAX_WITH_BUFF) {
            log.warn("[策略-EAT] <<< 饱食度已满({})，无法继续进食", agent.getSatiety());
            return ActionResult.failure("饱食度已满(" + agent.getSatiety() + ")，无法继续进食");
        }
        
        if (agent.getCarriedFood() > 0) {
            agent.setCarriedFood(agent.getCarriedFood() - 1);
            int recover = GameConstants.SATIETY_EAT_RECOVER;
            int oldSatiety = agent.getSatiety();
            int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, oldSatiety + recover);
            agent.setSatiety(newSatiety);
            
            String buffMsg = "";
            if (oldSatiety <= GameConstants.SATIETY_BUFF_THRESHOLD && newSatiety > GameConstants.SATIETY_BUFF_THRESHOLD) {
                buffMsg = " 【触发饱餐Buff：耐力恢复加速50%】";
            }
            
            log.info("[策略-EAT] <<< 吃携带食物成功 饱食+{} {} -> {} {}", recover, oldSatiety, newSatiety, buffMsg);
            return ActionResult.success("吃携带食物，饱食+" + recover + "，" + oldSatiety + " -> " + newSatiety + buffMsg);
        }
        
        Optional<GameState> gsOptional = getGameState();
        if (gsOptional.isEmpty()) {
            log.warn("[策略-EAT] <<< 游戏状态未初始化");
            return ActionResult.failure("游戏状态未初始化");
        }
        
        GameState gs = gsOptional.get();
        String faction = agent.getFaction();
        
        if (gs.consumeFactionFood(faction, 1)) {
            int recover = GameConstants.SATIETY_EAT_RECOVER;
            int oldSatiety = agent.getSatiety();
            int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, oldSatiety + recover);
            agent.setSatiety(newSatiety);
            
            String buffMsg = "";
            if (oldSatiety <= GameConstants.SATIETY_BUFF_THRESHOLD && newSatiety > GameConstants.SATIETY_BUFF_THRESHOLD) {
                buffMsg = " 【触发饱餐Buff：耐力恢复加速50%】";
            }
            
            log.info("[策略-EAT] <<< 从营地库存进食成功 库存-1，饱食+{} {} -> {} {}", 
                recover, oldSatiety, newSatiety, buffMsg);
            return ActionResult.success("从营地库存吃食物，饱食+" + recover + "，" + oldSatiety + " -> " + newSatiety + buffMsg);
        }
        
        log.warn("[策略-EAT] <<< 营地库存为空，无法进食");
        return ActionResult.failure("营地库存为空，无法进食");
    }
    
    @Override
    public String getActionName() {
        return "eat";
    }
    
    private Optional<GameState> getGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (states.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(states.get(0));
    }
}
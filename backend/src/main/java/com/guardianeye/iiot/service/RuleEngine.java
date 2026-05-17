package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import com.guardianeye.iiot.service.action.ActionStrategy;
import com.guardianeye.iiot.service.action.ActionStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RuleEngine {

    private final ActionStrategyFactory actionStrategyFactory;
    
    public RuleEngine(ActionStrategyFactory actionStrategyFactory) {
        this.actionStrategyFactory = actionStrategyFactory;
    }

    public ActionResult validateAndExecute(Agent agent, String action, String target) {
        log.info("[规则引擎] >>> Agent:{} 尝试执行动作 action:{}, target:{}", 
            agent.getName(), action, target);
        log.info("[规则引擎] 当前状态 - 位置:{}, 耐力:{}, 饱食:{}, 健康:{}", 
            agent.getCurrentNode(), agent.getStamina(), agent.getSatiety(), agent.getHealth());
        
        if (!agent.getAlive()) {
            log.warn("[规则引擎] <<< Agent已死亡，拒绝动作");
            return ActionResult.failure("Agent已死亡");
        }

        String actionLower = action.toLowerCase();
        log.info("[规则引擎] 动作小写:{}, 白名单:{}", actionLower, GameConstants.ALLOWED_ACTIONS);

        if (GameConstants.ALLOWED_ACTIONS.contains(actionLower)) {
            ActionResult result = executeAllowedAction(agent, actionLower, target);
            log.info("[规则引擎] <<< 动作执行结果 success:{}, message:{}", result.isSuccess(), result.getMessage());
            return result;
        }

        log.warn("[规则引擎] <<< 动作不在白名单，需AI判官判定");
        return ActionResult.pending("JUDGE_PENDING", GameConstants.DEFAULT_SUCCESS_RATE);
    }

    private ActionResult executeAllowedAction(Agent agent, String action, String target) {
        ActionStrategy strategy = actionStrategyFactory.getStrategy(action);
        if (strategy == null) {
            log.warn("[规则引擎] <<< 未找到动作策略:{}", action);
            return ActionResult.failure("未知动作: " + action);
        }
        return strategy.execute(agent, target);
    }
    
    public boolean hasOrderSword(Agent agent, GameState gameState) {
        if (gameState.getOrderSwordHolderId() == null) {
            return false;
        }
        return gameState.getOrderSwordHolderId().equals(agent.getId());
    }
}
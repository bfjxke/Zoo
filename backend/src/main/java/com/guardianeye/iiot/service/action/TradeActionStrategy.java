package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeActionStrategy implements ActionStrategy {
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-TRADE] Agent:{} 与 {} 交易", agent.getName(), target);
        
        double penalty = computePenalty(agent);
        int cost = (int)(GameConstants.CUSTOM_ACTION_COST * penalty);
        
        if (agent.getStamina() < cost) {
            return ActionResult.failure("耐力不足(" + agent.getStamina() + ")，需要 " + cost);
        }
        
        agent.setStamina(agent.getStamina() - cost);
        return ActionResult.success("与 " + (target != null ? target : "未知") + " 交易，消耗耐力 " + cost);
    }
    
    @Override
    public String getActionName() {
        return "trade";
    }
    
    private double computePenalty(Agent agent) {
        double penalty = 1.0;
        if (agent.isFatigued()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        if (agent.isHungry()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        return penalty;
    }
}
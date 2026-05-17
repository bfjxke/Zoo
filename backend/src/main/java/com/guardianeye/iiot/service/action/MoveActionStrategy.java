package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.ActionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoveActionStrategy implements ActionStrategy {
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-MOVE] Agent:{} 移动到:{}", agent.getName(), target);
        
        if (target == null || !GameConstants.getAllNodeIds().contains(target)) {
            log.warn("[策略-MOVE] <<< 无效目标节点:{}", target);
            return ActionResult.failure("无效目标节点: " + target);
        }
        
        if (target.equals(agent.getCurrentNode())) {
            log.warn("[策略-MOVE] <<< 已在目标节点:{}", target);
            return ActionResult.failure("已在目标节点: " + target);
        }
        
        List<String> adjacent = GameConstants.getAdjacentNodes(agent.getCurrentNode());
        
        if (adjacent == null || !adjacent.contains(target)) {
            log.warn("[策略-MOVE] <<< 无法从{}移动到{}，节点不相邻", agent.getCurrentNode(), target);
            return ActionResult.failure("无法从 " + agent.getCurrentNode() + " 移动到 " + target + "，节点不相邻");
        }
        
        double penalty = computePenalty(agent);
        int cost = (int)(GameConstants.STAMINA_MOVE_COST * penalty);
        
        if (agent.getStamina() < cost) {
            log.warn("[策略-MOVE] <<< 耐力不足({})，需要{}", agent.getStamina(), cost);
            return ActionResult.failure("耐力不足(" + agent.getStamina() + ")，需要 " + cost);
        }
        
        String from = agent.getCurrentNode();
        agent.setStamina(agent.getStamina() - cost);
        agent.setCurrentNode(target);
        
        log.info("[策略-MOVE] <<< 移动成功 {} -> {}，消耗耐力{}，剩余耐力{}", from, target, cost, agent.getStamina());
        return ActionResult.success(from + " -> " + target + "，消耗耐力 " + cost + "，剩余 " + agent.getStamina());
    }
    
    @Override
    public String getActionName() {
        return "move";
    }
    
    private double computePenalty(Agent agent) {
        double penalty = 1.0;
        if (agent.isFatigued()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        if (agent.isHungry()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        return penalty;
    }
}
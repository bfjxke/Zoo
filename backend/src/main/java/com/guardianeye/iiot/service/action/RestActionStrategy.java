package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RestActionStrategy implements ActionStrategy {
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-REST] Agent:{} 休息，当前耐力:{}", agent.getName(), agent.getStamina());
        
        if (agent.getStamina() >= 100) {
            log.warn("[策略-REST] <<< 耐力已满，无需休息");
            return ActionResult.failure("耐力已满，无需休息");
        }
        
        double penalty = computePenalty(agent);
        double recoveryMultiplier = computeRecoveryMultiplier(agent);
        int recover = (int)(GameConstants.STAMINA_REST_RECOVER / (penalty * recoveryMultiplier));
        
        int oldStamina = agent.getStamina();
        agent.setStamina(Math.min(100, oldStamina + recover));
        
        String msg = "休息恢复耐力 +" + recover + "，" + oldStamina + " -> " + agent.getStamina();
        if (recoveryMultiplier < 1.0) {
            msg += " 【饱餐Buff生效：恢复效率×" + String.format("%.1f", 1/recoveryMultiplier) + "】";
        }
        
        log.info("[策略-REST] <<< 休息成功 +{} 耐力 {} -> {}", recover, oldStamina, agent.getStamina());
        return ActionResult.success(msg);
    }
    
    @Override
    public String getActionName() {
        return "rest";
    }
    
    private double computePenalty(Agent agent) {
        double penalty = 1.0;
        if (agent.isFatigued()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        if (agent.isHungry()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        return penalty;
    }
    
    private double computeRecoveryMultiplier(Agent agent) {
        if (agent.getSatiety() > GameConstants.SATIETY_BUFF_THRESHOLD) {
            return GameConstants.SATIETY_BUFF_RECOVERY_MULTIPLIER;
        }
        return 1.0;
    }
}
package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.service.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProvokeActionStrategy implements ActionStrategy {
    
    @Override
    public ActionResult execute(Agent agent, String target) {
        log.info("[策略-PROVOKE] Agent:{} 挑衅", agent.getName());
        
        String targetDesc = target != null ? "对 " + target : "";
        return ActionResult.success("挑衅" + targetDesc + "【无实际数值影响】");
    }
    
    @Override
    public String getActionName() {
        return "provoke";
    }
}
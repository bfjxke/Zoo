package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.service.ActionResult;

public interface ActionStrategy {
    
    ActionResult execute(Agent agent, String target);
    
    String getActionName();
    
    default boolean canExecute(Agent agent) {
        return agent.getAlive();
    }
}
package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.service.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ActionStrategyFactory {
    
    private final Map<String, ActionStrategy> strategies;
    
    public ActionStrategyFactory(List<ActionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        s -> s.getActionName().toLowerCase(),
                        Function.identity()
                ));
        log.info("[策略工厂] 已注册 {} 个动作策略: {}", strategies.size(), strategies.keySet());
    }
    
    public ActionStrategy getStrategy(String actionName) {
        if (actionName == null) {
            log.warn("[策略工厂] 动作名为空");
            return null;
        }
        
        ActionStrategy strategy = strategies.get(actionName.toLowerCase());
        if (strategy == null) {
            log.warn("[策略工厂] 未找到动作策略: {}", actionName);
        }
        return strategy;
    }
    
    public boolean hasStrategy(String actionName) {
        return strategies.containsKey(actionName.toLowerCase());
    }
    
    public ActionResult execute(String actionName, com.guardianeye.iiot.model.Agent agent, String target) {
        ActionStrategy strategy = getStrategy(actionName);
        if (strategy == null) {
            return ActionResult.failure("未知动作: " + actionName);
        }
        return strategy.execute(agent, target);
    }
}
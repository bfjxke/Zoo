package com.guardianeye.iiot.service.action;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TalkActionStrategy implements ActionStrategy {
    
    @Override
    public ActionResult execute(Agent agent, String channel) {
        log.info("[策略-TALK] Agent:{} 在频道发言:{}", agent.getName(), channel);
        
        String ch = channel != null ? channel : "public";
        log.info("[策略-TALK] Agent阵营:{}, 当前节点:{}", agent.getFaction(), agent.getCurrentNode());
        
        if (ch.endsWith("_private")) {
            String faction = ch.replace("_private", "");
            if (!faction.equals(agent.getFaction())) {
                log.warn("[策略-TALK] <<< 无法在{}发言，阵营不匹配", ch);
                return ActionResult.failure("无法在 " + ch + " 发言，阵营不匹配");
            }
            String baseNode = GameConstants.FACTION_BASE.get(faction);
            if (!agent.getCurrentNode().equals(baseNode)) {
                log.warn("[策略-TALK] <<< 阵营私聊需在基地节点({})，当前在{}", baseNode, agent.getCurrentNode());
                return ActionResult.failure("阵营私聊需在基地节点(" + baseNode + ")，当前在 " + agent.getCurrentNode());
            }
        }
        
        if (!GameConstants.FACTION_CHANNELS.contains(ch)) {
            log.warn("[策略-TALK] <<< 无效频道:{}", ch);
            return ActionResult.failure("无效频道: " + ch);
        }
        
        log.info("[策略-TALK] <<< 在[{}]频道发言成功", ch);
        return ActionResult.success("在 [" + ch + "] 频道发言");
    }
    
    @Override
    public String getActionName() {
        return "talk";
    }
}
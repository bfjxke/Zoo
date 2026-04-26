package com.guardianeye.iiot.controller;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.SandboxStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/god")
@RequiredArgsConstructor
public class GodController {

    private final AgentRepository agentRepository;
    private final SandboxStateMachine stateMachine;

    @PostMapping("/airdrop")
    public ResponseEntity<String> airdrop(@RequestBody Map<String, Object> body) {
        String targetNode = (String) body.getOrDefault("target_node", "G");
        Integer foodAmount = (Integer) body.getOrDefault("food_amount", 50);
        
        // 给指定节点附近的所有存活Agent增加饱食度
        for (Agent agent : agentRepository.findByAliveTrue()) {
            // 检查Agent是否在目标节点或相邻节点
            if (agent.getCurrentNode().equals(targetNode) || 
                GameConstants.areAdjacent(agent.getCurrentNode(), targetNode)) {
                int oldSatiety = agent.getSatiety();
                int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, 
                    oldSatiety + foodAmount);
                agent.setSatiety(newSatiety);
                agentRepository.save(agent);
            }
        }
        
        return ResponseEntity.ok("上帝空投物资到 " + targetNode + "，食物量: " + foodAmount);
    }

    @PostMapping("/plague")
    public ResponseEntity<String> plague(@RequestBody Map<String, Object> body) {
        String targetFaction = (String) body.getOrDefault("target_faction", "all");
        Integer staminaPenalty = (Integer) body.getOrDefault("stamina_penalty", 30);

        return ResponseEntity.ok("上帝对 " + targetFaction + " 施加疲惫Buff，耐力惩罚: -" + staminaPenalty);
    }

    @PostMapping("/amnesty")
    public ResponseEntity<String> amnesty(@RequestBody Map<String, Object> body) {
        String targetAgent = (String) body.getOrDefault("target_agent", "all");

        if ("all".equals(targetAgent)) {
            for (Agent agent : agentRepository.findAll()) {
                if (!agent.getAlive()) {
                    agent.setAlive(true);
                    agent.setStamina(50);
                    agent.setSatiety(50);
                    agent.setHealth(50);
                    agent.setDeathTicksRemaining(0);
                    agentRepository.save(agent);
                }
            }
            return ResponseEntity.ok("上帝赦免！所有死亡Agent立即复活");
        }

        return ResponseEntity.ok("上帝赦免 " + targetAgent);
    }
}

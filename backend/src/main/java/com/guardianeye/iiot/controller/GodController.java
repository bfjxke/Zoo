package com.guardianeye.iiot.controller;

import com.guardianeye.iiot.service.GodModeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/god")
@RequiredArgsConstructor
public class GodController {

    private final GodModeService godModeService;

    @PostMapping("/airdrop")
    public ResponseEntity<String> airdrop(@RequestBody Map<String, Object> body) {
        String targetNode = (String) body.getOrDefault("target_node", "G");
        Integer foodAmount = (Integer) body.getOrDefault("food_amount", 50);
        
        int affectedCount = godModeService.airdropSupplies(targetNode, foodAmount);
        
        return ResponseEntity.ok(String.format(
            "上帝空投物资到 %s，食物量: %d，影响 %d 个Agent", 
            targetNode, foodAmount, affectedCount));
    }

    @PostMapping("/plague")
    public ResponseEntity<String> plague(@RequestBody Map<String, Object> body) {
        String targetFaction = (String) body.getOrDefault("target_faction", "all");
        Integer staminaPenalty = (Integer) body.getOrDefault("stamina_penalty", 30);
        
        int affectedCount = godModeService.applyPlague(targetFaction, staminaPenalty);
        
        return ResponseEntity.ok(String.format(
            "上帝对 %s 施加疲惫Buff，耐力惩罚: -%d，影响 %d 个Agent", 
            targetFaction, staminaPenalty, affectedCount));
    }

    @PostMapping("/amnesty")
    public ResponseEntity<String> amnesty(@RequestBody Map<String, Object> body) {
        String targetAgent = (String) body.getOrDefault("target_agent", "all");
        
        int pardonedCount = godModeService.grantAmnesty(targetAgent);
        
        if ("all".equals(targetAgent)) {
            return ResponseEntity.ok(String.format(
                "上帝赦免！%d 个死亡Agent立即复活", pardonedCount));
        } else {
            return ResponseEntity.ok(String.format(
                "上帝赦免 %s，%d 个Agent被赦免", targetAgent, pardonedCount));
        }
    }
}
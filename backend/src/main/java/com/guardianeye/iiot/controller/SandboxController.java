package com.guardianeye.iiot.controller;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.ActionLog;
import com.guardianeye.iiot.model.ActionLogRepository;
import com.guardianeye.iiot.model.GameState;
import com.guardianeye.iiot.service.SandboxStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxStateMachine stateMachine;
    private final AgentRepository agentRepository;
    private final ActionLogRepository actionLogRepository;

    @GetMapping("/state")
    public ResponseEntity<GameState> getState() {
        return ResponseEntity.ok(stateMachine.getCurrentState());
    }

    @PostMapping("/start")
    public ResponseEntity<GameState> startSimulation() {
        return ResponseEntity.ok(stateMachine.startSimulation());
    }

    @PostMapping("/stop")
    public ResponseEntity<GameState> stopSimulation() {
        return ResponseEntity.ok(stateMachine.stopSimulation());
    }

    @GetMapping("/agents")
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentRepository.findAll());
    }

    @GetMapping("/agents/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable Long id) {
        return agentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/agents/{id}/action")
    public ResponseEntity<Agent> executeAction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String target = body.get("target");
        Agent result = stateMachine.executeAction(id, action, target);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ActionLog>> getLogs(
            @RequestParam(required = false) Integer tick) {
        if (tick != null) {
            return ResponseEntity.ok(actionLogRepository.findByTickNumberOrderByTimestampAsc(tick));
        }
        return ResponseEntity.ok(actionLogRepository.findAll());
    }

    @PostMapping("/init")
    public ResponseEntity<String> initAgents() {
        if (agentRepository.count() > 0) {
            return ResponseEntity.ok("Agent已存在，跳过初始化");
        }

        String[][] lawfulAgents = {
                {"守序领袖", "lawful", "leader"},
                {"守序兵-Alpha", "lawful", "soldier"},
                {"守序兵-Beta", "lawful", "soldier"},
                {"守序兵-Gamma", "lawful", "soldier"},
                {"守序兵-Delta", "lawful", "soldier"}
        };

        String[][] aggressiveAgents = {
                {"强势领袖", "aggressive", "leader"},
                {"强势兵-Alpha", "aggressive", "soldier"},
                {"强势兵-Beta", "aggressive", "soldier"},
                {"强势兵-Gamma", "aggressive", "soldier"},
                {"强势兵-Delta", "aggressive", "soldier"}
        };

        String[][] neutralAgents = {
                {"中立领袖", "neutral", "leader"},
                {"中立兵-Alpha", "neutral", "soldier"},
                {"中立兵-Beta", "neutral", "soldier"},
                {"中立兵-Gamma", "neutral", "soldier"}
        };

        createAgents(lawfulAgents);
        createAgents(aggressiveAgents);
        createAgents(neutralAgents);

        stateMachine.getOrCreateGameState();

        return ResponseEntity.ok("15个Agent初始化完成！守序5 + 强势5 + 中立5");
    }

    private void createAgents(String[][] data) {
        for (String[] d : data) {
            Agent agent = new Agent();
            agent.setName(d[0]);
            agent.setFaction(d[1]);
            agent.setRole(d[2]);
            agent.setCurrentNode("base_" + d[1]);
            agentRepository.save(agent);
        }
    }
}

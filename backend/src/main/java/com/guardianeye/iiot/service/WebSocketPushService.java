package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AgentRepository agentRepository;
    private final SandboxStateMachine stateMachine;

    private long lastPushTime = 0;
    private static final long PUSH_INTERVAL_MS = 2000;

    public void pushTickUpdate() {
        long now = System.currentTimeMillis();
        if (now - lastPushTime < PUSH_INTERVAL_MS) {
            return;
        }
        lastPushTime = now;

        try {
            Map<String, Object> update = new HashMap<>();
            GameState gs = stateMachine.getCurrentState();
            update.put("tick", gs.getCurrentTick());
            update.put("running", gs.getRunning());

            List<Agent> agents = agentRepository.findAll();
            update.put("agents", agents.stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("name", a.getName());
                m.put("faction", a.getFaction());
                m.put("role", a.getRole());
                m.put("stamina", a.getStamina());
                m.put("satiety", a.getSatiety());
                m.put("health", a.getHealth());
                m.put("currentNode", a.getCurrentNode());
                m.put("alive", a.getAlive());
                m.put("fatigued", a.getFatigued());
                m.put("hungry", a.getHungry());
                return m;
            }).toList());

            messagingTemplate.convertAndSend("/topic/sandbox", update);
        } catch (Exception e) {
            log.warn("WebSocket推送失败: {}", e.getMessage());
        }
    }
}

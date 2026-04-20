package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PythonDispatcher {

    @Value("${python.agent.url}")
    private String pythonAgentUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String requestDecisions(int tick, List<Agent> aliveAgents) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tick", tick);
            payload.put("agent_count", aliveAgents.size());

            List<Map<String, Object>> agentData = aliveAgents.stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("name", a.getName());
                m.put("faction", a.getFaction());
                m.put("role", a.getRole());
                m.put("stamina", a.getStamina());
                m.put("satiety", a.getSatiety());
                m.put("health", a.getHealth());
                m.put("current_node", a.getCurrentNode());
                return m;
            }).toList();
            payload.put("agents", agentData);

            String url = pythonAgentUrl + "/decide";
            String response = restTemplate.postForObject(url, payload, String.class);
            log.info("[Tick #{}] Python调度器响应: {}", tick, response);
            return response;
        } catch (Exception e) {
            log.warn("[Tick #{}] Python调度器不可用: {}", tick, e.getMessage());
            return "{\"error\": \"Python调度器不可用\"}";
        }
    }
}

    package com.guardianeye.iiot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationScheduler {

    private final SandboxStateMachine stateMachine;
    private final AgentRepository agentRepository;
    private final RuleEngine ruleEngine;
    private final RestTemplateBuilder restTemplateBuilder;

    private final Map<Long, Random> agentRandoms = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.agent.url}")
    private String pythonAgentBaseUrl;

    private static final String DECIDE_ENDPOINT = "/decide";

    @PostConstruct
    public void validateConfiguration() {
        if (pythonAgentBaseUrl == null || pythonAgentBaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("配置项 python.agent.url 未设置或为空");
        }
        if (!pythonAgentBaseUrl.startsWith("http://") && !pythonAgentBaseUrl.startsWith("https://")) {
            log.warn("配置项 python.agent.url 可能无效: {}，请确保使用 http:// 或 https://", pythonAgentBaseUrl);
        }
        log.info("LLM服务地址: {}{}", pythonAgentBaseUrl, DECIDE_ENDPOINT);
    }

    private String getLLMServiceUrl() {
        return pythonAgentBaseUrl + DECIDE_ENDPOINT;
    }

    private Random getAgentRandom(Agent agent) {
        return agentRandoms.computeIfAbsent(agent.getId(), id -> {
            long seed = id * 31 + System.currentTimeMillis() % 1000;
            return new Random(seed);
        });
    }

    @Scheduled(fixedRate = 3000)
    public void scheduledTick() {
        GameState gameState = stateMachine.getOrCreateGameState();
        
        if (!gameState.getRunning()) {
            log.debug("[调度器] 游戏未运行，跳过Tick");
            return;
        }

        int currentTick = gameState.getCurrentTick();
        log.info("========================================");
        log.info("[调度器] ========== Tick #{} 开始 ==========", currentTick);
        log.info("========================================");
        
        stateMachine.executeTick();

        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        log.info("[调度器] 当前存活Agent数量: {}", aliveAgents.size());
        
        for (Agent agent : aliveAgents) {
            try {
                log.info("[调度器] === Agent决策开始 ===");
                log.info("[Agent] ID:{}, 名称:{}, 阵营:{}, 角色:{}", 
                    agent.getId(), agent.getName(), agent.getFaction(), agent.getRole());
                log.info("[Agent状态] 位置:{}, 耐力:{}, 饱食:{}, 健康:{}", 
                    agent.getCurrentNode(), agent.getStamina(), agent.getSatiety(), agent.getHealth());
                
                makeUniqueDecision(agent, currentTick);
                
                log.info("[Agent决策后] 位置:{}, 耐力:{}, 饱食:{}, 健康:{}", 
                    agent.getCurrentNode(), agent.getStamina(), agent.getSatiety(), agent.getHealth());
                log.info("[调度器] === Agent决策结束 ===");
            } catch (Exception e) {
                log.error("[错误] Agent {} 决策失败: {}", agent.getName(), e.getMessage(), e);
            }
        }

        if (currentTick % 3 == 0) {
            autoAirdrop();
        }
    }

    private void makeUniqueDecision(Agent agent, int currentTick) {
        log.info("[LLM调用] >>> 调用LLM API为Agent {} 做决策", agent.getName());
        
        try {
            Map<String, Object> decision = callLLMForDecision(agent, currentTick);
            
            if (decision != null && decision.containsKey("action")) {
                String action = (String) decision.get("action");
                String target = decision.containsKey("target") ? (String) decision.get("target") : null;
                String reasoning = decision.containsKey("reasoning") ? (String) decision.get("reasoning") : "";
                String modelUsed = decision.containsKey("model_used") ? (String) decision.get("model_used") : "unknown";
                
                log.info("[LLM响应] Agent:{}, 动作:{}, 目标:{}, 推理:{}", 
                    agent.getName(), action, target, reasoning);
                log.info("[LLM响应] 使用模型:{}", modelUsed);
                
                executeLLMDecision(agent, action, target);
            } else {
                log.warn("[LLM响应] Agent {} 返回无效决策，使用备用规则引擎", agent.getName());
                fallbackToRuleEngine(agent);
            }
        } catch (Exception e) {
            log.error("[LLM错误] Agent {} 调用LLM失败: {}，使用备用规则引擎", agent.getName(), e.getMessage());
            fallbackToRuleEngine(agent);
        }
    }

    private Map<String, Object> callLLMForDecision(Agent agent, int currentTick) {
        try {
            RestTemplate restTemplate = restTemplateBuilder.build();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("tick", currentTick);
            requestBody.put("agent_count", 1);
            
            List<Map<String, Object>> agentsList = new ArrayList<>();
            Map<String, Object> agentData = new HashMap<>();
            agentData.put("id", agent.getId());
            agentData.put("name", agent.getName());
            agentData.put("faction", agent.getFaction());
            agentData.put("role", agent.getRole());
            agentData.put("stamina", agent.getStamina());
            agentData.put("satiety", agent.getSatiety());
            agentData.put("health", agent.getHealth());
            agentData.put("current_node", agent.getCurrentNode());
            agentData.put("alive", agent.getAlive());
            agentData.put("personality", agent.getPersonality());
            agentsList.add(agentData);
            
            requestBody.put("agents", agentsList);
            
            String llmServiceUrl = getLLMServiceUrl();
            log.info("[LLM请求] 发送请求到 {}", llmServiceUrl);
            log.info("[LLM请求] 请求体: {}", requestBody);
            
            Map<String, Object> response = restTemplate.postForObject(
                llmServiceUrl,
                requestBody,
                Map.class
            );
            
            if (response != null && response.containsKey("decisions")) {
                List<Map<String, Object>> decisions = (List<Map<String, Object>>) response.get("decisions");
                if (!decisions.isEmpty()) {
                    return decisions.get(0);
                }
            }
            
            log.warn("[LLM响应] 响应格式无效或为空");
            return null;
            
        } catch (Exception e) {
            log.error("[LLM调用] 网络错误: {}", e.getMessage());
            return null;
        }
    }

    private void executeLLMDecision(Agent agent, String action, String target) {
        log.info("[执行器] >>> Agent {} 执行动作 action:{}, target:{}", agent.getName(), action, target);
        
        switch (action.toLowerCase()) {
            case "move":
                ruleEngine.validateAndExecute(agent, "move", target);
                break;
            case "eat":
                ruleEngine.validateAndExecute(agent, "eat", null);
                break;
            case "rest":
                ruleEngine.validateAndExecute(agent, "rest", null);
                break;
            case "talk":
                ruleEngine.validateAndExecute(agent, "talk", target != null ? target : "public");
                break;
            case "trade":
                ruleEngine.validateAndExecute(agent, "trade", target);
                break;
            case "provoke":
                ruleEngine.validateAndExecute(agent, "provoke", target);
                break;
            default:
                log.warn("[执行器] 未知动作 {}，执行休息", action);
                ruleEngine.validateAndExecute(agent, "rest", null);
        }
    }

    private void fallbackToRuleEngine(Agent agent) {
        log.info("[备用引擎] Agent {} 使用规则引擎决策", agent.getName());
        
        Random rand = getAgentRandom(agent);
        String faction = agent.getFaction();
        String role = agent.getRole();
        
        if (agent.getSatiety() < 30) {
            ruleEngine.validateAndExecute(agent, "eat", null);
            return;
        }
        
        if (agent.getStamina() < 20) {
            ruleEngine.validateAndExecute(agent, "rest", null);
            return;
        }
        
        int action = rand.nextInt(100);
        
        if ("lawful".equals(faction)) {
            if (action < 30) {
                ruleEngine.validateAndExecute(agent, "move", "D");
            } else if (action < 50) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else {
                ruleEngine.validateAndExecute(agent, "rest", null);
            }
        } else if ("aggressive".equals(faction)) {
            if (action < 50) {
                ruleEngine.validateAndExecute(agent, "move", "E");
            } else if (action < 70) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else {
                ruleEngine.validateAndExecute(agent, "rest", null);
            }
        } else {
            if (action < 40) {
                ruleEngine.validateAndExecute(agent, "move", "G");
            } else if (action < 60) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else {
                ruleEngine.validateAndExecute(agent, "rest", null);
            }
        }
    }

    private void autoAirdrop() {
        for (String node : Arrays.asList("D", "E")) {
            for (Agent agent : agentRepository.findByAliveTrue()) {
                if (agent.getCurrentNode().equals(node)) {
                    int foodAmount = 8;
                    int oldSatiety = agent.getSatiety();
                    int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, 
                        oldSatiety + foodAmount);
                    agent.setSatiety(newSatiety);
                    agentRepository.save(agent);
                }
            }
        }
    }
}

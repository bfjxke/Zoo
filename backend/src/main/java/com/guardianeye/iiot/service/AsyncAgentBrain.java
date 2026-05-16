package com.guardianeye.iiot.service;

import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class AsyncAgentBrain {
    
    @Value("${python.agent.url}")
    private String pythonAgentUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    @PreDestroy
    public void shutdown() {
        log.info("关闭AsyncAgentBrain线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("AsyncAgentBrain线程池已关闭");
    }
    
    public List<AgentDecision> thinkAllSync(List<Map<String, Object>> agents) {
        List<CompletableFuture<AgentDecision>> futures = new ArrayList<>();
        
        for (Map<String, Object> agent : agents) {
            CompletableFuture<AgentDecision> future = CompletableFuture.supplyAsync(
                () -> thinkAsync(agent), 
                executor
            );
            futures.add(future);
        }
        
        List<AgentDecision> results = new ArrayList<>();
        for (CompletableFuture<AgentDecision> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("Agent决策失败", e);
                results.add(fallbackDecision(null));
            }
        }
        
        return results;
    }
    
    private AgentDecision thinkAsync(Map<String, Object> agent) {
        try {
            String url = pythonAgentUrl + "/decide";
            Map<String, Object> request = Map.of(
                "tick", 0,
                "agent", agent,
                "agent_count", 1
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("decisions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> decisions = (List<Map<String, Object>>) response.get("decisions");
                if (!decisions.isEmpty()) {
                    Map<String, Object> d = decisions.get(0);
                    return AgentDecision.builder()
                        .action((String) d.get("action"))
                        .target((String) d.get("target"))
                        .reasoning((String) d.get("reasoning"))
                        .successRate(1.0)
                        .build();
                }
            }
            
            return fallbackDecision(agent);
            
        } catch (Exception e) {
            log.warn("Agent {} 决策失败，使用默认决策: {}", agent.get("name"), e.getMessage());
            return fallbackDecision(agent);
        }
    }
    
    private AgentDecision fallbackDecision(Map<String, Object> agent) {
        return AgentDecision.builder()
            .action("rest")
            .target(null)
            .reasoning("LLM调用失败，默认休息")
            .successRate(1.0)
            .build();
    }
    
    @Data
    @lombok.Builder
    public static class AgentDecision {
        private String action;
        private String target;
        private String reasoning;
        private Double successRate;
    }
}
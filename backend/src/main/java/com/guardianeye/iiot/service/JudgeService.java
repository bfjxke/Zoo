package com.guardianeye.iiot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class JudgeService {
    
    @Value("${python.agent.url}")
    private String pythonAgentUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Data
    public static class JudgeResult {
        private boolean approved;
        private int confinementTicks;
        private String reasoning;
    }
    
    private final Map<String, CachedJudgeResult> cache = new ConcurrentHashMap<>();
    private static final int CACHE_TICKS = 8;
    
    private static class CachedJudgeResult {
        int tick;
        JudgeResult result;
        
        CachedJudgeResult(int tick, JudgeResult result) {
            this.tick = tick;
            this.result = result;
        }
    }
    
    public JudgeResult judgeAction(Long agentId, String agentName, String agentFaction,
                                   String action, String target, int currentTick) {
        String cacheKey = agentId + "_" + action;
        
        CachedJudgeResult cached = cache.get(cacheKey);
        if (cached != null && currentTick - cached.tick < CACHE_TICKS) {
            log.info("[AI判官] 命中缓存 {} -> approved={}", cacheKey, cached.result.isApproved());
            return cached.result;
        }
        
        try {
            String url = pythonAgentUrl + "/judge";
            Map<String, Object> request = new HashMap<>();
            request.put("agent_id", agentId);
            request.put("agent_name", agentName);
            request.put("agent_faction", agentFaction);
            request.put("action", action);
            request.put("target", target);
            request.put("context", "当前状态判断");
            request.put("current_tick", currentTick);
            request.put("recent_logs", Collections.emptyList());
            
            log.info("[AI判官] 发送请求到 {} with action={}", url, action);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null) {
                JudgeResult result = new JudgeResult();
                result.setApproved((Boolean) response.getOrDefault("approved", false));
                result.setConfinementTicks(((Number) response.getOrDefault("confinement_ticks", 0)).intValue());
                result.setReasoning((String) response.getOrDefault("reasoning", ""));
                
                cache.put(cacheKey, new CachedJudgeResult(currentTick, result));
                log.info("[AI判官] 结果 approved={} confinement={}",
                    result.isApproved(), result.getConfinementTicks());
                
                return result;
            }
        } catch (Exception e) {
            log.warn("[AI判官] 调用失败，使用默认结果: {}", e.getMessage());
        }
        
        JudgeResult fallbackResult = new JudgeResult();
        fallbackResult.setApproved(false);
        fallbackResult.setConfinementTicks(2);
        fallbackResult.setReasoning("AI判官服务不可用，默认拒绝");
        
        return fallbackResult;
    }
    
    public void clearCache() {
        cache.clear();
        log.info("[AI判官] 缓存已清空");
    }
}
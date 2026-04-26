package com.guardianeye.iiot.observer;

import com.guardianeye.iiot.model.ActionLog;
import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket观察者
 * 
 * 负责通过WebSocket向前端推送实时数据
 * 
 * 功能：
 * - 推送Agent状态更新
 * - 推送动作日志
 * - 推送游戏状态
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketObserver implements GameObserver {
    
    /**
     * WebSocket消息模板
     * 用于向订阅者推送消息
     */
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void onTickComplete(int tick, GameState state, List<ActionLog> actions) {
        // 推送游戏状态更新
        messagingTemplate.convertAndSend("/topic/game/state", state);
        
        // 推送最新动作
        messagingTemplate.convertAndSend("/topic/game/actions", actions);
        
        log.debug("[WebSocketObserver] 推送Tick #{} 状态完成", tick);
    }
    
    @Override
    public void onAgentAction(ActionLog action) {
        // 推送单个动作（实时显示）
        messagingTemplate.convertAndSend("/topic/action", action);
    }
    
    @Override
    public void onGameEnd(int tick, String reason, String winner) {
        // 推送游戏结束
        messagingTemplate.convertAndSend("/topic/game/end", 
            new GameEndMessage(tick, reason, winner));
        
        log.info("[WebSocketObserver] 推送游戏结束消息");
    }
    
    @Override
    public void onPeaceEnding(int tick, String winner) {
        // 推送和平结局
        messagingTemplate.convertAndSend("/topic/game/peace", 
            new PeaceEndingMessage(tick, winner));
        
        log.info("[WebSocketObserver] 推送和平结局消息");
    }
    
    /**
     * 游戏结束消息
     */
    public record GameEndMessage(int tick, String reason, String winner) {}
    
    /**
     * 和平结局消息
     */
    public record PeaceEndingMessage(int tick, String winner) {}
}

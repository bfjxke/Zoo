package com.guardianeye.iiot.observer;

import com.guardianeye.iiot.model.ActionLog;
import com.guardianeye.iiot.model.GameState;
import java.util.List;

/**
 * 游戏观察者接口
 * 
 * 观察者模式核心接口
 * 所有观察者必须实现此接口
 * 
 * 使用场景：
 * - 数据库持久化（DatabaseObserver）
 * - WebSocket推送（WebSocketObserver）
 * - 日志记录（LogObserver）
 * - 指标统计（MetricsObserver）
 */
public interface GameObserver {
    
    /**
     * Tick结算完成时通知
     * @param tick 回合数
     * @param state 当前游戏状态
     * @param actions 本回合所有动作
     */
    void onTickComplete(int tick, GameState state, List<ActionLog> actions);
    
    /**
     * Agent动作执行时通知
     * @param action 动作日志
     */
    void onAgentAction(ActionLog action);
    
    /**
     * 游戏结束时通知
     * @param tick 结束回合
     * @param reason 结束原因
     * @param winner 获胜方（如果有）
     */
    void onGameEnd(int tick, String reason, String winner);
    
    /**
     * 和平结局触发时通知
     * @param tick 触发回合
     * @param winner 获胜阵营
     */
    void onPeaceEnding(int tick, String winner);
}

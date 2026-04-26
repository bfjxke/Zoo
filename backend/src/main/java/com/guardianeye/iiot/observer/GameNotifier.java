package com.guardianeye.iiot.observer;

import com.guardianeye.iiot.model.ActionLog;
import com.guardianeye.iiot.model.GameState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏通知中心
 * 
 * 观察者模式的核心类
 * 负责管理所有观察者并发送通知
 * 
 * 设计思想：
 * - 单例模式：通过Spring容器管理
 * - 线程安全：使用CopyOnWriteArrayList
 * - 动态注册：运行时添加/删除观察者
 */
@Component
@Slf4j
public class GameNotifier {
    
    /**
     * 观察者列表
     * CopyOnWriteArrayList：读写分离，适合读多写少场景
     */
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();
    
    /**
     * 添加观察者
     * @param observer 要添加的观察者
     */
    public void addObserver(GameObserver observer) {
        if (observer != null) {
            observers.add(observer);
            log.info("[观察者] 已添加观察者: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * 移除观察者
     * @param observer 要移除的观察者
     */
    public void removeObserver(GameObserver observer) {
        if (observers.remove(observer)) {
            log.info("[观察者] 已移除观察者: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * 获取当前观察者数量
     * @return 观察者数量
     */
    public int getObserverCount() {
        return observers.size();
    }
    
    /**
     * 通知所有观察者Tick结算完成
     * @param tick 回合数
     * @param state 游戏状态
     * @param actions 所有动作
     */
    public void notifyTickComplete(int tick, GameState state, List<ActionLog> actions) {
        for (GameObserver observer : observers) {
            try {
                observer.onTickComplete(tick, state, actions);
            } catch (Exception e) {
                log.error("[观察者] {} 执行onTickComplete失败: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * 通知所有观察者Agent动作
     * @param action 动作日志
     */
    public void notifyAgentAction(ActionLog action) {
        for (GameObserver observer : observers) {
            try {
                observer.onAgentAction(action);
            } catch (Exception e) {
                log.error("[观察者] {} 执行onAgentAction失败: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * 通知所有观察者游戏结束
     * @param tick 结束回合
     * @param reason 结束原因
     * @param winner 获胜方
     */
    public void notifyGameEnd(int tick, String reason, String winner) {
        for (GameObserver observer : observers) {
            try {
                observer.onGameEnd(tick, reason, winner);
            } catch (Exception e) {
                log.error("[观察者] {} 执行onGameEnd失败: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * 通知所有观察者和平结局
     * @param tick 触发回合
     * @param winner 获胜阵营
     */
    public void notifyPeaceEnding(int tick, String winner) {
        for (GameObserver observer : observers) {
            try {
                observer.onPeaceEnding(tick, winner);
            } catch (Exception e) {
                log.error("[观察者] {} 执行onPeaceEnding失败: {}", 
                    observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}

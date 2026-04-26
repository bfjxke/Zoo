package com.guardianeye.iiot.logger;

/**
 * 日志处理器接口
 * 
 * 责任链模式的核心接口
 * 每个Handler负责一种日志处理逻辑
 * 
 * 设计思想：
 * - 每个Handler只做一件事
 * - Handler可以链式组合
 * - 可以动态添加/删除Handler
 * - 可以跳过某个Handler
 */
public interface LogHandler {
    
    /**
     * 处理日志条目
     * @param entry 日志条目
     */
    void handle(LogEntry entry);
    
    /**
     * 设置下一个处理器（链式调用）
     * @param next 下一个处理器
     * @return 下一个处理器（方便链式调用）
     */
    LogHandler setNext(LogHandler next);
}

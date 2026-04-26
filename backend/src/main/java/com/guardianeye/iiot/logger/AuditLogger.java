package com.guardianeye.iiot.logger;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计日志管理器
 * 
 * 责任链模式的使用者
 * 组装日志处理链路
 * 
 * 默认链路：
 * ConsoleHandler -> FileHandler -> ViolationHandler
 * 
 * 使用示例：
 * auditLogger.logAction(1, "张三", "lawful", "move", "center");
 * auditLogger.logViolation(2, "李四", "aggressive", "attack", "目标不相邻");
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {
    
    /**
     * 责任链头部
     */
    private final ConsoleHandler consoleHandler;
    
    /**
     * 文件处理器
     */
    private final FileHandler fileHandler;
    
    /**
     * 违规处理器
     */
    private final ViolationHandler violationHandler;
    
    /**
     * 初始化责任链
     */
    @PostConstruct
    public void init() {
        // 组装责任链：控制台 -> 文件 -> 违规
        consoleHandler.setNext(fileHandler);
        fileHandler.setNext(violationHandler);
        
        log.info("[AuditLogger] 责任链初始化完成");
    }
    
    /**
     * 记录Agent动作
     * @param tick 回合数
     * @param agent Agent名称
     * @param faction 阵营
     * @param action 执行的动作
     * @param result 执行结果
     */
    public void logAction(int tick, String agent, String faction, String action, String result) {
        LogEntry entry = LogEntry.action(tick, agent, faction, action, result);
        consoleHandler.handle(entry);
    }
    
    /**
     * 记录违规操作
     * @param tick 回合数
     * @param agent Agent名称
     * @param faction 阵营
     * @param action 尝试的动作
     * @param reason 违规原因
     */
    public void logViolation(int tick, String agent, String faction, String action, String reason) {
        LogEntry entry = LogEntry.violation(tick, agent, faction, action, reason);
        consoleHandler.handle(entry);
    }
    
    /**
     * 记录系统日志
     * @param tick 回合数
     * @param message 系统消息
     */
    public void logSystem(int tick, String message) {
        LogEntry entry = LogEntry.system(tick, message);
        consoleHandler.handle(entry);
    }
}

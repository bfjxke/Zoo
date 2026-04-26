package com.guardianeye.iiot.logger;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class LogEntry {
    private int tick;
    private String agent;
    private String faction;
    private String action;
    private String result;
    private String judgeId;
    private Double successRate;
    private boolean violation;
    private String violationReason;
    private boolean system;
    private String systemMessage;

    public static LogEntry action(int tick, String agent, String faction, String action, String result) {
        LogEntry entry = new LogEntry();
        entry.tick = tick;
        entry.agent = agent;
        entry.faction = faction;
        entry.action = action;
        entry.result = result;
        entry.violation = false;
        entry.system = false;
        return entry;
    }

    public static LogEntry violation(int tick, String agent, String faction, String action, String reason) {
        LogEntry entry = new LogEntry();
        entry.tick = tick;
        entry.agent = agent;
        entry.faction = faction;
        entry.action = action;
        entry.result = "违规: " + reason;
        entry.judgeId = "VIOLATION";
        entry.violation = true;
        entry.violationReason = reason;
        entry.system = false;
        return entry;
    }

    public static LogEntry system(int tick, String message) {
        LogEntry entry = new LogEntry();
        entry.tick = tick;
        entry.systemMessage = message;
        entry.system = true;
        return entry;
    }
}

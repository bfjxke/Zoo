package com.guardianeye.iiot.logger;

import org.springframework.stereotype.Component;

@Component
public class ConsoleHandler implements LogHandler {
    private LogHandler next;

    @Override
    public LogHandler setNext(LogHandler next) {
        this.next = next;
        return next;
    }

    @Override
    public void handle(LogEntry entry) {
        if (entry.isSystem()) {
            System.out.printf("[SYSTEM] [Tick #%d] %s%n", entry.getTick(), entry.getSystemMessage());
        } else {
            System.out.printf("[Tick #%d][%s][%s][%s] -> %s%n",
                entry.getTick(),
                entry.getAgent(),
                entry.getFaction(),
                entry.getAction(),
                entry.getResult());
        }
        if (next != null) {
            next.handle(entry);
        }
    }
}

package com.guardianeye.iiot.logger;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class FileHandler implements LogHandler {
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private LogHandler next;

    @PostConstruct
    public void init() {
        Path path = Paths.get(LOG_DIR);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("创建日志目录失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public LogHandler setNext(LogHandler next) {
        this.next = next;
        return next;
    }

    @Override
    public void handle(LogEntry entry) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String filename = String.format("%s/audit-%s.log", LOG_DIR, date);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            if (entry.isSystem()) {
                writer.printf("[SYSTEM] [Tick #%d] %s%n", entry.getTick(), entry.getSystemMessage());
            } else {
                writer.printf("[Tick #%d][%s][%s][%s] -> %s%n",
                    entry.getTick(), entry.getAgent(), entry.getFaction(), entry.getAction(), entry.getResult());
            }
        } catch (IOException e) {
            log.error("写入日志文件失败: {}", e.getMessage());
        }
        if (next != null) {
            next.handle(entry);
        }
    }
}

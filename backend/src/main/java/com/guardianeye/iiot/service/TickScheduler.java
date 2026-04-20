package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickScheduler {

    private final SandboxStateMachine stateMachine;
    private final WebSocketPushService webSocketPushService;

    @Scheduled(fixedRate = 30000)
    public void tick() {
        GameState gs = stateMachine.getCurrentState();
        if (!gs.getRunning()) {
            return;
        }
        stateMachine.executeTick();
        webSocketPushService.pushTickUpdate();
    }
}

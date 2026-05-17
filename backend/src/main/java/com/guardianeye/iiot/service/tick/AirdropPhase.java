package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AirdropPhase implements TickPhase {
    
    @Override
    public void execute(GameState gameState, int tick) {
        if (gameState.getCurrentTick() > 0 && gameState.getCurrentTick() % 11 == 0) {
            gameState.dropFood("D", 30);
            gameState.dropFood("E", 30);
            log.info("[空投] 第{}轮空投物资：D节点+30份，E节点+30份", gameState.getCurrentTick());
        }
    }
    
    @Override
    public int getOrder() {
        return 5;
    }
    
    @Override
    public String getName() {
        return "airdrop";
    }
}
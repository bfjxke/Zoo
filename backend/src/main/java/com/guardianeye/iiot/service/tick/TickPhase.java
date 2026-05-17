package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.GameState;

public interface TickPhase {
    
    void execute(GameState gameState, int tick);
    
    int getOrder();
    
    String getName();
}
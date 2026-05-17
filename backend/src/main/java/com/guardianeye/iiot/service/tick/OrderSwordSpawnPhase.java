package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSwordSpawnPhase implements TickPhase {
    
    @Override
    public void execute(GameState gameState, int tick) {
        if (!gameState.getOrderSwordSpawned() && gameState.getCurrentTick() >= 40) {
            List<String> spawnNodes = List.of("D", "E", "F", "G", "H");
            String spawnNode = spawnNodes.get(new Random().nextInt(spawnNodes.size()));
            gameState.setOrderSwordLocation(spawnNode);
            gameState.setOrderSwordSpawned(true);
            log.info("[秩序之剑] 第{}回合，在 {} 生成！", gameState.getCurrentTick(), spawnNode);
        }
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
    
    @Override
    public String getName() {
        return "orderSwordSpawn";
    }
}
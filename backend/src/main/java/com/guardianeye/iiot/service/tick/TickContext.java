package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TickContext {
    private GameState gameState;
    private int tick;
    private List<Agent> aliveAgents;
    private List<Agent> deadAgents;
}
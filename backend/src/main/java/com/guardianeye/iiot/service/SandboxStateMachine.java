package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandboxStateMachine {

    private final AgentRepository agentRepository;
    private final ActionLogRepository actionLogRepository;
    private final GameStateRepository gameStateRepository;
    private final RuleEngine ruleEngine;
    private final PythonDispatcher pythonDispatcher;

    @Transactional
    public GameState getOrCreateGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (states.isEmpty()) {
            GameState gs = new GameState();
            gs.setCurrentTick(0);
            gs.setRunning(false);
            gs.setLastTickTime(LocalDateTime.now());
            return gameStateRepository.save(gs);
        }
        return states.get(0);
    }

    @Transactional
    public void executeTick() {
        GameState gameState = getOrCreateGameState();
        if (!gameState.getRunning()) {
            log.info("[Tick #{}] 模拟未运行，跳过结算", gameState.getCurrentTick());
            return;
        }

        int tick = gameState.getCurrentTick() + 1;
        gameState.setCurrentTick(tick);
        gameState.setLastTickTime(LocalDateTime.now());
        log.info("========== [Tick #{}] 开始结算 ==========", tick);

        // 阶段1: 被动消耗（所有存活Agent）
        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        for (Agent agent : aliveAgents) {
            applyPassiveConsumption(agent, tick);
        }
        agentRepository.saveAll(aliveAgents);

        // 阶段2: 死亡Agent复活倒计时
        List<Agent> deadAgents = agentRepository.findAll().stream()
                .filter(a -> !a.getAlive())
                .toList();
        for (Agent agent : deadAgents) {
            processRespawn(agent, tick);
        }
        agentRepository.saveAll(deadAgents);

        // 阶段3: 调度Python获取Agent决策
        List<Agent> currentAlive = agentRepository.findByAliveTrue();
        if (!currentAlive.isEmpty()) {
            try {
                String decisions = pythonDispatcher.requestDecisions(tick, currentAlive);
                log.info("[Tick #{}] Python调度器返回决策: {}", tick, decisions);
            } catch (Exception e) {
                log.warn("[Tick #{}] Python调度器调用失败: {}", tick, e.getMessage());
            }
        }

        // 阶段4: 保存游戏状态
        gameStateRepository.save(gameState);
        log.info("========== [Tick #{}] 结算完成 ==========", tick);
    }

    private void applyPassiveConsumption(Agent agent, int tick) {
        double staminaCost = GameConstants.STAMINA_BASE_COST;
        double satietyCost = GameConstants.SATIETY_BASE_COST;

        agent.applyFatigueMultiplier();

        if (agent.isFatigued()) {
            staminaCost *= GameConstants.PENALTY_MULTIPLIER;
            satietyCost *= GameConstants.PENALTY_MULTIPLIER;
        }
        if (agent.isHungry()) {
            staminaCost *= GameConstants.PENALTY_MULTIPLIER;
            satietyCost *= GameConstants.PENALTY_MULTIPLIER;
        }

        agent.setStamina(Math.max(0, (int)(agent.getStamina() - staminaCost)));
        agent.setSatiety(Math.max(0, (int)(agent.getSatiety() - satietyCost)));

        if (agent.isHungry()) {
            agent.setHealth(Math.max(0, agent.getHealth() - GameConstants.HEALTH_HUNGER_DAMAGE));
            logAction(tick, agent.getName(), agent.getFaction(), "HUNGER_DAMAGE",
                    "饥饿扣血 -" + GameConstants.HEALTH_HUNGER_DAMAGE, null, null);
        }

        if (agent.isDead()) {
            agent.setAlive(false);
            agent.setDeathTicksRemaining(GameConstants.RESPAWN_TICKS);
            String baseNode = GameConstants.FACTION_BASE.getOrDefault(agent.getFaction(), "center");
            agent.setCurrentNode(baseNode);
            logAction(tick, agent.getName(), agent.getFaction(), "DEATH",
                    "健康归零，进入死亡复活流程，复活位置: " + baseNode, null, null);
        }

        agent.setFatigued(agent.isFatigued());
        agent.setHungry(agent.isHungry());
        agent.setTickCount(tick);
    }

    private void processRespawn(Agent agent, int tick) {
        if (agent.getDeathTicksRemaining() > 0) {
            agent.setDeathTicksRemaining(agent.getDeathTicksRemaining() - 1);
            logAction(tick, agent.getName(), agent.getFaction(), "RESPAWN_COUNTDOWN",
                    "复活倒计时剩余 " + agent.getDeathTicksRemaining() + " Tick", null, null);
        }

        if (agent.getDeathTicksRemaining() <= 0) {
            agent.setAlive(true);
            agent.setStamina(GameConstants.STAMINA_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setSatiety(GameConstants.SATIETY_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setHealth(GameConstants.HEALTH_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
            agent.setFatigued(false);
            agent.setHungry(false);
            String baseNode = GameConstants.FACTION_BASE.getOrDefault(agent.getFaction(), "center");
            agent.setCurrentNode(baseNode);
            logAction(tick, agent.getName(), agent.getFaction(), "RESPAWN",
                    "复活！状态重置为50%，位置: " + baseNode, null, null);
        }
    }

    @Transactional
    public Agent executeAction(Long agentId, String action, String target) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId));

        if (!agent.getAlive()) {
            throw new RuntimeException("Agent已死亡，无法执行动作");
        }

        GameState gs = getOrCreateGameState();
        int tick = gs.getCurrentTick();

        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(agent, action, target);

        if (result.isSuccess()) {
            agentRepository.save(agent);
        }

        logAction(tick, agent.getName(), agent.getFaction(), action,
                result.getMessage(), result.getJudgeId(), result.getSuccessRate());

        return agent;
    }

    private void logAction(int tick, String agentName, String faction,
                           String action, String result, String judgeId, Double successRate) {
        ActionLog actionLog = new ActionLog();
        actionLog.setTickNumber(tick);
        actionLog.setAgentName(agentName);
        actionLog.setFaction(faction);
        actionLog.setAction(action);
        actionLog.setResult(result);
        actionLog.setJudgeId(judgeId);
        actionLog.setSuccessRate(successRate);
        actionLogRepository.save(actionLog);
        log.info("[Tick #{}][{}][{}][{}] -> {} | {}[{}]",
                tick, agentName, faction, action, result,
                judgeId != null ? "Judge:" + judgeId + " " : "",
                successRate != null ? successRate : "");
    }

    @Transactional
    public GameState startSimulation() {
        GameState gs = getOrCreateGameState();
        gs.setRunning(true);
        return gameStateRepository.save(gs);
    }

    @Transactional
    public GameState stopSimulation() {
        GameState gs = getOrCreateGameState();
        gs.setRunning(false);
        return gameStateRepository.save(gs);
    }

    public GameState getCurrentState() {
        return getOrCreateGameState();
    }

    public AgentStatus computeStatus(Agent agent) {
        if (!agent.getAlive()) {
            return agent.getDeathTicksRemaining() > 0
                    ? AgentStatus.RESPAWNING
                    : AgentStatus.DEAD;
        }
        if (agent.isFatigued() && agent.isHungry()) return AgentStatus.CRITICAL;
        if (agent.isFatigued()) return AgentStatus.FATIGUED;
        if (agent.isHungry()) return AgentStatus.HUNGRY;
        return AgentStatus.NORMAL;
    }
}

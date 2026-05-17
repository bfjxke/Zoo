package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.*;
import com.guardianeye.iiot.service.tick.TickPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final VoteRepository voteRepository;
    private final List<TickPhase> tickPhases;

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
    public GameState resetGameState() {
        List<GameState> states = gameStateRepository.findAll();
        if (!states.isEmpty()) {
            GameState gs = states.get(0);
            gs.setCurrentTick(0);
            gs.setRunning(false);
            gs.setOrderSwordSpawned(false);
            gs.setOrderSwordHolderId(null);
            gs.setOrderDeclarationActive(false);
            gs.setLastDeclarationTick(0);
            gs.setLastTickTime(LocalDateTime.now());
            gs.setFoodInventory(java.util.Map.of(
                "lawful", 20,
                "aggressive", 20,
                "neutral", 20
            ));
            gs.setFoodDropLocations(new java.util.HashMap<>());
            return gameStateRepository.save(gs);
        }
        return getOrCreateGameState();
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

        tickPhases.stream()
                .sorted(Comparator.comparingInt(TickPhase::getOrder))
                .forEach(phase -> {
                    log.info("[Tick阶段] 执行: {}", phase.getName());
                    phase.execute(gameState, tick);
                });

        gameStateRepository.save(gameState);
        log.info("========== [Tick #{}] 结算完成 ==========", tick);
    }

    @Transactional
    public void publishOrderDeclaration(Long agentId) {
        GameState gs = getOrCreateGameState();
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId));
        
        if (gs.getCurrentTick() - gs.getLastDeclarationTick() < gs.getDeclarationCooldown()) {
            throw new RuntimeException("宣言冷却中，还需 " + 
                (gs.getDeclarationCooldown() - (gs.getCurrentTick() - gs.getLastDeclarationTick())) + " 回合");
        }
        
        if (!"lawful".equals(agent.getFaction())) {
            throw new RuntimeException("只有守序阵营可以发布秩序宣言");
        }
        if (!gs.getOrderSwordHolderId().equals(agentId)) {
            throw new RuntimeException("需要持有秩序之剑才能发布宣言");
        }
        
        gs.setOrderDeclarationActive(true);
        gs.setLastDeclarationTick(gs.getCurrentTick());
        gameStateRepository.save(gs);
        log.info("[秩序宣言] {} 发布了秩序宣言！", agent.getName());
    }

    @Transactional
    public void castVote(Long agentId, boolean agree) {
        GameState gs = getOrCreateGameState();
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId));
        
        if (!gs.getOrderDeclarationActive()) {
            throw new RuntimeException("当前没有激活的宣言");
        }
        
        if (!"aggressive".equals(agent.getFaction())) {
            throw new RuntimeException("只有激进阵营可以投票");
        }
        
        Vote vote = new Vote();
        vote.setTickNumber(gs.getCurrentTick());
        vote.setAgentName(agent.getName());
        vote.setDeclarationTick(gs.getLastDeclarationTick());
        vote.setVoteResult(agree);
        voteRepository.save(vote);
        
        log.info("[投票] {} 投票: {}", agent.getName(), agree ? "同意" : "拒绝");
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

        ActionResult result = ruleEngine.validateAndExecute(agent, action, target);

        if (result.isSuccess()) {
            if ("move".equals(action) && gs.getOrderSwordHolderId() != null && 
                gs.getOrderSwordHolderId().equals(agent.getId())) {
                gs.setOrderSwordLocation(target);
                log.info("[秩序之剑] {} 携带剑移动到 {}", agent.getName(), target);
            }
            
            checkOrderSwordPickup(agent, gs);
            agentRepository.save(agent);
        }

        logAction(tick, agent.getName(), agent.getFaction(), action,
                result.getMessage(), result.getJudgeId(), result.getSuccessRate());

        return agent;
    }

    private void checkOrderSwordPickup(Agent agent, GameState gameState) {
        if (gameState.getOrderSwordSpawned() && 
            gameState.getOrderSwordHolderId() == null &&
            gameState.getOrderSwordLocation().equals(agent.getCurrentNode())) {
            gameState.setOrderSwordHolderId(agent.getId());
            log.info("[秩序之剑] {} 拾取了秩序之剑！", agent.getName());
        }
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
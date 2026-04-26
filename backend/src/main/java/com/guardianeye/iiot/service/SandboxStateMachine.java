package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

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

        // 阶段1: 被动消耗（所有存活Agent）
        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        for (Agent agent : aliveAgents) {
            applyPassiveConsumption(agent, tick);
        }
        agentRepository.saveAll(aliveAgents);

        // 阶段1.5: 检查秩序之剑生成（第40回合）
        checkOrderSwordSpawn(gameState);

        // 阶段2: 死亡Agent复活倒计时
        List<Agent> deadAgents = agentRepository.findAll().stream()
                .filter(a -> !a.getAlive())
                .toList();
        for (Agent agent : deadAgents) {
            processRespawn(agent, tick);
        }
        agentRepository.saveAll(deadAgents);

        // 阶段2.5: 检查和平结局
        checkPeaceEnding(gameState);

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
        if (tick % 3 == 0) {
            agent.setSatiety(Math.max(0, (int)(agent.getSatiety() - GameConstants.SATIETY_BASE_COST)));
        }

        if (agent.isHungry()) {
            agent.setHealth(Math.max(0, agent.getHealth() - GameConstants.HEALTH_HUNGER_DAMAGE));
            logAction(tick, agent.getName(), agent.getFaction(), "HUNGER_DAMAGE",
                    "饥饿扣血 -" + GameConstants.HEALTH_HUNGER_DAMAGE, null, null);
        }

        int healthRegen = 0;
        if (agent.getSatiety() > GameConstants.SATIETY_BUFF_THRESHOLD) {
            healthRegen = GameConstants.HEALTH_REGEN_BUFF;
            logAction(tick, agent.getName(), agent.getFaction(), "HEALTH_REGEN",
                    "饱餐Buff回血 +" + healthRegen, null, null);
        } else if (agent.getSatiety() > GameConstants.HEALTH_REGEN_SATIETY_THRESHOLD) {
            healthRegen = GameConstants.HEALTH_REGEN_NORMAL;
            logAction(tick, agent.getName(), agent.getFaction(), "HEALTH_REGEN",
                    "饱食回血 +" + healthRegen, null, null);
        }

        if (healthRegen > 0) {
            int oldHealth = agent.getHealth();
            agent.setHealth(Math.min(GameConstants.HEALTH_MAX, agent.getHealth() + healthRegen));
            log.debug("健康 {} -> {}", oldHealth, agent.getHealth());
        }

        if (agent.isDead()) {
            agent.setAlive(false);
            agent.setDeathTicksRemaining(GameConstants.RESPAWN_TICKS);
            String baseNode = GameConstants.getFactionBaseNode(agent.getFaction());
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
            String baseNode = GameConstants.getFactionBaseNode(agent.getFaction());
            agent.setCurrentNode(baseNode);
            logAction(tick, agent.getName(), agent.getFaction(), "RESPAWN",
                    "复活！状态重置为50%，位置: " + baseNode, null, null);
        }
    }
    
    /**
     * 检查并生成秩序之剑（第40回合时在随机位置生成）
     * @param gameState 当前游戏状态
     */
    private void checkOrderSwordSpawn(GameState gameState) {
        // 如果剑还未生成且当前回合>=40，则生成剑
        if (!gameState.getOrderSwordSpawned() && gameState.getCurrentTick() >= 40) {
            // 随机选择中心或野外节点作为生成位置
            List<String> spawnNodes = List.of("center", "forest", "river", "mountain");
            String spawnNode = spawnNodes.get(new Random().nextInt(spawnNodes.size()));
            // 设置剑的位置为随机选择的节点
            gameState.setOrderSwordLocation(spawnNode);
            // 标记剑已生成
            gameState.setOrderSwordSpawned(true);
            log.info("[秩序之剑] 在 {} 生成！", spawnNode);
        }
    }
    
    /**
     * 检查Agent是否拾取了秩序之剑
     * @param agent 待检查的Agent
     * @param gameState 当前游戏状态
     */
    private void checkOrderSwordPickup(Agent agent, GameState gameState) {
        // 只有在剑已生成且无持有者时才能拾取
        if (gameState.getOrderSwordSpawned() && 
            gameState.getOrderSwordHolderId() == null &&
            gameState.getOrderSwordLocation().equals(agent.getCurrentNode())) {
            // Agent拾取了秩序之剑
            gameState.setOrderSwordHolderId(agent.getId());
            log.info("[秩序之剑] {} 拾取了秩序之剑！", agent.getName());
        }
    }

    /**
     * 检查是否触发和平结局
     * 条件：
     * 1. 游戏进行 >= 40回合
     * 2. 所有阵营都有Agent存活
     * 3. 守序阵营持有秩序之剑
     * 4. 守序阵营发布秩序宣言
     * 5. 激进阵营过半数同意
     * @param gameState 当前游戏状态
     */
    private void checkPeaceEnding(GameState gameState) {
        // 条件1: >= 40回合
        if (gameState.getCurrentTick() < 40) return;
        
        // 条件2: 所有阵营存活
        List<Agent> allAgents = agentRepository.findAll();
        boolean lawfulAlive = allAgents.stream().anyMatch(a -> "lawful".equals(a.getFaction()) && a.getAlive());
        boolean aggressiveAlive = allAgents.stream().anyMatch(a -> "aggressive".equals(a.getFaction()) && a.getAlive());
        boolean neutralAlive = allAgents.stream().anyMatch(a -> "neutral".equals(a.getFaction()) && a.getAlive());
        if (!lawfulAlive || !aggressiveAlive || !neutralAlive) return;
        
        // 条件3: 守序持有秩序之剑
        if (gameState.getOrderSwordHolderId() == null) return;
        
        // 条件4: 宣言已发布
        if (!gameState.getOrderDeclarationActive()) return;
        
        // 条件5: 激进过半数同意
        List<Vote> votes = voteRepository.findByDeclarationTick(gameState.getLastDeclarationTick());
        if (votes.isEmpty()) return;
        
        long agreeCount = votes.stream().filter(Vote::getVoteResult).count();
        long totalCount = votes.size();
        double agreeRate = (double) agreeCount / totalCount;
        
        // 需要超过50%同意
        if (agreeRate <= 0.5) return;
        
        // 触发和平结局！
        log.info("========== [和平结局] 守序阵营胜利！激进阵营 {}% 同意 ==========", (int)(agreeRate * 100));
        gameState.setRunning(false);
    }

    /**
     * 发布秩序宣言
     * @param agentId 发布者Agent ID
     */
    @Transactional
    public void publishOrderDeclaration(Long agentId) {
        GameState gs = getOrCreateGameState();
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId));
        
        // 检查冷却
        if (gs.getCurrentTick() - gs.getLastDeclarationTick() < gs.getDeclarationCooldown()) {
            throw new RuntimeException("宣言冷却中，还需 " + 
                (gs.getDeclarationCooldown() - (gs.getCurrentTick() - gs.getLastDeclarationTick())) + " 回合");
        }
        
        // 检查守序阵营+持有剑
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

    /**
     * 投票
     * @param agentId 投票者Agent ID
     * @param agree 是否同意
     */
    @Transactional
    public void castVote(Long agentId, boolean agree) {
        GameState gs = getOrCreateGameState();
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId));
        
        // 检查是否在宣言期间
        if (!gs.getOrderDeclarationActive()) {
            throw new RuntimeException("当前没有激活的宣言");
        }
        
        // 激进阵营才能投票
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

        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(agent, action, target);

        if (result.isSuccess()) {
            // 移动后秩序之剑跟随持有者
            if ("move".equals(action) && gs.getOrderSwordHolderId() != null && 
                gs.getOrderSwordHolderId().equals(agent.getId())) {
                gs.setOrderSwordLocation(target);
                log.info("[秩序之剑] {} 携带剑移动到 {}", agent.getName(), target);
            }
            
            // 检查秩序之剑拾取
            checkOrderSwordPickup(agent, gs);
            
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

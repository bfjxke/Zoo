package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationScheduler {

    private final SandboxStateMachine stateMachine;
    private final AgentRepository agentRepository;
    private final RuleEngine ruleEngine;

    private final Map<Long, Random> agentRandoms = new HashMap<>();

    private Random getAgentRandom(Agent agent) {
        return agentRandoms.computeIfAbsent(agent.getId(), id -> {
            long seed = id * 31 + System.currentTimeMillis() % 1000;
            return new Random(seed);
        });
    }

    @Scheduled(fixedRate = 3000)
    public void scheduledTick() {
        GameState gameState = stateMachine.getOrCreateGameState();
        
        if (!gameState.getRunning()) {
            return;
        }

        int currentTick = gameState.getCurrentTick();
        stateMachine.executeTick();

        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        for (Agent agent : aliveAgents) {
            try {
                makeUniqueDecision(agent, currentTick);
            } catch (Exception e) {
                log.warn("Agent {} 决策失败: {}", agent.getName(), e.getMessage());
            }
        }

        if (currentTick % 3 == 0) {
            autoAirdrop();
        }
    }

    private void makeUniqueDecision(Agent agent, int currentTick) {
        Random rand = getAgentRandom(agent);
        String faction = agent.getFaction();
        String role = agent.getRole();
        int tickOffset = (int) (agent.getId() % 10);

        if (currentTick % 3 == tickOffset % 3) {
            if (agent.getSatiety() < 30) {
                if (agent.getStamina() >= GameConstants.STAMINA_MOVE_COST) {
                    String food = findFoodSource(agent);
                    if (food != null) {
                        ruleEngine.validateAndExecute(agent, "move", food);
                        return;
                    }
                }
                ruleEngine.validateAndExecute(agent, "eat", null);
                return;
            }
        }

        if (agent.getStamina() < 20) {
            ruleEngine.validateAndExecute(agent, "rest", null);
            return;
        }

        if ("leader".equals(role) && rand.nextDouble() < 0.08) {
            executeLeaderSpecialTask(agent, rand);
            return;
        }

        if (rand.nextDouble() < 0.15) {
            executeSecretMission(agent, rand, faction);
            return;
        }

        executeFactionStrategy(agent, rand, faction);
    }

    private void executeLeaderSpecialTask(Agent agent, Random rand) {
        int taskType = rand.nextInt(4);
        switch (taskType) {
            case 0:
                String enemyNode = findEnemyBase(agent.getFaction());
                if (enemyNode != null && rand.nextDouble() < 0.3) {
                    ruleEngine.validateAndExecute(agent, "move", enemyNode);
                    log.info("[秘密任务] {} 突袭敌方基地: {}", agent.getName(), enemyNode);
                    return;
                }
                break;
            case 1:
                ruleEngine.validateAndExecute(agent, "talk", "public");
                log.info("[秘密任务] {} 发布公开演讲", agent.getName());
                return;
            case 2:
                List<String> allNodes = new ArrayList<>(GameConstants.getAllNodeIds());
                allNodes.remove(agent.getCurrentNode());
                String scoutTarget = allNodes.get(rand.nextInt(allNodes.size()));
                ruleEngine.validateAndExecute(agent, "move", scoutTarget);
                log.info("[秘密任务] {} 侦察未知区域: {}", agent.getName(), scoutTarget);
                return;
            case 3:
                ruleEngine.validateAndExecute(agent, "eat", null);
                log.info("[秘密任务] {} 秘密进食", agent.getName());
                return;
        }
        
        executeFactionStrategy(agent, rand, agent.getFaction());
    }

    private void executeSecretMission(Agent agent, Random rand, String faction) {
        int missionType = rand.nextInt(5);
        
        switch (missionType) {
            case 0:
                String stealTarget = findRandomEnemyTerritory(agent, faction);
                if (stealTarget != null) {
                    ruleEngine.validateAndExecute(agent, "move", stealTarget);
                    log.info("[秘密任务] {} 执行偷窃任务，目标是: {}", agent.getName(), stealTarget);
                    return;
                }
                break;
            case 1:
                String hideSpot = findHidingSpot(agent);
                if (hideSpot != null && !hideSpot.equals(agent.getCurrentNode())) {
                    ruleEngine.validateAndExecute(agent, "move", hideSpot);
                    log.info("[秘密任务] {} 前往隐蔽地点: {}", agent.getName(), hideSpot);
                    return;
                }
                break;
            case 2:
                ruleEngine.validateAndExecute(agent, "talk", faction + "_private");
                log.info("[秘密任务] {} 进行秘密通讯", agent.getName());
                return;
            case 3:
                String patrolTarget = findPatrolTarget(agent, faction);
                if (patrolTarget != null) {
                    ruleEngine.validateAndExecute(agent, "move", patrolTarget);
                    log.info("[秘密任务] {} 执行巡逻任务: {}", agent.getName(), patrolTarget);
                    return;
                }
                break;
            case 4:
                ruleEngine.validateAndExecute(agent, "rest", null);
                log.info("[秘密任务] {} 秘密休息恢复", agent.getName());
                return;
        }
        
        executeFactionStrategy(agent, rand, faction);
    }

    private String findRandomEnemyTerritory(Agent agent, String faction) {
        Set<String> enemyNodes = new HashSet<>();
        
        if (!"lawful".equals(faction)) {
            enemyNodes.add("A");
        }
        if (!"neutral".equals(faction)) {
            enemyNodes.add("B");
        }
        if (!"aggressive".equals(faction)) {
            enemyNodes.add("C");
        }
        
        enemyNodes.addAll(GameConstants.getAdjacentNodes(agent.getCurrentNode()));
        
        if (enemyNodes.isEmpty()) {
            return null;
        }
        
        List<String> targets = new ArrayList<>(enemyNodes);
        return targets.get(new Random().nextInt(targets.size()));
    }

    private String findHidingSpot(Agent agent) {
        List<String> candidates = new ArrayList<>();
        for (String nodeId : GameConstants.getAllNodeIds()) {
            if (!nodeId.equals(agent.getCurrentNode())) {
                candidates.add(nodeId);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(new Random().nextInt(candidates.size()));
    }

    private String findPatrolTarget(Agent agent, String faction) {
        List<String> friendlyNodes = new ArrayList<>();
        
        if ("lawful".equals(faction)) {
            friendlyNodes.addAll(Arrays.asList("A", "F"));
        } else if ("aggressive".equals(faction)) {
            friendlyNodes.addAll(Arrays.asList("C", "H"));
        } else {
            friendlyNodes.addAll(Arrays.asList("B", "G"));
        }
        
        friendlyNodes.retainAll(GameConstants.getAdjacentNodes(agent.getCurrentNode()));
        
        if (friendlyNodes.isEmpty()) {
            return null;
        }
        
        return friendlyNodes.get(new Random().nextInt(friendlyNodes.size()));
    }

    private void executeFactionStrategy(Agent agent, Random rand, String faction) {
        int action = rand.nextInt(100);

        if ("lawful".equals(faction)) {
            if (action < 30) {
                moveTowardsObjective(agent, rand, "B", "D");
            } else if (action < 50) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else if (action < 80) {
                ruleEngine.validateAndExecute(agent, "rest", null);
            } else {
                ruleEngine.validateAndExecute(agent, "talk", "lawful_private");
            }
        } else if ("aggressive".equals(faction)) {
            if (action < 50) {
                moveTowardsObjective(agent, rand, "A", "C");
            } else if (action < 70) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else if (action < 85) {
                ruleEngine.validateAndExecute(agent, "rest", null);
            } else {
                ruleEngine.validateAndExecute(agent, "talk", "aggressive_private");
            }
        } else {
            if (action < 40) {
                moveTowardsObjective(agent, rand, "G", "E");
            } else if (action < 60) {
                ruleEngine.validateAndExecute(agent, "eat", null);
            } else if (action < 85) {
                ruleEngine.validateAndExecute(agent, "rest", null);
            } else {
                ruleEngine.validateAndExecute(agent, "talk", "neutral_private");
            }
        }
    }

    private void moveTowardsObjective(Agent agent, Random rand, String... objectives) {
        List<String> adjacent = GameConstants.getAdjacentNodes(agent.getCurrentNode());
        if (adjacent == null || adjacent.isEmpty()) {
            ruleEngine.validateAndExecute(agent, "rest", null);
            return;
        }

        List<String> validTargets = new ArrayList<>(adjacent);
        for (String obj : objectives) {
            if (adjacent.contains(obj)) {
                if (rand.nextDouble() < 0.6) {
                    ruleEngine.validateAndExecute(agent, "move", obj);
                    return;
                }
            }
        }

        String randomTarget = validTargets.get(rand.nextInt(validTargets.size()));
        ruleEngine.validateAndExecute(agent, "move", randomTarget);
    }

    private String findFoodSource(Agent agent) {
        Map<String, Integer> foodScores = new HashMap<>();
        foodScores.put("F", 10);
        foodScores.put("G", 10);
        foodScores.put("H", 10);
        foodScores.put("D", 3);
        foodScores.put("E", 3);
        foodScores.put("B", 5);

        List<String> adjacent = GameConstants.getAdjacentNodes(agent.getCurrentNode());
        if (adjacent == null) return null;

        String bestTarget = null;
        int bestScore = -1;

        for (String node : adjacent) {
            int score = foodScores.getOrDefault(node, 1);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = node;
            }
        }

        return bestTarget;
    }

    private String findEnemyBase(String faction) {
        if ("lawful".equals(faction)) return "C";
        if ("aggressive".equals(faction)) return "A";
        return null;
    }

    private void autoAirdrop() {
        for (String node : Arrays.asList("D", "E")) {
            for (Agent agent : agentRepository.findByAliveTrue()) {
                if (agent.getCurrentNode().equals(node)) {
                    int foodAmount = 8;
                    int oldSatiety = agent.getSatiety();
                    int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, 
                        oldSatiety + foodAmount);
                    agent.setSatiety(newSatiety);
                    agentRepository.save(agent);
                }
            }
        }
    }
}

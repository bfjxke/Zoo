package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.List;
import java.util.Optional;
import com.guardianeye.iiot.model.AgentRepository;

@Service
@Slf4j
public class RuleEngine {

    // AgentRepository用于查询Agent信息，支持秩序之剑持有者查询
    private final AgentRepository agentRepository;
    
    // 构造函数注入AgentRepository
    public RuleEngine(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Data
    @AllArgsConstructor
    public static class ActionResult {
        private boolean success;
        private String message;
        private String judgeId;
        private Double successRate;
    }

    public ActionResult validateAndExecute(Agent agent, String action, String target) {
        log.info("[规则引擎] >>> Agent:{} 尝试执行动作 action:{}, target:{}", 
            agent.getName(), action, target);
        log.info("[规则引擎] 当前状态 - 位置:{}, 耐力:{}, 饱食:{}, 健康:{}", 
            agent.getCurrentNode(), agent.getStamina(), agent.getSatiety(), agent.getHealth());
        
        if (!agent.getAlive()) {
            log.warn("[规则引擎] <<< Agent已死亡，拒绝动作");
            return new ActionResult(false, "Agent已死亡", null, null);
        }

        String actionLower = action.toLowerCase();
        log.info("[规则引擎] 动作小写:{}, 白名单:{}", actionLower, GameConstants.ALLOWED_ACTIONS);

        if (GameConstants.ALLOWED_ACTIONS.contains(actionLower)) {
            ActionResult result = executeAllowedAction(agent, actionLower, target);
            log.info("[规则引擎] <<< 动作执行结果 success:{}, message:{}", result.isSuccess(), result.getMessage());
            return result;
        }

        log.warn("[规则引擎] <<< 动作不在白名单，需AI判官判定");
        return new ActionResult(false, "动作不在白名单中，需提交AI判官判定",
                "JUDGE_PENDING", GameConstants.DEFAULT_SUCCESS_RATE);
    }

    private ActionResult executeAllowedAction(Agent agent, String action, String target) {
        double penalty = computePenalty(agent);

        return switch (action) {
            case "move" -> executeMove(agent, target, penalty);
            case "eat" -> executeEat(agent);
            case "rest" -> executeRest(agent, penalty);
            case "talk" -> executeTalk(agent, target);
            case "trade" -> executeTrade(agent, target, penalty);
            default -> new ActionResult(false, "未知动作: " + action, null, null);
        };
    }

    private double computePenalty(Agent agent) {
        double penalty = 1.0;
        if (agent.isFatigued()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        if (agent.isHungry()) penalty *= GameConstants.PENALTY_MULTIPLIER;
        return penalty;
    }

    /**
     * 计算恢复倍率
     * 当饱食度 > 100时，触发"饱餐Buff"，恢复速度加快50%
     */
    private double computeRecoveryMultiplier(Agent agent) {
        double multiplier = 1.0;
        if (agent.getSatiety() > GameConstants.SATIETY_BUFF_THRESHOLD) {
            multiplier = GameConstants.SATIETY_BUFF_RECOVERY_MULTIPLIER;
        }
        return multiplier;
    }

    private ActionResult executeMove(Agent agent, String target, double penalty) {
        log.info("[规则引擎-MOVE] 开始执行移动 - 目标:{}", target);
        
        if (target == null || !GameConstants.getAllNodeIds().contains(target)) {
            log.warn("[规则引擎-MOVE] <<< 无效目标节点:{}", target);
            return new ActionResult(false, "无效目标节点: " + target, null, null);
        }

        if (target.equals(agent.getCurrentNode())) {
            log.warn("[规则引擎-MOVE] <<< 已在目标节点:{}", target);
            return new ActionResult(false, "已在目标节点: " + target, null, null);
        }

        List<String> adjacent = GameConstants.getAdjacentNodes(agent.getCurrentNode());
        log.info("[规则引擎-MOVE] 相邻节点:{}, penalty:{}", adjacent, penalty);
        
        if (adjacent == null || !adjacent.contains(target)) {
            log.warn("[规则引擎-MOVE] <<< 无法从{}移动到{}，节点不相邻", agent.getCurrentNode(), target);
            return new ActionResult(false,
                    "无法从 " + agent.getCurrentNode() + " 移动到 " + target + "，节点不相邻", null, null);
        }

        int cost = (int)(GameConstants.STAMINA_MOVE_COST * penalty);
        log.info("[规则引擎-MOVE] 消耗计算 - 基础消耗:{}, penalty:{}, 总消耗:{}", 
            GameConstants.STAMINA_MOVE_COST, penalty, cost);
            
        if (agent.getStamina() < cost) {
            log.warn("[规则引擎-MOVE] <<< 耐力不足({})，需要{}", agent.getStamina(), cost);
            return new ActionResult(false, "耐力不足(" + agent.getStamina() + ")，需要 " + cost, null, null);
        }

        String from = agent.getCurrentNode();
        agent.setStamina(agent.getStamina() - cost);
        agent.setCurrentNode(target);
        log.info("[规则引擎-MOVE] <<< 移动成功 {} -> {}，消耗耐力{}，剩余耐力{}", from, target, cost, agent.getStamina());
        return new ActionResult(true,
                from + " -> " + target + "，消耗耐力 " + cost + "，剩余 " + agent.getStamina(), null, null);
    }

    private ActionResult executeEat(Agent agent) {
        log.info("[规则引擎-EAT] 开始执行进食 - 当前饱食:{}", agent.getSatiety());
        
        if (agent.getSatiety() >= GameConstants.SATIETY_MAX_WITH_BUFF) {
            log.warn("[规则引擎-EAT] <<< 饱食度已满({})，无法继续进食", agent.getSatiety());
            return new ActionResult(false,
                    "饱食度已满(" + agent.getSatiety() + ")，无法继续进食", null, null);
        }

        int recover = GameConstants.SATIETY_EAT_RECOVER;
        int oldSatiety = agent.getSatiety();
        int newSatiety = Math.min(GameConstants.SATIETY_MAX_WITH_BUFF, oldSatiety + recover);
        agent.setSatiety(newSatiety);

        String buffMsg = "";
        if (oldSatiety <= GameConstants.SATIETY_BUFF_THRESHOLD && newSatiety > GameConstants.SATIETY_BUFF_THRESHOLD) {
            buffMsg = " 【触发饱餐Buff：耐力恢复加速50%】";
        }

        log.info("[规则引擎-EAT] <<< 进食成功 {} -> {} {}", oldSatiety, newSatiety, buffMsg);
        return new ActionResult(true,
                "进食恢复饱食 +" + recover + "，" + oldSatiety + " -> " + newSatiety + buffMsg, null, null);
    }

    private ActionResult executeRest(Agent agent, double penalty) {
        log.info("[规则引擎-REST] 开始执行休息 - 当前耐力:{}, penalty:{}", agent.getStamina(), penalty);
        
        if (agent.getStamina() >= 100) {
            log.warn("[规则引擎-REST] <<< 耐力已满，无需休息");
            return new ActionResult(false, "耐力已满，无需休息", null, null);
        }

        double recoveryMultiplier = computeRecoveryMultiplier(agent);
        int recover = (int)(GameConstants.STAMINA_REST_RECOVER / (penalty * recoveryMultiplier));
        int oldStamina = agent.getStamina();
        agent.setStamina(Math.min(100, oldStamina + recover));

        String msg = "休息恢复耐力 +" + recover + "，" + oldStamina + " -> " + agent.getStamina();
        if (recoveryMultiplier < 1.0) {
            msg += " 【饱餐Buff生效：恢复效率×" + String.format("%.1f", 1/recoveryMultiplier) + "】";
        }

        log.info("[规则引擎-REST] <<< 休息成功 +{} 耐力 {} -> {}", recover, oldStamina, agent.getStamina());
        return new ActionResult(true, msg, null, null);
    }

    private ActionResult executeTalk(Agent agent, String channel) {
        log.info("[规则引擎-TALK] 开始执行发言 - 频道:{}", channel);
        
        String ch = channel != null ? channel : "public";
        log.info("[规则引擎-TALK] Agent阵营:{}, 当前节点:{}", agent.getFaction(), agent.getCurrentNode());

        if (ch.endsWith("_private")) {
            String faction = ch.replace("_private", "");
            if (!faction.equals(agent.getFaction())) {
                log.warn("[规则引擎-TALK] <<< 无法在{}发言，阵营不匹配", ch);
                return new ActionResult(false, "无法在 " + ch + " 发言，阵营不匹配", null, null);
            }
            String baseNode = GameConstants.FACTION_BASE.get(faction);
            if (!agent.getCurrentNode().equals(baseNode)) {
                log.warn("[规则引擎-TALK] <<< 阵营私聊需在基地节点({})，当前在{}", baseNode, agent.getCurrentNode());
                return new ActionResult(false,
                        "阵营私聊需在基地节点(" + baseNode + ")，当前在 " + agent.getCurrentNode(), null, null);
            }
        }

        if (!GameConstants.FACTION_CHANNELS.contains(ch) && !ch.equals("public")) {
            log.warn("[规则引擎-TALK] <<< 无效频道:{}", ch);
            return new ActionResult(false, "无效频道: " + ch, null, null);
        }

        log.info("[规则引擎-TALK] <<< 在[{}]频道发言成功", ch);
        return new ActionResult(true, "在 [" + ch + "] 频道发言", null, null);
    }

    private ActionResult executeTrade(Agent agent, String target, double penalty) {
        int cost = (int)(GameConstants.CUSTOM_ACTION_COST * penalty);
        if (agent.getStamina() < cost) {
            return new ActionResult(false, "耐力不足(" + agent.getStamina() + ")，需要 " + cost, null, null);
        }

        agent.setStamina(agent.getStamina() - cost);
        return new ActionResult(true,
                "与 " + (target != null ? target : "未知") + " 交易，消耗耐力 " + cost, null, null);
    }
    
    /**
     * 检查Agent是否持有秩序之剑
     * @param agent 待检查的Agent
     * @param gameState 当前游戏状态
     * @return true表示持有秩序之剑，false表示未持有
     */
    public boolean hasOrderSword(Agent agent, GameState gameState) {
        // 如果游戏状态中没有持有者ID，返回false
        if (gameState.getOrderSwordHolderId() == null) {
            return false;
        }
        // 比较Agent的ID与持有者ID是否匹配
        return gameState.getOrderSwordHolderId().equals(agent.getId());
    }
    
    /**
     * 获取秩序之剑持有者Agent
     * @param gameState 当前游戏状态
     * @return 持有者Agent，如果无持有者则返回null
     */
    private Agent getOrderSwordHolder(GameState gameState) {
        // 如果没有持有者ID，返回null
        if (gameState.getOrderSwordHolderId() == null) {
            return null;
        }
        // 通过Repository查询持有者Agent
        Optional<Agent> holder = agentRepository.findById(gameState.getOrderSwordHolderId());
        return holder.orElse(null);
    }
}

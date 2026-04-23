package com.guardianeye.iiot;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.service.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SandboxSimulationTest {

    private RuleEngine ruleEngine;
    private Agent testAgent;

    @BeforeEach
    void setUp() {
        // 创建模拟的AgentRepository用于测试
        AgentRepository mockAgentRepository = Mockito.mock(AgentRepository.class);
        ruleEngine = new RuleEngine(mockAgentRepository);
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setName("测试Agent");
        testAgent.setFaction("lawful");
        testAgent.setRole("soldier");
        testAgent.setStamina(GameConstants.STAMINA_INITIAL);
        testAgent.setSatiety(GameConstants.SATIETY_INITIAL);
        testAgent.setHealth(GameConstants.HEALTH_INITIAL);
        testAgent.setCurrentNode("base_lawful");
        testAgent.setAlive(true);
        testAgent.setFatigued(false);
        testAgent.setHungry(false);
    }

    @Test
    @DisplayName("满状态Agent: 移动消耗15耐力")
    void testMoveFromFullState() {
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "center");
        assertTrue(result.isSuccess());
        assertEquals(85, testAgent.getStamina());
        assertEquals("center", testAgent.getCurrentNode());
    }

    @Test
    @DisplayName("疲劳Agent: 移动消耗15*1.5=22耐力")
    void testMoveWhileFatigued() {
        testAgent.setStamina(19);
        testAgent.setFatigued(true);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "center");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("耐力不足"));
    }

    @Test
    @DisplayName("疲劳+饥饿Agent: 移动消耗15*1.5*1.5=33耐力，耐力不足则失败")
    void testMoveWhileFatiguedAndHungry() {
        testAgent.setStamina(19);
        testAgent.setSatiety(10);
        testAgent.setFatigued(true);
        testAgent.setHungry(true);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "center");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("耐力不足"));
    }

    @Test
    @DisplayName("耐力不足: 移动失败")
    void testMoveInsufficientStamina() {
        testAgent.setStamina(5);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "center");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("耐力不足"));
    }

    @Test
    @DisplayName("节点不相邻: 移动失败")
    void testMoveNonAdjacentNode() {
        testAgent.setCurrentNode("base_lawful");
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "base_aggressive");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("不相邻"));
    }

    @Test
    @DisplayName("进食: 恢复30饱食")
    void testEatRecovery() {
        testAgent.setSatiety(50);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "eat", null);
        assertTrue(result.isSuccess());
        assertEquals(80, testAgent.getSatiety());
    }

    @Test
    @DisplayName("饱食已满(100): 进食仍然成功(可超量进食)")
    void testEatWhenFull() {
        testAgent.setSatiety(100);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "eat", null);
        assertTrue(result.isSuccess());
        assertEquals(130, testAgent.getSatiety());
    }

    @Test
    @DisplayName("饱食超量(140): 进食失败")
    void testEatWhenMaxed() {
        testAgent.setSatiety(140);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "eat", null);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("已满"));
    }

    @Test
    @DisplayName("休息: 恢复20耐力")
    void testRestRecovery() {
        testAgent.setStamina(50);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "rest", null);
        assertTrue(result.isSuccess());
        assertEquals(70, testAgent.getStamina());
    }

    @Test
    @DisplayName("疲劳状态休息: 恢复20/1.5=13耐力")
    void testRestWhileFatigued() {
        testAgent.setStamina(15);
        testAgent.setFatigued(true);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "rest", null);
        assertTrue(result.isSuccess());
        int expectedRecover = (int)(GameConstants.STAMINA_REST_RECOVER / GameConstants.PENALTY_MULTIPLIER);
        assertEquals(15 + expectedRecover, testAgent.getStamina());
    }

    @Test
    @DisplayName("阵营私聊: 在基地节点可以发言")
    void testFactionPrivateChatAtBase() {
        testAgent.setCurrentNode("base_lawful");
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "talk", "lawful_private");
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("阵营私聊: 不在基地节点不能发言")
    void testFactionPrivateChatNotAtBase() {
        testAgent.setCurrentNode("center");
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "talk", "lawful_private");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("基地"));
    }

    @Test
    @DisplayName("阵营私聊: 非本阵营不能发言")
    void testFactionPrivateChatWrongFaction() {
        testAgent.setCurrentNode("base_aggressive");
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "talk", "aggressive_private");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("阵营不匹配"));
    }

    @Test
    @DisplayName("全频道: 任何位置都可以发言")
    void testPublicChatAnywhere() {
        testAgent.setCurrentNode("center");
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "talk", "public");
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("非白名单动作: 提交AI判官")
    void testNonWhitelistedAction() {
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "attack", "enemy");
        assertFalse(result.isSuccess());
        assertEquals("JUDGE_PENDING", result.getJudgeId());
        assertEquals(GameConstants.DEFAULT_SUCCESS_RATE, result.getSuccessRate());
    }

    @Test
    @DisplayName("死亡Agent: 无法执行动作")
    void testDeadAgentCannotAct() {
        testAgent.setAlive(false);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "center");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("死亡"));
    }

    @Test
    @DisplayName("被动消耗: 满状态1个Tick消耗10耐力5饱食")
    void testPassiveConsumptionNormal() {
        int staminaBefore = testAgent.getStamina();
        int satietyBefore = testAgent.getSatiety();

        testAgent.applyFatigueMultiplier();
        double staminaCost = GameConstants.STAMINA_BASE_COST;
        double satietyCost = GameConstants.SATIETY_BASE_COST;
        if (testAgent.isFatigued()) staminaCost *= GameConstants.PENALTY_MULTIPLIER;
        if (testAgent.isHungry()) satietyCost *= GameConstants.PENALTY_MULTIPLIER;

        testAgent.setStamina(Math.max(0, (int)(testAgent.getStamina() - staminaCost)));
        testAgent.setSatiety(Math.max(0, (int)(testAgent.getSatiety() - satietyCost)));

        assertEquals(staminaBefore - GameConstants.STAMINA_BASE_COST, testAgent.getStamina());
        assertEquals(satietyBefore - GameConstants.SATIETY_BASE_COST, testAgent.getSatiety());
    }

    @Test
    @DisplayName("v1.1饥饿扣血: 饱食<30时每Tick扣20健康")
    void testHungerDamage() {
        testAgent.setSatiety(25);
        testAgent.setHealth(50);
        testAgent.applyFatigueMultiplier();
        assertTrue(testAgent.isHungry());
        if (testAgent.isHungry()) {
            testAgent.setHealth(Math.max(0, testAgent.getHealth() - GameConstants.HEALTH_HUNGER_DAMAGE));
        }
        assertEquals(30, testAgent.getHealth());
    }

    @Test
    @DisplayName("v1.1死亡流程: 健康归零进入复活倒计时，复活后属性50%")
    void testDeathAndRespawn() {
        testAgent.setHealth(0);
        assertTrue(testAgent.isDead());
        testAgent.setAlive(false);
        testAgent.setDeathTicksRemaining(GameConstants.RESPAWN_TICKS);
        assertFalse(testAgent.getAlive());
        assertEquals(GameConstants.RESPAWN_TICKS, testAgent.getDeathTicksRemaining());

        for (int i = GameConstants.RESPAWN_TICKS; i > 0; i--) {
            testAgent.setDeathTicksRemaining(testAgent.getDeathTicksRemaining() - 1);
        }
        assertEquals(0, testAgent.getDeathTicksRemaining());

        testAgent.setAlive(true);
        testAgent.setStamina(GameConstants.STAMINA_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
        testAgent.setSatiety(GameConstants.SATIETY_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
        testAgent.setHealth(GameConstants.HEALTH_INITIAL * GameConstants.RESPAWN_STAT_PERCENT / 100);
        assertTrue(testAgent.getAlive());
        assertEquals(50, testAgent.getStamina());
        assertEquals(50, testAgent.getSatiety());
        assertEquals(45, testAgent.getHealth()); // v1.1: 90*50%=45
    }

    @Test
    @DisplayName("疲劳阈值: 耐力<20进入疲劳状态")
    void testFatigueThreshold() {
        testAgent.setStamina(19);
        assertTrue(testAgent.isFatigued());
        testAgent.setStamina(20);
        assertFalse(testAgent.isFatigued());
    }

    @Test
    @DisplayName("饥饿阈值: 饱食<30进入饥饿状态")
    void testHungerThreshold() {
        testAgent.setSatiety(29);
        assertTrue(testAgent.isHungry());
        testAgent.setSatiety(30);
        assertFalse(testAgent.isHungry());
    }

    @Test
    @DisplayName("交易: 消耗30耐力")
    void testTrade() {
        testAgent.setStamina(50);
        RuleEngine.ActionResult result = ruleEngine.validateAndExecute(testAgent, "trade", "other_agent");
        assertTrue(result.isSuccess());
        assertEquals(20, testAgent.getStamina());
    }
}

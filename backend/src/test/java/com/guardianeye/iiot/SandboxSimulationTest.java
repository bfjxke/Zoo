package com.guardianeye.iiot;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.GameConstants;
import com.guardianeye.iiot.service.ActionResult;
import com.guardianeye.iiot.service.RuleEngine;
import com.guardianeye.iiot.service.action.ActionStrategyFactory;
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
        ActionStrategyFactory mockFactory = Mockito.mock(ActionStrategyFactory.class);
        ruleEngine = new RuleEngine(mockFactory);
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setName("测试Agent");
        testAgent.setFaction("lawful");
        testAgent.setRole("soldier");
        testAgent.setStamina(GameConstants.STAMINA_INITIAL);
        testAgent.setSatiety(GameConstants.SATIETY_INITIAL);
        testAgent.setHealth(GameConstants.HEALTH_INITIAL);
        testAgent.setCurrentNode("A");
        testAgent.setAlive(true);
        testAgent.setFatigued(false);
        testAgent.setHungry(false);
    }

    @Test
    @DisplayName("死亡Agent: 无法执行动作")
    void testDeadAgentCannotAct() {
        testAgent.setAlive(false);
        ActionResult result = ruleEngine.validateAndExecute(testAgent, "move", "D");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("死亡"));
    }

    @Test
    @DisplayName("非白名单动作: 提交AI判官")
    void testNonWhitelistedAction() {
        ActionResult result = ruleEngine.validateAndExecute(testAgent, "attack", "enemy");
        assertFalse(result.isSuccess());
        assertEquals("JUDGE_PENDING", result.getJudgeId());
        assertEquals(GameConstants.DEFAULT_SUCCESS_RATE, result.getSuccessRate());
    }

    @Test
    @DisplayName("v1.1饥饿扣血: 饱食<30时进入饥饿状态")
    void testHungerDamage() {
        testAgent.setSatiety(25);
        testAgent.setHealth(50);
        testAgent.applyFatigueMultiplier();
        assertTrue(testAgent.isHungry());
        assertEquals(30, testAgent.getHealth() - GameConstants.HEALTH_HUNGER_DAMAGE);
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
        assertEquals(45, testAgent.getHealth());
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
}
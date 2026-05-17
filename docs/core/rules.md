# GuardianEye-IIoT 沙箱动物园 - 游戏规则白皮书

> 本文档是游戏的"规则宪法"，所有游戏逻辑必须严格遵循此文档。
>
> **版本历史**：
> - v1.0: 初始版本
> - v1.1: 生命值调整，饥饿惩罚重做，和平结局机制
> - v1.2: Phase 4资源博弈与AI判官系统
> - **v2.1: 当前版本** - 设计模式重构完成

---

## 一、时间系统

### 1.1 回合（Tick）

| 参数 | 数值 |
|------|------|
| Tick间隔 | 30秒 |
| 最大回合数 | 50回合 |
| 秩序之剑生成 | 第40回合 |

### 1.2 Tick结算流程（责任链模式）

```
SandboxStateMachine.executeTick()
         │
         ▼
┌─────────────────────────────────────┐
│  阶段1: PassiveConsumptionPhase (order=1)
│  - 禁闭检查：禁闭时长-1              │
│  - 被动消耗：每3轮扣5饱食度           │
│  - 饥饿扣血：饱食<30时每回合扣20健康   │
│  - 健康回复：饱食>100时+10，>30时+5   │
│  - 死亡处理：食物掉落、位置重置        │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段2: OrderSwordSpawnPhase (order=2)
│  - 第40回合在随机节点生成秩序之剑    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段3: RespawnPhase (order=3)
│  - 复活倒计时：deathTicksRemaining-1 │
│  - 复活处理：属性恢复50%，回到基地    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段4: PeaceEndingPhase (order=4)
│  - 和平结局条件检查                  │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段5: AirdropPhase (order=5)
│  - 每11轮在D、E节点各投放30份食物    │
└─────────────────────────────────────┘
         │
         ▼
     游戏状态保存
```

---

## 二、GameConstants 配置详解

`GameConstants` 是整个游戏的"规则宪法"，定义所有核心数值常量。

### 2.1 时间系统常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `TICK_INTERVAL_SECONDS` | 30 | Tick间隔（秒） |
| `MAX_GAME_TICKS` | 50 | 最大回合数 |
| `ORDER_SWORD_SPAWN_TICK` | 40 | 秩序之剑生成回合 |

### 2.2 Agent基础属性常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `STAMINA_INITIAL` | 100 | 初始耐力 |
| `SATIETY_INITIAL` | 100 | 初始饱食度 |
| `HEALTH_INITIAL` | 90 | 初始健康 |
| `HEALTH_MAX` | 90 | 健康上限 |
| `SATIETY_MAX_WITH_BUFF` | 140 | 饱食度上限（含Buff） |

### 2.3 消耗与恢复常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `STAMINA_BASE_COST` | 10 | 每回合耐力消耗 |
| `STAMINA_MOVE_COST` | 15 | 移动耐力消耗 |
| `STAMINA_REST_RECOVER` | 20 | 休息恢复耐力基础值 |
| `SATIETY_BASE_COST` | 5 | 每回合饱食度消耗 |
| `SATIETY_EAT_RECOVER` | 30 | 进食恢复饱食度 |
| `HEALTH_HUNGER_DAMAGE` | 20 | 饥饿扣血 |

### 2.4 状态阈值常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `FATIGUE_THRESHOLD` | 20 | 疲劳阈值（耐力<20进入疲劳） |
| `HUNGER_THRESHOLD` | 30 | 饥饿阈值（饱食<30进入饥饿） |
| `PENALTY_MULTIPLIER` | 1.5 | 状态惩罚倍率 |
| `SATIETY_BUFF_THRESHOLD` | 100 | 饱餐Buff触发阈值 |
| `SATIETY_BUFF_RECOVERY_MULTIPLIER` | 0.588 | 饱餐Buff恢复倍率（约1.7倍） |

### 2.5 资源系统常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `MAX_CARRIED_FOOD` | 20 | 最大携带食物数量 |
| `STEAL_AMOUNT` | 3 | 每次偷窃上限份数 |
| `STEAL_CATCH_CONFINEMENT` | 3 | 偷窃被抓禁闭轮数 |

### 2.6 复活系统常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `RESPAWN_TICKS` | 20 | 复活所需回合数 |
| `RESPAWN_STAT_PERCENT` | 50 | 复活后属性保留百分比 |

### 2.7 和平结局常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `PEACE_ENDING_MIN_TICKS` | 40 | 和平结局最小回合数 |
| `ORDER_FACTION_BUFF` | 0.1 | 守序阵营全员Buff（10%） |
| `ORDER_DECLARATION_COOLDOWN` | 10 | 秩序宣言冷却回合数 |

### 2.8 健康回复常量

| 常量名 | 数值 | 说明 |
|--------|------|------|
| `HEALTH_REGEN_SATIETY_THRESHOLD` | 30 | 健康回复饱食度阈值 |
| `HEALTH_REGEN_NORMAL` | 5 | 普通健康回复量 |
| `HEALTH_REGEN_BUFF` | 10 | 饱餐Buff健康回复量 |

### 2.9 地图节点常量

```java
// 所有节点
ALL_NODES = Set.of("A", "B", "C", "D", "E", "F", "G", "H")

// 节点相邻关系
ADJACENT_NODES = Map.of(
    "A", Set.of("D", "F"),      // 守序基地
    "B", Set.of("D", "E", "G"),  // 中立基地
    "C", Set.of("E", "H"),       // 激进基地
    "D", Set.of("A", "B", "E", "F", "G"),  // 守序广场
    "E", Set.of("B", "C", "D", "G", "H"),  // 激进广场
    "F", Set.of("A", "D"),       // 森林
    "G", Set.of("B", "D", "E", "H"),  // 河流
    "H", Set.of("C", "E", "G")   // 山地
)

// 阵营基地
FACTION_BASE = Map.of(
    "lawful", "A",
    "aggressive", "C",
    "neutral", "B"
)
```

### 2.10 白名单动作

```java
ALLOWED_ACTIONS = Set.of(
    "move",      // 移动
    "eat",       // 进食
    "rest",      // 休息
    "talk",      // 发言
    "trade",     // 交易
    "provoke",   // 挑衅
    "claim_food", // 领取食物
    "pickup_food", // 捡起食物
    "steal"      // 偷窃
)
```

---

## 三、Agent属性系统

### 3.1 五项基础属性

| 属性 | 含义 | 初始值 | 上限 |
|------|------|--------|------|
| 耐力(Stamina) | Agent的体力，决定能执行多少动作 | 100 | 100 |
| 饱食度(Satiety) | Agent的饱饿程度，耗尽会扣健康 | 100 | 140（含Buff） |
| 健康(Health) | Agent的生命值，归零则死亡 | 90 | 90 |
| 携带食物(CarriedFood) | Agent随身携带的食物数量 | 0 | 20 |
| 禁闭轮数(ConfinementTicks) | 禁闭剩余回合数 | 0 | - |

### 3.2 状态判定规则

| 状态 | 触发条件 | 效果 |
|------|----------|------|
| 疲劳(Fatigued) | 耐力 < 20 | 耐力消耗×1.5，恢复效果降低 |
| 饥饿(Hungry) | 饱食度 < 30 | 耐力消耗×1.5，每回合扣20健康 |
| 饱食回血 | 饱食度 > 30 | 每回合回复5健康 |
| 饱餐Buff | 饱食度 > 100 | 耐力恢复×1.7，每回合回复10健康 |
| 禁闭(Confined) | confinementTicks > 0 | 不能执行任何动作，属性固定 |
| 死亡(Dead) | 健康 ≤ 0 | 无法行动，等待复活 |

---

## 四、动作系统（策略模式）

### 4.1 策略接口

```java
public interface ActionStrategy {
    ActionResult execute(Agent agent, String target);
    String getActionName();
    default boolean canExecute(Agent agent) {
        return agent.getAlive();
    }
}
```

### 4.2 具体动作策略

| 策略类 | 动作名 | 核心逻辑 |
|--------|--------|----------|
| `MoveActionStrategy` | move | 检查相邻节点、耐力消耗、移动位置 |
| `EatActionStrategy` | eat | 先吃携带食物、再吃营地库存 |
| `RestActionStrategy` | rest | 休息恢复耐力，受状态惩罚影响 |
| `TalkActionStrategy` | talk | 阵营私聊需在基地 |
| `TradeActionStrategy` | trade | 消耗30耐力 |
| `ProvokeActionStrategy` | provoke | 无实际效果 |
| `ClaimFoodActionStrategy` | claim_food | 在基地领食物从库存扣 |
| `PickupFoodActionStrategy` | pickup_food | 在场景捡食物 |
| `StealActionStrategy` | steal | 对方营地偷食物，被抓禁闭 |

### 4.3 策略工厂

```java
@Component
public class ActionStrategyFactory {
    private final Map<String, ActionStrategy> strategies;
    
    // Spring自动注入所有ActionStrategy实现
    public ActionStrategyFactory(List<ActionStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                s -> s.getActionName().toLowerCase(),
                Function.identity()
            ));
    }
    
    public ActionStrategy getStrategy(String actionName) {
        return strategies.get(actionName.toLowerCase());
    }
}
```

### 4.4 动作执行流程

```
RuleEngine.validateAndExecute()
         │
         ▼
┌─────────────────────────────────────┐
│  1. 死亡检查：Agent.alive == false?  │
│     是 → 返回失败                    │
│     否 → 继续                        │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  2. 白名单检查：action in ALLOWED_ACTIONS? │
│     否 → 返回JUDGE_PENDING            │
│     是 → 继续                        │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  3. 策略执行                         │
│     ActionStrategyFactory.getStrategy(action) │
│     strategy.execute(agent, target) │
└─────────────────────────────────────┘
         │
         ▼
     返回 ActionResult
```

---

## 五、资源系统

### 5.1 空投物资

| 参数 | 数值 |
|------|------|
| 投放周期 | 每11轮 |
| 投放地点 | D节点、E节点 |
| 每次投放量 | 各30份 |

### 5.2 食物携带

| 参数 | 数值 |
|------|------|
| 携带上限 | 20份 |
| 获取方式 | 在营地领取 / 在场景捡起 |
| 死亡掉落 | 死亡时食物掉落在原地 |

---

## 六、死亡与复活系统

### 6.1 死亡触发

当健康值 ≤ 0时，Agent进入死亡状态

### 6.2 复活机制

| 参数 | 数值 |
|------|------|
| 复活所需回合 | 20 Tick |
| 属性保留 | 50% |
| 复活后健康 | 90 × 50% = 45点 |

---

## 七、秩序之剑系统

### 7.1 生成规则

- **生成时间**：第40回合
- **生成位置**：随机节点（D、E、F、G、H之一）
- **唯一性**：整局游戏只有一把

### 7.2 持有效果

| 阵营 | 持有效果 |
|------|----------|
| 守序阵营 | 全员10%属性加成 + 可发布秩序宣言 |
| 其他阵营 | 无特殊效果 |

---

## 八、完整常量代码

```java
package com.guardianeye.iiot.model;

public final class GameConstants {
    
    private GameConstants() {}
    
    // ========== 时间系统 ==========
    public static final int TICK_INTERVAL_SECONDS = 30;
    public static final int MAX_GAME_TICKS = 50;
    public static final int ORDER_SWORD_SPAWN_TICK = 40;
    
    // ========== Agent基础属性 ==========
    public static final int STAMINA_INITIAL = 100;
    public static final int SATIETY_INITIAL = 100;
    public static final int HEALTH_INITIAL = 90;
    public static final int HEALTH_MAX = 90;
    public static final int SATIETY_MAX_WITH_BUFF = 140;
    
    // ========== 动作消耗 ==========
    public static final int STAMINA_BASE_COST = 10;
    public static final int STAMINA_MOVE_COST = 15;
    public static final int STAMINA_REST_RECOVER = 20;
    public static final int SATIETY_BASE_COST = 5;
    public static final int SATIETY_EAT_RECOVER = 30;
    
    // ========== 生命值系统 ==========
    public static final int HEALTH_HUNGER_DAMAGE = 20;
    public static final int HEALTH_REGEN_SATIETY_THRESHOLD = 30;
    public static final int HEALTH_REGEN_NORMAL = 5;
    public static final int HEALTH_REGEN_BUFF = 10;
    
    // ========== 状态阈值 ==========
    public static final int FATIGUE_THRESHOLD = 20;
    public static final int HUNGER_THRESHOLD = 30;
    public static final double PENALTY_MULTIPLIER = 1.5;
    public static final double SATIETY_BUFF_RECOVERY_MULTIPLIER = 0.588;
    public static final int SATIETY_BUFF_THRESHOLD = 100;
    
    // ========== 资源系统 ==========
    public static final int MAX_CARRIED_FOOD = 20;
    public static final int STEAL_AMOUNT = 3;
    public static final int STEAL_CATCH_CONFINEMENT = 3;
    
    // ========== 复活系统 ==========
    public static final int RESPAWN_TICKS = 20;
    public static final int RESPAWN_STAT_PERCENT = 50;
    
    // ========== 和平结局 ==========
    public static final int PEACE_ENDING_MIN_TICKS = 40;
    public static final double ORDER_FACTION_BUFF = 0.1;
    public static final int ORDER_DECLARATION_COOLDOWN = 10;
}
```

---

## 九、版本历史

*文档版本：2.1*
*最后更新：2026-05-17*
*更新内容：*
* - GameConstants 详细配置文档*
* - Tick结算流程（责任链模式）更新*
* - 动作系统（策略模式）详细说明*
* - Strategy接口和Factory文档化*
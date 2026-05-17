# GuardianEye-IIoT 沙箱动物园 - 架构设计文档

> 本文档描述系统的架构设计、模块关系和设计模式应用。
>
> **版本历史**：
> - v1.0: 初始架构
> - v1.2: Phase 4 资源博弈系统
> - v2.0: Phase 5 架构升级（策略模式+责任链模式）
> - **v2.1: 当前版本** - 设计模式完整实现

---

## 一、系统架构概览

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           前端 (Vue 3)                                   │
│   Dashboard.vue │ GameMap.vue │ AgentStatus.vue │ LogStream.vue          │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ HTTP / WebSocket
┌─────────────────────────────────┴───────────────────────────────────────┐
│                        Backend (Spring Boot)                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      Controller Layer                            │    │
│  │  SandboxController │ GodController │ 其他REST接口               │    │
│  └─────────────────────────────────┬───────────────────────────────┘    │
│                                    │                                    │
│  ┌─────────────────────────────────┴───────────────────────────────┐    │
│  │                      Service Layer                               │    │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐       │    │
│  │  │ SandboxStateMachine │  RuleEngine   │  GodModeService│       │    │
│  │  │  (责任链模式)  │  │  (策略模式)   │  │               │       │    │
│  │  └────────────────┘  └────────────────┘  └────────────────┘       │    │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐       │    │
│  │  │SimulationScheduler │ PersonalityService │ WebSocketPushService │    │
│  │  └────────────────┘  └────────────────┘  └────────────────┘       │    │
│  └─────────────────────────────────┬───────────────────────────────┘    │
│                                    │                                    │
│  ┌─────────────────────────────────┴───────────────────────────────┐    │
│  │                      Model Layer                                   │    │
│  │  Agent │ GameState │ ActionLog │ Vote │ GameConstants            │    │
│  └───────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ REST API
┌─────────────────────────────────┴───────────────────────────────────────┐
│                      Agent服务 (Python FastAPI)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                    │
│  │ /decide     │  │ /judge      │  │ /health      │                    │
│  └──────┬───────┘  └──────────────┘  └──────────────┘                    │
│         │                                                              │
│  ┌──────┴──────────────────────────────────────────────────────┐      │
│  │                 AI决策层 (MiniMax API)                       │      │
│  │  AgentScheduler │ MiniMaxClient │ RateLimiter               │      │
│  └───────────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        数据层 (MySQL)                                    │
│  agents │ game_states │ action_logs │ votes                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、设计模式应用

### 2.1 策略模式 (Strategy Pattern) - RuleEngine

**目标**：解耦动作逻辑，实现动作的可插拔

**应用前问题**：
- RuleEngine 包含 10+ 种动作的执行逻辑
- 添加新动作需要修改核心类
- 难以单独测试每个动作

**应用后架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                      ActionStrategy                          │
│                   (策略接口)                                  │
│  + execute(Agent, String): ActionResult                      │
│  + getActionName(): String                                  │
│  + canExecute(Agent): boolean                               │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ MoveAction    │  │  EatAction   │  │  RestAction  │
│ Strategy      │  │  Strategy    │  │  Strategy    │
└───────────────┘  └───────────────┘  └───────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ TalkAction    │  │  TradeAction  │  │ ProvokeAction│
│ Strategy      │  │  Strategy    │  │  Strategy    │
└───────────────┘  └───────────────┘  └───────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ClaimFoodAction│  │PickupFoodAction│  │ StealAction  │
│ Strategy      │  │  Strategy    │  │  Strategy    │
└───────────────┘  └───────────────┘  └───────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   ActionStrategyFactory                      │
│  (工厂类 - 自动收集所有 @Component 策略)                    │
│  + getStrategy(actionName): ActionStrategy                   │
│  + execute(actionName, Agent, target): ActionResult        │
└─────────────────────────────────────────────────────────────┘
```

**核心代码**：

```java
// 1. 策略接口
public interface ActionStrategy {
    ActionResult execute(Agent agent, String target);
    String getActionName();
}

// 2. 具体策略实现
@Component
@Slf4j
public class MoveActionStrategy implements ActionStrategy {
    @Override
    public ActionResult execute(Agent agent, String target) {
        // 移动逻辑...
        return ActionResult.success("移动成功");
    }
    
    @Override
    public String getActionName() {
        return "move";
    }
}

// 3. 工厂自动收集
@Component
@Slf4j
public class ActionStrategyFactory {
    private final Map<String, ActionStrategy> strategies;
    
    public ActionStrategyFactory(List<ActionStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                s -> s.getActionName().toLowerCase(),
                Function.identity()
            ));
    }
}

// 4. RuleEngine 极简化
@Service
@Slf4j
public class RuleEngine {
    private final ActionStrategyFactory actionStrategyFactory;
    
    public ActionResult validateAndExecute(Agent agent, String action, String target) {
        // 校验死亡、白名单等...
        ActionStrategy strategy = actionStrategyFactory.getStrategy(action);
        return strategy.execute(agent, target);
    }
}
```

**优势**：
- ✅ 每个动作独立，易于测试
- ✅ 添加新动作只需新增策略类
- ✅ 工厂自动注册，无需修改现有代码
- ✅ RuleEngine 代码量从 388 行减少到 60 行

---

### 2.2 责任链模式 (Chain of Responsibility) - SandboxStateMachine

**目标**：解耦 Tick 结算流程，实现阶段的可插拔

**应用前问题**：
- executeTick() 包含 6 个阶段
- 难以单独测试某个阶段
- 添加新阶段需要修改核心类

**应用后架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                        TickPhase                            │
│                   (阶段接口)                                  │
│  + execute(GameState, tick): void                          │
│  + getOrder(): int                                          │
│  + getName(): String                                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│PassiveConsump│  │ OrderSwordSpawn│  │   Respawn     │
│tionPhase     │  │ Phase         │  │   Phase       │
│ order = 1     │  │ order = 2     │  │ order = 3     │
└───────────────┘  └───────────────┘  └───────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ PeaceEnding   │  │   Airdrop     │  │   (未来扩展)  │
│ Phase         │  │   Phase       │  │               │
│ order = 4     │  │ order = 5     │  │               │
└───────────────┘  └───────────────┘  └───────────────┘

┌─────────────────────────────────────────────────────────────┐
│                SandboxStateMachine                          │
│  (状态机 - 自动收集并执行所有 Phase)                          │
│  + executeTick(): void                                      │
│    → 按 order 排序执行所有 TickPhase                         │
└─────────────────────────────────────────────────────────────┘
```

**核心代码**：

```java
// 1. 阶段接口
public interface TickPhase {
    void execute(GameState gameState, int tick);
    int getOrder();
    String getName();
}

// 2. 具体阶段实现
@Component
@Slf4j
public class PassiveConsumptionPhase implements TickPhase {
    @Override
    @Transactional
    public void execute(GameState gameState, int tick) {
        List<Agent> aliveAgents = agentRepository.findByAliveTrue();
        for (Agent agent : aliveAgents) {
            applyPassiveConsumption(agent, tick);
        }
        agentRepository.saveAll(aliveAgents);
    }
    
    @Override
    public int getOrder() { return 1; }
    @Override
    public String getName() { return "passiveConsumption"; }
}

// 3. SandboxStateMachine 自动执行
@Service
@RequiredArgsConstructor
@Slf4j
public class SandboxStateMachine {
    private final List<TickPhase> tickPhases;  // Spring 自动注入
    
    @Transactional
    public void executeTick() {
        GameState gameState = getOrCreateGameState();
        int tick = gameState.getCurrentTick() + 1;
        
        tickPhases.stream()
            .sorted(Comparator.comparingInt(TickPhase::getOrder))
            .forEach(phase -> phase.execute(gameState, tick));
        
        gameStateRepository.save(gameState);
    }
}
```

**优势**：
- ✅ 每个阶段独立，易于测试
- ✅ 添加新阶段只需新增 Phase 类
- ✅ 可以动态调整执行顺序
- ✅ 事务边界更清晰（每个阶段可独立 @Transactional）

---

### 2.3 服务层分离 (GodModeService)

**目标**：将上帝命令的业务逻辑从 Controller 分离

**应用前问题**：
- GodController 包含业务逻辑
- plague 接口空实现
- 难以测试

**应用后架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                    GodController                            │
│  (只负责 HTTP 请求处理)                                      │
│  + airdrop() → 调用 GodModeService                          │
│  + plague() → 调用 GodModeService                           │
│  + amnesty() → 调用 GodModeService                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    GodModeService                           │
│  (业务逻辑层)                                               │
│  + airdropSupplies(node, amount): int                     │
│  + applyPlague(faction, penalty): int                      │
│  + grantAmnesty(agentName): int                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、模块详细说明

### 3.1 后端模块结构

```
backend/src/main/java/com/guardianeye/iiot/
├── controller/                      # 控制层
│   ├── SandboxController.java      # 沙盒主控制器
│   └── GodController.java          # 上帝控制器
│
├── service/                        # 服务层
│   ├── SandboxStateMachine.java    # 游戏状态机 (责任链)
│   ├── RuleEngine.java             # 规则引擎 (策略)
│   ├── GodModeService.java         # 上帝模式服务
│   ├── PersonalityService.java    # 性格分配服务
│   ├── SimulationScheduler.java   # 模拟调度器
│   └── ActionResult.java           # 动作结果 (统一返回)
│   │
│   ├── action/                     # 动作策略 (策略模式)
│   │   ├── ActionStrategy.java             # 策略接口
│   │   ├── ActionStrategyFactory.java      # 策略工厂
│   │   ├── MoveActionStrategy.java         # 移动
│   │   ├── EatActionStrategy.java          # 进食
│   │   ├── RestActionStrategy.java         # 休息
│   │   ├── TalkActionStrategy.java         # 发言
│   │   ├── TradeActionStrategy.java        # 交易
│   │   ├── ProvokeActionStrategy.java     # 挑衅
│   │   ├── ClaimFoodActionStrategy.java    # 领食物
│   │   ├── PickupFoodActionStrategy.java  # 捡食物
│   │   └── StealActionStrategy.java       # 偷窃
│   │
│   └── tick/                       # Tick阶段 (责任链)
│       ├── TickPhase.java                  # 阶段接口
│       ├── PassiveConsumptionPhase.java     # 被动消耗
│       ├── OrderSwordSpawnPhase.java       # 秩序之剑生成
│       ├── RespawnPhase.java               # 复活
│       ├── PeaceEndingPhase.java           # 和平结局
│       └── AirdropPhase.java               # 空投
│
├── model/                          # 实体层
│   ├── Agent.java                 # Agent实体
│   ├── GameState.java             # 游戏状态
│   ├── GameConstants.java         # 游戏常量
│   ├── ActionLog.java            # 动作日志
│   └── ...
│
└── observer/                       # 观察者模式
    ├── GameObserver.java          # 观察者接口
    ├── WebSocketObserver.java     # WebSocket推送
    └── DatabaseObserver.java      # 数据库观察
```

### 3.2 Agent服务模块结构

```
agent/
├── main.py                         # FastAPI 入口
├── routers/                        # API路由
│   ├── decide.py                   # Agent决策
│   ├── judge.py                    # AI判官
│   └── health.py                  # 健康检查
├── services/                       # 服务层
│   ├── agent_scheduler.py          # Agent调度
│   ├── minimax_client.py           # MiniMax API调用
│   ├── rate_limiter.py             # 限流器
│   └── memory_manager.py           # 记忆管理
├── graphs/                         # LangGraph图
│   ├── leader_graph.py             # 领袖图
│   ├── soldier_graph.py            # 士兵图
│   └── judge_graph.py              # 判官图
├── prompts/                        # 提示词
│   ├── leader_prompt.py
│   ├── soldier_prompt.py
│   └── judge_prompt.py
└── tools/                          # 工具函数
    └── action_tools.py
```

---

## 四、数据流

### 4.1 Agent决策流程

```
Java Backend                      Python Agent
     │                                 │
     │  POST /api/agents/{id}/action   │
     │  {action: "move", target: "D"} │
     │ ───────────────────────────────►│
     │                                 │
     │                           RuleEngine.validateAndExecute()
     │                           ActionStrategyFactory.getStrategy("move")
     │                           MoveActionStrategy.execute()
     │                                 │
     │  {success: true, message: "..."}│
     │ ◄───────────────────────────────│
     │                                 │
     │  Agent状态更新                   │
     │  秩序之剑跟随检查                 │
     │  日志记录                       │
```

### 4.2 Tick结算流程

```
SimulationScheduler
         │
         ▼
SandboxStateMachine.executeTick()
         │
         ▼
┌─────────────────────────────────────┐
│  阶段1: PassiveConsumptionPhase     │
│  - 禁闭检查                         │
│  - 被动消耗（耐力、饱食度）         │
│  - 饥饿扣血                         │
│  - 健康回复                         │
│  - 死亡处理                         │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段2: OrderSwordSpawnPhase        │
│  - 第40回合生成秩序之剑             │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段3: RespawnPhase                │
│  - 复活倒计时                       │
│  - 复活处理                         │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段4: PeaceEndingPhase            │
│  - 和平结局条件检查                 │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  阶段5: AirdropPhase                │
│  - 每11轮空投物资                   │
└─────────────────────────────────────┘
         │
         ▼
     游戏状态保存
```

---

## 五、关键设计决策

### 5.1 为什么选择这些设计模式？

| 设计模式 | 适用场景 | 收益 |
|---------|---------|------|
| 策略模式 | 动作执行逻辑 | 解耦、易扩展、易测试 |
| 责任链模式 | Tick阶段串联 | 阶段独立、可排序、易扩展 |
| 工厂模式 | 策略自动注册 | 自动收集、无需配置 |
| 观察者模式 | 状态变更通知 | 解耦推送逻辑 |

### 5.2 事务管理策略

- **每个 TickPhase** 可独立设置 `@Transactional`
- **RuleEngine** 本身无事务，动作事务在 Controller 层管理
- **GodModeService** 设置 `@Transactional` 确保数据一致性

### 5.3 依赖注入策略

- 使用 `@RequiredArgsConstructor` + `final` 实现构造器注入
- Spring 自动收集 `List<T>` 注入（策略工厂、阶段链）
- 避免循环依赖：RuleEngine 不依赖 Repository，只通过策略类访问

---

## 六、扩展指南

### 6.1 添加新动作

1. 创建 `XxxActionStrategy implements ActionStrategy`
2. 标注 `@Component`
3. 实现 `getActionName()` 返回动作名
4. 工厂自动注册，无需其他修改

### 6.2 添加新Tick阶段

1. 创建 `XxxPhase implements TickPhase`
2. 标注 `@Component`
3. 设置 `getOrder()` 返回执行顺序
4. 自动纳入执行链

### 6.3 添加新上帝命令

1. 在 `GodModeService` 添加新方法
2. 在 `GodController` 添加新端点

---

## 七、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v2.1 | 2026-05-17 | 策略模式 + 责任链模式完整实现 |
| v2.0 | 2026-05-16 | Phase 5.5 架构升级规划 |
| v1.2 | 2026-05-06 | Phase 4 资源博弈系统 |
| v1.0 | 2026-04-16 | 初始版本 |

---

*文档版本：2.1*
*最后更新：2026-05-17*
*更新内容：*
* - 策略模式完整实现（ActionStrategy + Factory）*
* - 责任链模式完整实现（TickPhase）*
* - GodModeService 服务层分离*
* - 模块结构详细说明*
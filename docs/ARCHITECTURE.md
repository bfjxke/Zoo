# GuardianEye-IIoT 沙箱动物园 - 架构设计建议

> 本文档为架构师提供设计模式应用建议，用于优化当前代码结构和提升系统可扩展性。
>
> **版本**：v1.0
> **日期**：2026-04-16
> **状态**：参考建议，非强制要求

---

## 一、当前系统问题分析

### 1.1 SandboxStateMachine 问题

```java
// 当前：executeTick() 方法承担多个职责
public void executeTick() {
    // 职责1：被动消耗处理
    // 职责2：复活倒计时处理
    // 职责3：Python调度器调用
    // 职责4：游戏状态保存
}
```

**问题**：
- 违反单一职责原则（SRP）
- 难以单独测试某个阶段
- 添加新阶段需要修改核心类
- 代码耦合度高

### 1.2 RuleEngine 问题

```java
// 当前：所有数值计算在一个类中
public class RuleEngine {
    executeMove() {...}
    executeEat() {...}
    executeRest() {...}
    // 未来可能添加：executeSteal(), executeVote()...
}
```

**问题**：
- 类职责过多
- 新增动作需要修改核心类
- 难以实现动作的可插拔

### 1.3 WebSocket 推送问题

```java
// 当前：状态变化直接推送
public void executeTick() {
    // 计算状态
    // 立即推送
    webSocketService.push(state);
}
```

**问题**：
- 推送逻辑耦合在业务逻辑中
- 难以控制推送时机和内容
- 难以添加新的观察者

---

## 二、设计模式建议

### 2.1 单例模式（Singleton）- 最简单，必须

**应用场景**：全局唯一实例

```java
// 建议应用
public class RuleEngine {
    private static RuleEngine instance;

    public static RuleEngine getInstance() {
        if (instance == null) {
            instance = new RuleEngine();
        }
        return instance;
    }
}

// Spring Boot 中更简单的写法
@Service
public class RuleEngine {
    // Spring 自动管理为单例
}
```

**当前应用**：
| 类 | 是否单例 | 建议 |
|----|----------|------|
| RuleEngine | 否 | ✅ 改为单例 |
| PythonDispatcher | 否 | ✅ 改为单例 |
| GameStateRepository | 是 | 保持现状 |
| AgentRepository | 是 | 保持现状 |

---

### 2.2 责任链模式（Chain of Responsibility）- 优化状态机

**应用场景**：Tick结算的4个阶段

```
原始设计：
┌─────────────────────────────────────────┐
│  SandboxStateMachine.executeTick()      │
│  ├── 被动消耗                           │
│  ├── 复活处理                           │
│  ├── Python调度                         │
│  └── 状态保存                           │
└─────────────────────────────────────────┘

优化后：
┌─────────────┐
│   Handler   │ (接口)
└──────┬──────┘
       │
       ├──┌────────────────────┐
       │  │ PassiveConsumeHandler │ ──▶ 被动消耗
       ├──├────────────────────┤
       │  │ RespawnHandler       │ ──▶ 复活处理
       ├──├────────────────────┤
       │  │ PythonDispatchHandler │ ──▶ Python调度
       └──┴────────────────────┘
              │
              ▼
       ┌─────────────┐
       │  NextHandler │ ──▶ 状态保存
       └─────────────┘
```

**代码示例**：

```java
public interface TickPhaseHandler {
    void handle(GameState state, List<Agent> agents);

    default TickPhaseHandler setNext(TickPhaseHandler next) {
        return (s, a) -> {
            this.handle(s, a);
            next.handle(s, a);
        };
    }
}

// 实现类
@Component
public class PassiveConsumptionHandler implements TickPhaseHandler {
    @Override
    public void handle(GameState state, List<Agent> agents) {
        // 被动消耗逻辑
    }
}

@Component
public class RespawnHandler implements TickPhaseHandler {
    @Override
    public void handle(GameState state, List<Agent> agents) {
        // 复活逻辑
    }
}

// 使用
@Service
public class TickExecutor {
    private final List<TickPhaseHandler> handlers;

    public void execute(GameState state) {
        for (TickPhaseHandler handler : handlers) {
            handler.handle(state, agents);
        }
    }
}
```

**优势**：
- 每个阶段独立，可单独测试
- 添加新阶段只需新增Handler
- 可动态调整执行顺序

---

### 2.3 策略模式（Strategy）- 数值计算

**应用场景**：饥饿惩罚、Buff计算、投票计算

```
原始设计：
┌─────────────────────┐
│   RuleEngine        │
│   calculatePenalty() │ ← 多个if-else
│   calculateBuff()    │
└─────────────────────┘

优化后：
┌─────────────────────┐
│   PenaltyStrategy   │ (接口)
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
┌─────────┐ ┌─────────┐
│ Normal  │ │ Aggres │  ← 不同阵营可用不同策略
│Strategy │ │ sStrategy│
└─────────┘ └─────────┘
```

**代码示例**：

```java
public interface PenaltyStrategy {
    double calculateStaminaCost(double baseCost, Agent agent);
    double calculateSatietyCost(double baseCost, Agent agent);
    int calculateHealthDamage(Agent agent);
}

@Component
public class DefaultPenaltyStrategy implements PenaltyStrategy {
    @Override
    public double calculateStaminaCost(double baseCost, Agent agent) {
        double multiplier = 1.0;
        if (agent.isFatigued()) multiplier *= 1.5;
        if (agent.isHungry()) multiplier *= 1.5;
        return baseCost * multiplier;
    }
}

@Service
public class RuleEngine {
    private final Map<String, PenaltyStrategy> strategies;

    public double getStaminaCost(Agent agent) {
        PenaltyStrategy strategy = strategies.getOrDefault(
            agent.getFaction(),
            new DefaultPenaltyStrategy()
        );
        return strategy.calculateStaminaCost(BASE_COST, agent);
    }
}
```

**优势**：
- 不同阵营可用不同数值策略
- 便于调整游戏平衡
- 便于添加新的计算规则

---

### 2.4 观察者模式（Observer）- WebSocket推送

**应用场景**：状态变化通知

```
┌─────────────────┐
│   GameSubject   │ (被观察者)
│   - agents      │
│   - gameState   │
└────────┬────────┘
         │ notify()
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│WebSocket│ │  Log   │  ← 多个观察者
│Observer │ │Observer │
└────────┘ └────────┘
```

**代码示例**：

```java
public interface GameObserver {
    void onTickComplete(GameState state, List<Agent> agents);
    void onAgentAction(Agent agent, String action);
    void onGameEnd(GameResult result);
}

@Component
public class WebSocketObserver implements GameObserver {
    @Override
    public void onTickComplete(GameState state, List<Agent> agents) {
        // 推送更新到前端
        webSocketService.broadcast(state);
    }
}

@Service
public class GameNotifier {
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void notifyTickComplete(GameState state, List<Agent> agents) {
        for (GameObserver observer : observers) {
            observer.onTickComplete(state, agents);
        }
    }
}
```

**优势**：
- 推送逻辑与业务逻辑解耦
- 可添加多个观察者（WebSocket、Log、Metrics）
- 便于控制推送内容

---

### 2.5 命令模式（Command）- Agent动作

**应用场景**：Agent动作执行、撤销、重做

```
┌─────────────────┐
│   Command       │ (接口)
│   execute()     │
│   undo()        │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│  Move  │ │  Eat   │  ← 每个动作一个命令
│Command │ │Command │
└────────┘ └────────┘
```

**代码示例**：

```java
public interface AgentCommand {
    void execute(Agent agent);
    void undo(Agent agent);
    boolean canExecute(Agent agent);
}

@Component
public class MoveCommand implements AgentCommand {
    private final String targetNode;

    public MoveCommand(String targetNode) {
        this.targetNode = targetNode;
    }

    @Override
    public void execute(Agent agent) {
        agent.setCurrentNode(targetNode);
    }

    @Override
    public void undo(Agent agent) {
        agent.setCurrentNode(previousNode); // 需要记录之前位置
    }

    @Override
    public boolean canExecute(Agent agent) {
        return ruleEngine.validateMove(agent, targetNode).isSuccess();
    }
}

@Service
public class CommandExecutor {
    private final Stack<AgentCommand> history = new Stack<>();

    public void execute(AgentCommand command, Agent agent) {
        if (command.canExecute(agent)) {
            command.execute(agent);
            history.push(command);
        }
    }

    public void undo() {
        if (!history.isEmpty()) {
            AgentCommand command = history.pop();
            command.undo();
        }
    }
}
```

**优势**：
- 动作可撤销
- 便于实现动作日志
- 便于实现"回放"功能

---

## 三、架构升级建议

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │   Vue3     │  │  WebSocket  │  │  REST API   │       │
│  │  Frontend  │  │   Observer   │  │   (God)     │       │
│  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                          │
│  ┌─────────────────┐  ┌─────────────────┐                 │
│  │ SandboxStateMachine │  │   RuleEngine   │                 │
│  │  (责任链模式)    │  │  (策略模式)      │                 │
│  └─────────────────┘  └─────────────────┘                 │
│  ┌─────────────────┐  ┌─────────────────┐                 │
│  │ PythonDispatcher │  │   GameNotifier  │                 │
│  │  (异步调度)      │  │  (观察者模式)    │                 │
│  └─────────────────┘  └─────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │  MySQL      │  │   Redis     │  │   Kafka     │       │
│  │ (持久化)    │  │  (缓存)     │  │ (消息队列)   │       │
│  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 消息队列设计（可选）

用于解耦Java和Python的通信：

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│   Java   │───▶│  Kafka   │───▶│  Python  │
│  Spring  │    │  (队列)   │    │  FastAPI │
└──────────┘    └──────────┘    └──────────┘
                     │
                     ▼
              ┌──────────┐
              │  Dead    │  ← 死信队列
              │  Letter  │    (处理失败消息)
              └──────────┘
```

### 3.3 死信队列（Dead Letter Queue）

用于处理失败的API请求：

```yaml
# Kafka DLQ 配置示例
deadLetterQueue:
  topic: agent-decision-dlq
  maxRetries: 3
  retryDelay: 5s
  onFailure: manual-intervention
```

**处理流程**：
1. Python调用MiniMax API失败
2. 进入重试队列
3. 重试3次仍失败
4. 进入死信队列
5. 触发人工干预告警

---

## 四、扩展性设计

### 4.1 新增阵营

```java
// 配置化阵营
public class FactionConfig {
    String name;           // 阵营名
    String baseNode;       // 基地节点
    String primaryColor;    // 颜色
    PenaltyStrategy penaltyStrategy;  // 惩罚策略
}

// 配置文件
faction:
  - name: lawful
    baseNode: base_lawful
    penaltyStrategy: default

  - name: aggressive
    baseNode: base_aggressive
    penaltyStrategy: aggressive

  - name: neutral
    baseNode: base_neutral
    penaltyStrategy: default
```

### 4.2 新增动作

```java
// 命令模式支持新动作
public interface AgentCommand {
    String getName();
    boolean canExecute(Agent agent);
    void execute(Agent agent);
}

// 新增动作只需实现接口
@Component
public class StealCommand implements AgentCommand {
    @Override
    public String getName() { return "steal"; }
    // ...
}
```

### 4.3 新增结局

```java
// 策略模式支持新结局
public interface EndingStrategy {
    boolean checkCondition();
    String getName();
    void execute();
}

@Component
public class PeaceEnding implements EndingStrategy {
    @Override
    public boolean checkCondition() {
        return allFactionsAlive() &&
               hasOrderSword() &&
               declarationApproved();
    }
}
```

---

## 五、优先级建议

| 模式 | 优先级 | 工作量 | 收益 |
|------|--------|--------|------|
| 单例模式 | P0 | 低 | 确保全局唯一 |
| 观察者模式 | P1 | 中 | 解耦推送逻辑 |
| 责任链模式 | P1 | 中 | 解耦Tick阶段 |
| 策略模式 | P2 | 中 | 灵活计算规则 |
| 命令模式 | P2 | 高 | 动作可撤销 |

**建议实施顺序**：
1. **Phase 2.1**：单例 + 观察者模式
2. **Phase 2.2**：责任链模式（Tick阶段）
3. **Phase 2.3**：策略模式（数值计算）

---

## 六、风险与注意事项

### 6.1 过度设计风险

```
警告：不要为了设计模式而设计模式！

检查清单：
□ 这个模式解决了什么问题？
□ 不使用这个模式会有多糟糕？
□ 团队是否熟悉这个模式？
□ 未来是否真的会扩展？
```

### 6.2 性能考虑

| 模式 | 性能影响 | 注意事项 |
|------|----------|----------|
| 责任链 | 轻微 | 链不长时影响可忽略 |
| 策略 | 无 | 运行时多态调用 |
| 观察者 | 轻微 | 观察者不宜过多 |
| 命令 | 轻微 | 历史栈不宜过大 |

---

## 七、总结

### 设计模式应用总结

| 模式 | 核心价值 | 适用场景 |
|------|----------|----------|
| 单例 | 全局唯一 | 全局服务类 |
| 责任链 | 解耦串联 | Tick阶段、过滤器 |
| 策略 | 算法替换 | 数值计算、惩罚规则 |
| 观察者 | 解耦通知 | WebSocket、日志 |
| 命令 | 操作封装 | Agent动作 |

### 核心理念

```
1. 保持简单：KISS原则
2. 不要过度设计：YAGNI原则
3. 优先组合：优先使用组合而非继承
4. 面向接口：依赖抽象而非具体
```

---

*文档版本：1.0*
*最后更新：2026-04-16*
*作者：AI Assistant*
*用途：架构设计参考，非强制要求*

# Phase 3 规格说明

> GuardianEye-IIoT 沙箱动物园 - 可观测性与审计
>
> **版本**：1.0
> **日期**：2026-04-23
> **前置条件**：Phase 0 + Phase 1 + Phase 2 完成

---

## 一、Phase 3 目标概述

### 1.1 核心目标

| 目标 | 说明 | 优先级 |
|------|------|--------|
| 统一日志系统 | 格式化日志输出到控制台和文件 | P0 |
| 完整MySQL表结构 | 所有数据持久化 | P0 |
| 违规操作日志 | 单独记录到violation.log | P1 |
| 前端监控大屏 | 实时显示Agent状态和日志 | P1 |

### 1.2 为什么可观测性是最高准则？

```
这不是游戏，是社会学实验！

❌ 只看结果：Agent A赢了
✅ 看过程：Agent A在第20回合吃了什么，为什么选择移动到center

可观测性 = 理解"为什么"的能力
```

---

## 二、统一日志系统

### 2.1 日志格式

```
[Tick #][Agent Name][Faction][Action] -> [Result] | [Judge ID][Success Rate]

示例：
[Tick #15][张三][lawful][move] -> center | [JUDGE_PENDING][0.6]
[Tick #15][李四][aggressive][eat] -> 成功 | []
```

### 2.2 日志类型

| 类型 | 输出位置 | 说明 |
|------|----------|------|
| 正常日志 | 控制台 + 文件 | 所有Agent动作 |
| 违规日志 | violation.log | 违规操作单独记录 |
| 系统日志 | system.log | Tick开始/结束等 |

### 2.3 audit_logger.py 设计

```python
import logging
from logging.handlers import TimedRotatingFileHandler
from datetime import datetime

class AuditLogger:
    """统一日志管理器"""
    
    def __init__(self):
        # 创建logger
        self.logger = logging.getLogger("audit")
        self.logger.setLevel(logging.INFO)
        
        # 格式化器
        formatter = logging.Formatter(
            "[Tick #%(tick)s][%(agent)s][%(faction)s][%(action)s] -> %(result)s | %(judge)s[%(rate)s]"
        )
        
        # 控制台处理器
        console = logging.StreamHandler()
        console.setFormatter(formatter)
        
        # 文件处理器（按天分割）
        file_handler = TimedRotatingFileHandler(
            "logs/audit.log",
            when="midnight",  # 每天午夜分割
            interval=1
        )
        file_handler.setFormatter(formatter)
        
        # 违规日志处理器（单独文件）
        violation_handler = logging.FileHandler("logs/violation.log")
        violation_handler.setLevel(logging.WARNING)
        violation_handler.setFormatter(formatter)
        
        self.logger.addHandler(console)
        self.logger.addHandler(file_handler)
        self.logger.addHandler(violation_handler)
    
    def log_action(self, tick, agent, faction, action, result, judge_id=None, success_rate=None):
        """记录Agent动作"""
        self.logger.info(
            f"{tick},{agent},{faction},{action},{result}",
            extra={
                "tick": tick,
                "agent": agent,
                "faction": faction,
                "action": action,
                "result": result,
                "judge": judge_id or "",
                "rate": success_rate or ""
            }
        )
    
    def log_violation(self, tick, agent, faction, action, reason):
        """记录违规操作"""
        self.logger.warning(
            f"违规: {tick},{agent},{faction},{action} - {reason}",
            extra={
                "tick": tick,
                "agent": agent,
                "faction": faction,
                "action": action,
                "result": f"违规: {reason}",
                "judge": "VIOLATION",
                "rate": ""
            }
        )
```

---

## 三、MySQL表结构设计

### 3.1 表概览

```
┌─────────────────────────────────────────┐
│           GuardianEye Zoo Schema         │
├─────────────────────────────────────────┤
│ agents              : Agent状态          │
│ agent_states        : Agent历史快照      │
│ action_logs         : 动作历史记录       │
│ social_records      : 聊天外交记录        │
│ dynamic_rules       : AI判官临时规则     │
│ god_operations      : 上帝干预操作       │
│ votes               : 和平结局投票       │
│ game_state          : 游戏状态          │
│ leader_values       : 领袖价值观种子     │
└─────────────────────────────────────────┘
```

### 3.2 详细表结构

#### agents（当前Agent状态）

```sql
CREATE TABLE agents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    faction VARCHAR(50) NOT NULL,           -- lawful/aggressive/neutral
    role VARCHAR(20) DEFAULT 'soldier',    -- leader/soldier/judge
    stamina INT DEFAULT 100,
    satiety INT DEFAULT 100,
    health INT DEFAULT 90,
    current_node VARCHAR(50),
    alive BOOLEAN DEFAULT TRUE,
    fatigue_threshold INT DEFAULT 20,
    hunger_threshold INT DEFAULT 30,
    death_ticks_remaining INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_faction (faction),
    INDEX idx_alive (alive)
);
```

#### agent_states（Agent历史快照）

```sql
CREATE TABLE agent_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    tick_number INT NOT NULL,
    stamina INT NOT NULL,
    satiety INT NOT NULL,
    health INT NOT NULL,
    current_node VARCHAR(50),
    is_fatigued BOOLEAN,
    is_hungry BOOLEAN,
    is_alive BOOLEAN,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (agent_id) REFERENCES agents(id),
    INDEX idx_agent_tick (agent_id, tick_number)
);
```

#### action_logs（动作历史记录）

```sql
CREATE TABLE action_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    faction VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target VARCHAR(100),
    result VARCHAR(200),
    judge_id VARCHAR(50),
    success_rate DECIMAL(3,2),
    is_violation BOOLEAN DEFAULT FALSE,     -- 是否违规
    violation_reason VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_tick (tick_number),
    INDEX idx_faction (faction),
    INDEX idx_violation (is_violation)
);
```

#### social_records（聊天外交记录）

```sql
CREATE TABLE social_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    speaker_name VARCHAR(100) NOT NULL,
    speaker_faction VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,         -- public/lawful_private/aggressive_private/neutral_private
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_channel (channel),
    INDEX idx_tick (tick_number)
);
```

#### dynamic_rules（AI判官临时规则）

```sql
CREATE TABLE dynamic_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_created INT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_description TEXT,
    approved BOOLEAN DEFAULT FALSE,
    approved_by VARCHAR(50),                 -- agent name or AI judge
    expires_at_tick INT NOT NULL,          -- 过期回合
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_expires (expires_at_tick),
    INDEX idx_active (is_active)
);
```

#### god_operations（上帝干预操作）

```sql
CREATE TABLE god_operations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    operator_action VARCHAR(100) NOT NULL,  -- move/buff/heal/spawn等
    target_type VARCHAR(50),                -- agent/node/item
    target_id BIGINT,
    target_name VARCHAR(100),
    parameters JSON,                        -- 操作参数
    result VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_tick (tick_number)
);
```

---

## 四、设计模式应用（Phase 3）

### 4.1 责任链模式（必须应用）

**场景**：日志处理链路

```java
public interface LogHandler {
    void handle(LogEntry log);
    LogHandler setNext(LogHandler next);
}

public class ConsoleHandler implements LogHandler {
    @Override
    public void handle(LogEntry log) {
        System.out.println(format(log));  // 输出到控制台
        next.handle(log);  // 传递给下一个handler
    }
}

public class FileHandler implements LogHandler {
    @Override
    public void handle(LogEntry log) {
        writeToFile(log);  // 写入文件
        next.handle(log);
    }
}

public class ViolationHandler implements LogHandler {
    @Override
    public void handle(LogEntry log) {
        if (log.isViolation()) {
            writeToViolationLog(log);  // 违规写入单独文件
        }
        next.handle(log);
    }
}

// 使用
LogHandler chain = new ConsoleHandler()
    .setNext(new FileHandler())
    .setNext(new ViolationHandler());

// 所有日志统一经过这条链
chain.handle(logEntry);
```

**为什么用责任链？**
```
新增日志输出方式？新增Handler，不用改原有代码
跳过某个handler？注释掉一行
测试某个handler？单独测试
```

### 4.2 观察者模式（必须应用）

**场景**：状态变化通知

```java
public interface GameObserver {
    void onTickComplete(TickResult result);
    void onAgentAction(ActionLog log);
    void onPeaceEnding(String winner);
}

public class DatabaseObserver implements GameObserver {
    @Override
    public void onTickComplete(TickResult result) {
        // 保存到数据库
        agentStateRepository.saveAll(result.getStates());
        actionLogRepository.saveAll(result.getActions());
    }
}

public class WebSocketObserver implements GameObserver {
    @Override
    public void onAgentAction(ActionLog log) {
        // 推送WebSocket
        messagingTemplate.convertAndSend("/topic/action", log);
    }
}

// 通知中心
public class GameNotifier {
    private List<GameObserver> observers = new CopyOnWriteArrayList<>();
    
    public void notifyAgentAction(ActionLog log) {
        for (GameObserver o : observers) {
            o.onAgentAction(log);
        }
    }
}
```

**为什么用观察者？**
```
解耦：业务逻辑不需要知道有多少观察者
扩展：新增观察者只需addObserver
灵活：可以动态添加/删除观察者
```

---

## 五、前端监控大屏

### 5.1 功能需求

```
┌────────────────────────────────────────────────────────────────┐
│  GuardianEye-IIoT 沙箱动物园 - 监控大屏                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────┐  ┌────────────────────────────────┐ │
│  │    阵营统计         │  │      实时日志流                 │ │
│  │  ┌────┐┌────┐┌────┐│  │                                │ │
│  │  │守序││激进││中立││  │  [Tick #15] 张三移动到center    │ │
│  │  │ 5  ││ 5  ││ 5  ││  │  [Tick #15] 李四吃了食物       │ │
│  │  └────┘└────┘└────┘│  │  [Tick #15] 王五发言：...      │ │
│  └──────────────────────┘  │                                │ │
│                            │                                │ │
│  ┌──────────────────────┐  └────────────────────────────────┘ │
│  │    Agent状态列表     │                                     │
│  │  ┌────────────────┐  │  ┌────────────────────────────────┐ │
│  │  │ 张三            │  │  │      阵营资源图表              │ │
│  │  │ 耐力 ████████░░│  │  │  ┌──────────────────────┐   │ │
│  │  │ 饱食 ██████░░░░│  │  │  │     资源分布饼图       │   │ │
│  │  │ 生命 █████████░│  │  │  │ 守序40% 激进35% 中立25%│   │ │
│  │  └────────────────┘  │  │  └──────────────────────┘   │ │
│  └──────────────────────┘  └────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### 5.2 技术实现

```javascript
// WebSocket实时推送
const ws = new WebSocket('ws://localhost:8080/ws/game');

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    
    if (data.type === 'TICK_COMPLETE') {
        updateAgentList(data.agents);  // 更新Agent状态
        appendLog(data.logs);         // 追加日志
        updateCharts(data.stats);      // 更新图表
    }
};
```

---

## 六、可观测性核心问题

### 6.1 为什么可观测性是最高准则？

```
这不是游戏，是社会学实验！

❌ 游戏：Agent A赢了
✅ 实验：Agent A在第20回合选择移动到center，因为...

可观测性 = 理解"为什么"的能力
```

### 6.2 为什么数据要存MySQL？

| 原因 | 说明 |
|------|------|
| 持久化 | 服务器重启数据不丢失 |
| 可查询 | 用SQL查任意历史数据 |
| 事务性 | 保证数据一致性 |

### 6.3 日志格式为什么必须统一？

```
统一格式的好处：
1. 方便AI分析（可以批量处理）
2. 方便排查问题（一目了然）
3. 方便统计（写SQL就行）
```

---

## 七、Phase 3 与备忘录整合

### 7.1 Phase 3 实现内容

```
✅ 统一日志系统
✅ 完整MySQL表结构
✅ 违规操作日志
✅ 前端监控大屏
✅ 责任链模式（日志处理）
✅ 观察者模式（状态通知）
```

### 7.2 备忘录待实现

```
Phase 4：
- 阵营世界观策略模式
- Agent自主记忆管理
- 讨论版权限系统

Phase 5：
- 偷窃机制
- 食物完整系统
- Agent人格蒸馏
```

---

## 八、SQL查询示例

### 8.1 查询过去10回合所有违规操作

```sql
SELECT tick_number, agent_name, faction, action, violation_reason
FROM action_logs
WHERE is_violation = TRUE
AND tick_number > (SELECT MAX(tick_number) FROM action_logs) - 10
ORDER BY tick_number DESC;
```

### 8.2 查询守序阵营平均饱食度

```sql
SELECT 
    faction,
    AVG(stamina) as avg_stamina,
    AVG(satiety) as avg_satiety,
    AVG(health) as avg_health
FROM agent_states a
JOIN agents ag ON a.agent_id = ag.id
WHERE ag.faction = 'lawful'
AND tick_number = (SELECT MAX(tick_number) FROM agent_states)
GROUP BY faction;
```

### 8.3 查询Agent行动统计

```sql
SELECT 
    agent_name,
    faction,
    COUNT(*) as total_actions,
    SUM(CASE WHEN action = 'move' THEN 1 ELSE 0 END) as move_count,
    SUM(CASE WHEN action = 'eat' THEN 1 ELSE 0 END) as eat_count,
    SUM(CASE WHEN action = 'talk' THEN 1 ELSE 0 END) as talk_count
FROM action_logs
WHERE tick_number > (SELECT MAX(tick_number) FROM action_logs) - 20
GROUP BY agent_name, faction
ORDER BY total_actions DESC;
```

---

## 九、互动问题解答

### 问题1：增加"攻击"动作需要修改哪些表？

```sql
-- 1. action_logs：添加攻击动作（已有action字段，无需修改）
-- 2. agents：可能需要添加防御属性（新增字段）
ALTER TABLE agents ADD COLUMN defense INT DEFAULT 0;

-- 3. dynamic_rules：添加攻击相关规则（已有，无需修改）

-- 4. 新增：攻击记录表
CREATE TABLE attack_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    attacker_name VARCHAR(100),
    target_name VARCHAR(100),
    damage INT,
    success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 问题2：为什么日志存文件也存数据库？

```
不是"只存文件"或"只存数据库"！

日志文件：
- 实时查看（tail -f）
- 问题排查
- 高频写入性能好

数据库：
- 历史查询
- 统计分析
- 持久化

两者结合是最好的！
```

---

## 十、预计工作量

| 模块 | 任务 | 预估 |
|------|------|------|
| 日志系统 | audit_logger.py | 4h |
| 日志系统 | 责任链模式重构 | 4h |
| 数据库 | schema.sql | 2h |
| 数据库 | 观察者模式重构 | 4h |
| 前端 | 监控大屏 | 8h |
| 测试 | 集成测试 | 4h |
| **总计** | | **26h** |

---

*规格版本：1.0*
*最后更新：2026-04-23*
*前置条件：Phase 0 + Phase 1 + Phase 2 完成*

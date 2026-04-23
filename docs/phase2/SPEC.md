# Phase 2 规格说明

> GuardianEye-IIoT 沙箱动物园 - Python AI调度系统 + 游戏机制增强
>
> **版本**：1.0
> **日期**：2026-04-16
> **前置条件**：Phase 0 + Phase 1 完成

---

## 一、Phase 2 目标概述

### 1.1 核心目标

| 目标 | 说明 | 优先级 |
|------|------|--------|
| Python AI调度系统 | 异步并发+限流+记忆管理 | P0 |
| 食物Buff简化版 | 无限食物，只保留Buff | P1 |
| 秩序之剑系统 | 生成/拾取/掉落 | P1 |
| 和平结局 | 秩序宣言+投票 | P2 |

### 1.2 简化版 vs 完整版

```
【简化版（本Phase实现）】
├── 食物无限
├── 无营地库存
├── 无偷窃机制
└── 无权限系统

【完整版（后续Phase）】
├── 食物投放+争抢+运输
├── 营地库存系统
├── 偷窃机制
└── 讨论版权限系统
```

---

## 二、Python AI 调度系统

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Java Spring Boot                        │
│                    （游戏引擎+规则引擎）                       │
└──────────────────────────┬────────────────────────────────┘
                           │ HTTP POST /decide
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     Python FastAPI                            │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐   │
│  │  Routers    │───▶│   Services   │───▶│ MiniMax API │   │
│  │ decide.py   │    │scheduler.py  │    │ m2.7/m2-her │   │
│  │             │    │rate_limiter │    │             │   │
│  └─────────────┘    └──────────────┘    └─────────────┘   │
│                           │                                   │
│                    ┌──────┴──────┐                           │
│                    │  Memory     │                           │
│                    │  Manager    │                           │
│                    └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模型分层策略

| Agent类型 | 数量 | 模型 | 说明 |
|-----------|------|------|------|
| 守序领袖（lawful + leader） | 1 | m2.7 | 标准版，稳定决策 |
| 裁判（judge） | 1 | m2.7 | 标准版，公平判断 |
| 强势阵营随机1个（非领袖） | 1 | m2.7 | 标准版 |
| 中立阵营随机1个（非领袖） | 1 | m2.7 | 标准版 |
| 其他所有Agent | ~11 | m2-her | 角色扮演版 |

### 2.3 异步并发调度

```python
# 目标：15个Agent同时请求
async def dispatch_all_agents(agents: List[Agent]):
    # 使用asyncio.gather并发执行
    tasks = [make_decision(agent) for agent in agents]
    decisions = await asyncio.gather(*tasks, return_exceptions=True)
    return decisions
```

**优势**：
- 原来：串行，15秒（1秒/个）
- 现在：并发，约1-2秒

### 2.4 令牌桶限流

```python
class TokenBucketRateLimiter:
    def __init__(self, rate: int = 1):
        self.rate = rate  # 每秒1个请求
        self.tokens = 0
        self.last_update = time.time()

    async def acquire(self):
        # 获取令牌，超时跳过
        pass

    async def acquire_with_wait(self):
        # 获取令牌，等待可用
        pass
```

**参数**：
| 参数 | 值 | 说明 |
|------|------|------|
| rate | 1 | 每秒1个请求 |
| max_tokens | 1 | 桶容量 |
| timeout | 30s | 超时跳过 |

### 2.5 记忆管理系统

```python
class AgentMemory:
    def __init__(self):
        self.recent = []      # 常规记忆（12回合）
        self.important = []  # 重要记忆（永久）

    def add_memory(self, content: str, is_important: bool = False):
        if is_important:
            self.important.append(content)
        else:
            self.recent.append(content)

    def cleanup_recent(self, max_turns: int = 12):
        # 保留最近12回合
        pass

    def build_context(self) -> str:
        # 构建发送给AI的上下文
        return f"[重要记忆]\n{important}\n[近期记忆]\n{recent}"
```

**记忆分类**：
| 类型 | 保存时长 | 示例 |
|------|----------|------|
| 常规记忆 | 12回合 | 移动轨迹、动作记录 |
| 重要记忆 | 永久 | 秩序之剑作用、结盟、奸细 |

### 2.6 MiniMax API 封装

```python
async def call_minimax(
    system_prompt: str,
    user_prompt: str,
    model: str = "m2.7",
    memory_context: str = ""
) -> Optional[str]:
    # 1. 截断上下文到token限制
    # 2. 构建最终prompt
    # 3. 调用API（带重试）
    # 4. 返回结果
```

**功能**：
- 自动重试（3次）
- 上下文截断（保护token限制）
- Mock降级（API不可用时）

---

## 三、游戏机制

### 3.1 食物Buff（简化版）

```
【简化版设计】
├── 食物无限
├── 不消耗库存
└── 只保留Buff效果

【Buff效果】
├── 饱食度 > 100
├── 耐力恢复 ×1.7
└── 每回合回血 +10
```

### 3.2 秩序之剑系统

```java
public class OrderSword {
    String location;      // 所在节点
    Long holderId;       // 持有者Agent ID
    boolean spawned;     // 是否已生成
}
```

**生成规则**：
| 参数 | 值 |
|------|------|
| 生成时间 | 第40回合 |
| 生成位置 | 随机中立节点 |
| 唯一性 | 整局只有一把 |

**拾取规则**：
```
Agent移动到剑所在节点
    ↓
自动拾取（无需动作）
    ↓
剑跟随Agent移动
```

**掉落规则**：
```
Agent死亡
    ↓
剑掉落在当前位置
    ↓
需要重新拾取
```

### 3.3 守序阵营Buff

```java
// 当守序阵营持有秩序之剑时
if (faction == "lawful" && hasOrderSword()) {
    stamina *= 1.1;      // 全属性+10%
    satiety *= 1.1;
    health *= 1.1;
}
```

### 3.4 和平结局

**触发条件**（需同时满足）：
```
1. 游戏进行 ≥ 40回合
2. 所有阵营都有Agent存活
3. 守序阵营持有秩序之剑
4. 守序阵营发布"秩序宣言"
5. 激进阵营过半数同意
```

**秩序宣言**：
```java
public class OrderDeclaration {
    Long agentId;          // 发布者
    int tick;             // 发布回合
    int cooldown;          // 冷却（10回合）
    boolean active;        // 是否生效
}
```

**投票系统**：
```java
public class Vote {
    int tick;             // 投票回合
    String voter;         // 投票者
    boolean agree;        // 是否同意
}
```

---

## 四、数据模型

### 4.1 Agent 增强

```sql
-- 新增role字段
ALTER TABLE agents ADD COLUMN role VARCHAR(20) DEFAULT 'soldier';

-- role类型
-- 'leader'  - 领袖
-- 'soldier' - 普通成员
-- 'judge'   - 裁判
```

### 4.2 GameState 增强

```sql
-- 秩序之剑状态
ALTER TABLE game_state ADD COLUMN order_sword JSON;

-- JSON结构
{
    "location": "center",
    "holderId": null,
    "spawned": false,
    "spawnTick": 40
}

-- 宣言状态
ALTER TABLE game_state ADD COLUMN order_declaration JSON;

-- JSON结构
{
    "active": false,
    "lastTick": 0
}
```

### 4.3 Vote 表（新增）

```sql
CREATE TABLE votes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    agent_name VARCHAR(100),
    declaration_tick INT,
    vote_result BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 五、API接口

### 5.1 Java → Python

```python
# POST /decide
{
    "tick": 10,
    "agents": [
        {
            "id": 1,
            "name": "张三",
            "faction": "lawful",
            "role": "leader",
            "stamina": 80,
            "satiety": 90,
            "health": 90,
            "current_node": "center",
            "memory": {
                "recent": [...],
                "important": [...]
            }
        }
    ]
}

# Response
{
    "decisions": [
        {
            "agent_id": 1,
            "agent_name": "张三",
            "action": "move",
            "target": "base_lawful",
            "model_used": "m2.7",
            "reasoning": "..."
        }
    ]
}
```

### 5.2 Python → MiniMax

```python
# HTTP POST
URL: https://api.minimax.chat/v1/text/chatcompletion_v2
Headers: Authorization: Bearer {API_KEY}
Body: {
    "model": "m2.7" 或 "m2-her",
    "messages": [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt + memory_context}
    ],
    "temperature": 0.7,
    "max_tokens": 512
}
```

---

## 六、架构设计建议

### 6.1 推荐设计模式

| 模式 | 应用 | 优先级 |
|------|------|--------|
| 单例模式 | 全局服务 | P0 |
| 观察者模式 | WebSocket推送 | P1 |
| 责任链模式 | Tick阶段 | P1 |
| 策略模式 | 数值计算 | P2 |

详见：`docs/ARCHITECTURE.md`

### 6.2 扩展预留

```java
// 动作扩展预留
public interface AgentCommand {
    String getName();
    boolean canExecute(Agent agent);
    void execute(Agent agent);
}

// 新增动作只需实现接口
@Component
public class StealCommand implements AgentCommand { }
```

---

## 七、Phase 2 里程碑

| 里程碑 | 内容 | 验收 |
|--------|------|------|
| M1 | Python基础设施完成 | 15个Agent能并发请求 |
| M2 | 记忆管理系统完成 | 记忆正确保存12回合 |
| M3 | 秩序之剑完成 | 生成→拾取→掉落全流程 |
| M4 | 和平结局完成 | 投票→结局触发 |
| M5 | 集成测试完成 | 端到端可运行 |

---

## 八、已知限制

| 限制 | 说明 | 后续改进 |
|------|------|----------|
| 食物无限 | 简化版 | Phase 3添加库存 |
| 无权限系统 | 简化版 | Phase 3添加讨论 |
| 无偷窃 | 逻辑待完善 | 备忘录 |
| 无持久化记忆 | 仅本局有效 | 后续改进 |

---

## 九、参考文档

| 文档 | 位置 | 说明 |
|------|------|------|
| 游戏规则白皮书 | `docs/rules.md` | v1.1规则 |
| 项目结构 | `docs/PROJECT_STRUCTURE.md` | 目录说明 |
| 架构建议 | `docs/ARCHITECTURE.md` | 设计模式 |
| 功能备忘录 | `docs/FEATURE_MEMO.md` | 待实现功能 |
| 改动日志 | `docs/CHANGELOG.md` | 版本记录 |

---

*规格版本：1.0*
*最后更新：2026-04-16*
*前置条件：Phase 0 + Phase 1 完成*

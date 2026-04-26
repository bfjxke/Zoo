# GuardianEye-IIoT 沙箱动物园 - 功能备忘录

> 本文档记录用户提出的但尚未实现的功能点子，供后续阶段参考。

---

## 备忘录使用说明

- 每开始下一阶段前，查看此文档
- 将计划实现的功能整合到阶段计划书中
- 已实现的功能移到"已实现"区域并标注版本

---

## 一、待实现功能

### 1.1 营地食物库存系统

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 每个阵营营地有独立食物库存（如100份）
- Agent吃食物时消耗库存
- 库存耗尽则无法吃
- 引导偷窃行为

**预计阶段**：Phase 2 或 Phase 3

**实现建议**：
```java
// 在GameState或营地模型中新增
private Map<String, Integer> foodInventory;  // 阵营 -> 库存

// 修改RuleEngine.executeEat()
if (库存足够) {
    库存--;
    饱食度 += 30;
} else {
    return "营地食物耗尽";
}
```

**博弈效果**：
- 强势方囤积食物 → 库存高 → Buff持续
- 弱势方偷窃 → 消耗强势方库存
- 资源争夺形成动态平衡

---

### 1.2 食物携带系统

**来源**：用户讨论（2026-04-16）

**功能描述**：
- Agent可携带少量食物（如最多3份）
- 外出时可以吃携带的食物
- 不在营地也能恢复

**预计阶段**：Phase 2

**限制建议**：
- 每份食物占用"背包"空间
- 携带数量有限（如最多3份）
- 死亡时食物掉落

---

### 1.3 强势方优势机制

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 强势阵营拥有某些天然优势
- 例如：更强壮（耐力消耗减少）、更抗饿（饥饿扣血减少）

**预计阶段**：Phase 3 或 Phase 4

**设计建议**：
```java
// 在GameConstants中新增阵营系数
public static final Map<String, Double> FACTION_STAMINA_MODIFIER = Map.of(
    "lawful", 1.0,
    "aggressive", 0.9,   // 消耗减少10%
    "neutral", 1.0
);
```

---

### 1.4 弱势方联合机制

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 引导两个弱势阵营联合对抗强势方
- 例如：联合buff（同时在场的同阵营Agent恢复加速）
- 或者：联合技能（两方配合执行复杂动作）

**预计阶段**：Phase 4

**设计建议**：
- 同阵营Agent数量≥2时触发
- 提供资源加成或信息优势

---

### 1.5 偷窃机制

**来源**：用户讨论（2026-04-16）

**功能描述**：
- Agent可以偷取其他阵营的食物库存
- 需要进入对方营地
- 有被抓住的风险

**预计阶段**：Phase 2（与营地库存系统一起实现）

**设计建议**：
```java
public ActionResult stealFood(Agent agent, String targetFaction) {
    // 检查是否在对方营地
    if (!在对方营地) return "需要进入目标营地";

    // 消耗耐力
    if (耐力不足) return "耐力不足";

    // 偷取一份
    目标阵营库存--;
    return "偷取成功，获得1份食物";
}
```

---

### 1.6 动态资源刷新

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 野外资源点（森林、河流、山地）定时刷新食物
- 玩家需要抢占资源点
- 不抢占则资源枯竭

**预计阶段**：Phase 2 或 Phase 3

---

### 1.7 神级干预增强

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 放置食物到指定位置
- 创造特殊事件（如资源争夺战、联盟谈判）
- 强制Agent执行动作（用于调试）

**预计阶段**：Phase 1（部分已实现）或 Phase 2

---

### 1.8 Agent人格蒸馏

**来源**：用户讨论（2026-04-16）

**功能描述**：
- 提取用户的人格特征
- 结合阵营价值观种子
- 生成"像用户但更阵营化"的AI Agent

**预计阶段**：Phase 5 或后续

**设计建议**：
```
1. 用户完成人格测试问卷
2. 提取特征向量（进攻性/防守性/合作性等）
3. 结合阵营种子生成Prompt
4. 蒸馏出独特的AI人格
```

---

### 1.9 阵营联盟系统

**来源**：未来扩展预留

**功能描述**：
- 跨阵营结盟
- 共享信息
- 协同行动

**预计阶段**：Phase 4 或 Phase 5

---

### 1.10 装备系统

**来源**：未来扩展预留

**功能描述**：
- Agent可装备道具
- 提供属性加成
- 增加策略深度

**预计阶段**：Phase 4 或 Phase 5

---

## 二、已实现功能

| 功能 | 实现版本 | 说明 |
|------|----------|------|
| 4阶段Tick执行 | Phase 1 | SandboxStateMachine.executeTick() |
| 白名单动作验证 | Phase 1 | RuleEngine.validateAndExecute() |
| 疲劳/饥饿惩罚系统 | Phase 1 | 1.5倍叠加惩罚 |
| 死亡复活机制 | Phase 1 | 3回合倒计时+50%属性 |
| 饱食Buff系统 | Phase 1 | 饱食>100时恢复加速50% |
| 地图节点系统 | Phase 1 | 7节点辐射状结构 |
| 阵营私聊系统 | Phase 1 | 基地内私聊 |
| 上帝视角地图 | Phase 0 | Vue前端 |
| Python AI调度 | Phase 0 | FastAPI服务 |

---

## 三、待讨论功能

### 3.1 Buff机制细节

**问题**：Buff持续时间是否需要独立计时？

**方案A**（当前）：Buff与饱食度绑定
- 饱食>100时触发
- 饱食≤100时消失
- 简单，无需额外计时

**方案B**：独立Buff计时
- 吃完后持续N回合
- 与饱食度无关
- 复杂但更灵活

**当前选择**：方案A（简化版）

---

### 3.2 资源争夺胜利条件

**问题**：如何判定阵营胜负？

**方案A**：消灭其他阵营
- 所有敌对Agent死亡
- 可能导致游戏无限进行

**方案B**：积分制度
- 占领资源点得分
- 存活时间得分
- 游戏时长限制

**待定**：尚未决定

---

## 四、下一步计划

### Phase 1 已完成内容（v1.1）
- ✅ Java状态机（4阶段Tick）
- ✅ 规则引擎（白名单验证）
- ✅ 饱食Buff系统 v1.1增强
- ✅ 详细注释和文档
- ✅ 备忘录创建
- ✅ 地图可视化文档
- ✅ 改动日志文档
- ✅ v1.1规则重做（饥饿惩罚、健康系统、和平结局）
- ✅ 白名单新增挑衅动作
- ✅ AI判官规则有效期缩短到8回合

### Phase 2 预研功能
- 🔄 营地食物库存系统
- 🔄 偷窃机制
- 🔄 Python AI决策生成（MiniMax集成）
- 🔄 API限流处理
- 🔄 秩序之剑完整实现（拾取、掉落、抢夺）
- 🔄 和平结局完整实现（投票系统）

---

## 四、Phase 2 已完成内容（v1.2）

### 已完成
- ✅ Python异步并发调度（asyncio.gather）
- ✅ 令牌桶限流器（TokenBucketRateLimiter）
- ✅ Agent记忆管理器（16回合常规+永久重要）
- ✅ 重要记忆可修改（盟友变敌人）
- ✅ 秩序之剑详细信息模板
- ✅ 模型分层策略（m2.7/m2-her）

### 预研功能
- 🔄 死信队列（Dead Letter Queue）
- 🔄 Agent自主判断重要记忆
- 🔄 记忆动态更新机制

---

## 四、Phase 3 规划（可观测性与审计）

### 核心内容
- ✅ 统一日志系统（audit_logger.py）
- ✅ 完整MySQL表结构（schema.sql）
- ✅ 违规操作日志（violation.log）
- ✅ 前端监控大屏
- ✅ 责任链模式（日志处理）- **必须**
- ✅ 观察者模式（状态通知）- **必须**

### 设计模式应用
| 模式 | 应用场景 | 必须性 |
|------|----------|--------|
| 责任链 | 日志处理链路 | **必须** |
| 观察者 | 状态变化通知 | **必须** |
| 策略模式 | 阵营世界观 | Phase 4 |

### 预计工作量
- Sprint 1: Python日志系统（10h）
- Sprint 2: Java责任链重构（12h）
- Sprint 3: MySQL表结构（10h）
- Sprint 4: 观察者模式重构（12h）
- Sprint 5: 前端监控大屏（12h）
- **总计**: 58h

---

## 五、架构升级设计模式（备忘录）

### 5.1 责任链模式（Chain of Responsibility）

**目标**：拆分executeTick()方法

**当前问题**：
```java
// executeTick()承担6个职责，300+行代码
public void executeTick() {
    被动消耗
    秩序之剑检查
    复活处理
    和平结局检查
    Python调度
    状态保存
}
```

**目标架构**：
```java
public interface TickHandler {
    void handle(GameState state, List<Agent> agents);
}

public class TickChain {
    private List<TickHandler> handlers = List.of(
        new PassiveConsumeHandler(),
        new OrderSwordHandler(),
        new RespawnHandler(),
        new PeaceEndingHandler(),
        new PythonDispatchHandler(),
        new StateSaveHandler()
    );
}
```

**预计阶段**：Phase 3

---

### 5.2 策略模式（Strategy）- 阵营世界观和动作偏好

**应用场景**：不同阵营不同的世界观描述和动作偏好

**注意**：Buff和惩罚是统一的（游戏规则），但世界观和AI决策风格可以用策略

**目标架构**：
```java
// 阵营世界观策略
public interface WorldviewStrategy {
    String getSystemPrompt();  // 给AI的系统提示
    String getFactionDescription();  // 阵营描述
}

public class LawfulWorldview implements WorldviewStrategy {
    @Override
    public String getSystemPrompt() {
        return "你是守序阵营的Agent，代表朝廷建立秩序...";
    }
}

public class AggressiveWorldview implements WorldviewStrategy {
    @Override
    public String getSystemPrompt() {
        return "你是激进阵营的Agent，挑战旧秩序...";
    }
}

public class NeutralWorldview implements WorldviewStrategy {
    @Override
    public String getSystemPrompt() {
        return "你是中立阵营的Agent，游走各方...";
    }
}

// 工厂创建
public class WorldviewFactory {
    public static WorldviewStrategy getStrategy(String faction) {
        return switch (faction) {
            case "lawful" -> new LawfulWorldview();
            case "aggressive" -> new AggressiveWorldview();
            default -> new NeutralWorldview();
        };
    }
}
```

**应用位置**：Python Agent调度器，根据Agent阵营选择对应的WorldviewStrategy

**预计阶段**：Phase 3

---

### 5.3 观察者模式（Observer）

**目标**：状态变化通知解耦

**当前问题**：
```java
// 状态变化时，硬编码通知多个服务
webSocket.push(state);
log.info(state);
metrics.record(state);
```

**目标架构**：
```java
public interface GameObserver {
    void onTickComplete(GameState state);
}

public class WebSocketObserver implements GameObserver { }
public class LogObserver implements GameObserver { }
public class MetricsObserver implements GameObserver { }

public class GameNotifier {
    private List<GameObserver> observers;
    public void notify(GameState state) {
        for (observer : observers) {
            observer.onTickComplete(state);
        }
    }
}
```

**预计阶段**：Phase 3

---

### 5.4 死信队列（Dead Letter Queue）

**目标**：处理失败的API请求

**问题场景**：
```
请求 → API失败 → 重试3次 → 仍然失败 → 怎么办？
```

**目标架构**：
```python
class DeadLetterQueue:
    def add(self, request):
        # 存入队列
        # 发送告警通知管理员

    def retry_later(self):
        # 定时重试失败的请求
```

**预计阶段**：Phase 2 或 Phase 3

---

## 六、记忆系统升级（备忘录）

### 6.1 记忆类型扩展

**当前实现**：
- 常规记忆（16回合）
- 重要记忆（永久，可修改）
- 秩序之剑详情（永久）

**待实现**：
- 盟友/敌对关系自动识别
- Agent自主判断重要记忆
- 记忆置信度（模糊记忆 → 确认记忆）
- 记忆过期机制（旧的盟友信息可能过时）

### 6.2 Agent自主记忆管理

**目标**：Agent能自己判断什么重要

**设计思路**：
```python
# Agent的决策输出
{
    "action": "move",
    "target": "center",
    "reasoning": "我观察到X是盟友，应该去帮他",
    "important_memory": "X是盟友（确认）",
    "update_memory": "X之前是中立，现在可能变成敌人"
}
```

**预计阶段**：Phase 4

---

## 七、设计思想笔记

### 7.1 核心设计原则

| 原则 | 说明 | 例子 |
|------|------|------|
| 统一规则 | Buff和惩罚是游戏规则，所有人平等 | 所有阵营统一惩罚 |
| 差异化定位 | 世界观和AI决策风格可以差异化 | 守序=朝廷，激进=反贼 |
| 解耦思维 | 不是为了模式而模式，真的需要才用 | 责任链/观察者/策略 |
| 架构优先 | 先设计后实现，不要堆代码 | 画架构图再写代码 |

### 7.2 设计模式使用原则

```
不是所有场景都要用设计模式！

适合用模式的场景：
- 真的需要解耦（多个地方要调用）
- 真的需要扩展（未来可能新增实现）
- 真的需要灵活替换（不同策略切换）

不适合用模式的场景：
- 只有一处调用
- 永远不会有变化
- 为了"好看"而用
```

### 7.3 架构设计流程

```
1. 理解需求
   ↓
2. 画架构图（不用写代码）
   ↓
3. 识别变化点（什么会变？）
   ↓
4. 选择合适的模式（真的合适才用）
   ↓
5. 编写代码
   ↓
6. 验证设计
```

### 7.4 阵营世界观设计方向

| 阵营 | 定位 | AI世界观 |
|------|------|----------|
| 守序 | 朝廷 | 建立秩序，维护正统 |
| 中立 | 江湖 | 游走各方，待价而沽 |
| 激进 | 反贼 | 挑战秩序，争霸天下 |

**策略模式应用**：为每个阵营设计不同的SystemPrompt，影响AI决策风格

### 7.3 异步思维

```
同步：等一个完成再做下一个
异步：同时做多个，最后汇总结果

asyncio.gather = 同时发起15个请求，1秒完成
time.sleep(1) = 串行执行15秒
```

---

*文档版本：1.2*
*创建日期：2026-04-16*
*最后更新：2026-04-23*
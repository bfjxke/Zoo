# Phase 4 资源博弈与AI判官系统 - 变更日志

> 本文档记录Phase 4开发过程中每一轮的功能实现和规则变更。

---

## 第一轮：基础数据模型扩展

### 1.1 Agent模型扩展

**新增字段**：
- `carriedFood` (int, 默认0) - Agent携带的食物数量
- `confinementTicks` (int, 默认0) - 禁闭剩余轮数
- `confinementReason` (String) - 禁闭原因

**新增方法**：
- `isConfined()` - 判断是否处于禁闭状态

### 1.2 GameState模型扩展

**新增字段**：
- `foodInventory` (Map<String, Integer>) - 阵营食物库存 {"lawful": 20, "aggressive": 20, "neutral": 20}
- `foodDropLocations` (Map<String, Integer>) - 场景食物掉落位置 {"D": 10, "E": 15}

**新增方法**：
- `getFactionFood(faction)` - 获取某阵营库存
- `consumeFactionFood(faction, amount)` - 消耗阵营库存
- `addFactionFood(faction, amount)` - 增加阵营库存
- `dropFood(nodeId, amount)` - 在某节点掉落食物
- `pickupFood(nodeId, amount)` - 从某节点捡起食物
- `getFoodAtNode(nodeId)` - 获取某节点的食物数量

---

## 第二轮：资源系统实现

### 2.1 空投物资系统

**规则**：
- 每11轮自动投放物资（第11, 22, 33, 44, 55, 66...轮）
- 投放地点：D节点（守序广场）、E节点（激进广场）
- 每次投放：各30份食物
- 食物放置在场景中，任何Agent可捡起

**代码实现**：
```java
private void performAirdrop(GameState gameState) {
    if (gameState.getCurrentTick() > 0 && gameState.getCurrentTick() % 11 == 0) {
        gameState.dropFood("D", 30);
        gameState.dropFood("E", 30);
        log.info("[空投] 第{}轮空投物资：D节点+30份，E节点+30份", gameState.getCurrentTick());
    }
}
```

### 2.2 营地库存初始化

**规则**：
- 游戏重置时，每阵营基地初始20份食物
- 各阵营库存独立，不可共享，不可转移

**代码实现**：
```java
gs.setFoodInventory(Map.of(
    "lawful", 20,
    "aggressive", 20,
    "neutral", 20
));
```

### 2.3 进食消耗库存逻辑

**规则**：
- Agent吃食物时，先吃携带的，再吃营地库存
- 营地库存为0时，进食失败
- 每份食物恢复40点饱食度

**代码实现**：
```java
// 先吃携带的食物
if (agent.getCarriedFood() > 0) {
    agent.setCarriedFood(agent.getCarriedFood() - 1);
    // 饱食度+40
}
// 没有携带食物，尝试从营地库存吃
if (gs.consumeFactionFood(faction, 1)) {
    // 饱食度+40
}
```

---

## 第三轮：食物携带系统

### 3.1 新增动作

**claim_food** - 在阵营基地领取食物
- 条件：在自己阵营基地
- 限制：携带量<20，营地库存>0
- 效果：从营地库存取1份，Agent携带量+1

**pickup_food** - 捡起场景食物
- 条件：当前位置有食物掉落
- 限制：携带量<20
- 效果：从场景取1份，Agent携带量+1

### 3.2 常量定义

```java
public static final int MAX_CARRIED_FOOD = 20;
public static final int SATIETY_EAT_RECOVER = 40;
```

### 3.3 死亡食物掉落

**规则**：
- Agent死亡时，携带的食物掉落在当前位置
- 任何人可捡起

---

## 第四轮：偷窃与禁闭系统

### 4.1 偷窃机制

**steal动作** - 偷取对方营地食物
- 条件：在对方营地（基地节点）
- 成功条件：基地内无另外两个阵营的人
- 偷窃上限：每次最多3份
- 成功率：满足条件时100%成功

### 4.2 偷窃被抓

**被抓条件**：基地内有其他阵营的人

**被抓后果**：
- 关禁闭3轮
- 偷窃失败（食物不减少）

### 4.3 禁闭系统

**触发条件**：
- 偷窃被抓
- AI判官拒绝

**禁闭效果**：
- 不能执行任何动作
- 属性保持不变
- 每轮禁闭时长-1
- 禁闭结束恢复正常

### 4.4 常量定义

```java
public static final int STEAL_AMOUNT = 3;
public static final int STEAL_CATCH_CONFINEMENT = 3;
```

---

## 第五轮：秩序之剑掉落机制

### 5.1 死亡掉落

**规则**：
- 持有秩序之剑的Agent死亡时，剑掉落在当前位置
- 任何人可捡起

### 5.2 代码实现

```java
if (gs.getOrderSwordHolderId() != null && gs.getOrderSwordHolderId().equals(agent.getId())) {
    gs.setOrderSwordLocation(currentNode);
    gs.setOrderSwordHolderId(null);
    log.info("[秩序之剑] {} 死亡，剑掉落在{}", agent.getName(), currentNode);
}
```

---

## 第六轮：AI判官系统

### 6.1 Python端 - judge_service.py

**功能**：
- 接收Agent的自定义动作请求
- 基于当前情况判断是否合理
- 约60%通过率
- 8轮缓存机制（相同行为不重复判断）

**输出格式**：
```json
{
  "agent_id": 1,
  "agent_name": "Agent1",
  "action": "attack",
  "approved": true,
  "confinement_ticks": 0,
  "reasoning": "动作合理，允许执行",
  "timestamp": "2026-05-06T12:00:00"
}
```

### 6.2 Java端 - JudgeService.java

**功能**：
- 封装调用Python判官API
- 实现8轮缓存机制
- 默认降级处理

---

## 第七轮：游戏规则更新

### 7.1 最大回合数调整

**原值**：50回合
**新值**：60回合

**理由**：
- 增加游戏时长
- 让资源博弈更充分
- 给各阵营更多对抗机会

### 7.2 进食恢复调整

**原值**：30点饱食度/份
**新值**：40点饱食度/份

**理由**：
- 空投物资每轮30份
- 每Agent需要约3份食物吃饱（40×3=120）
- 合理配置食物恢复量

---

## 资源系统汇总

### 食物相关常量

| 常量 | 值 | 说明 |
|------|-----|------|
| SATIETY_EAT_RECOVER | 40 | 每份食物恢复40点饱食 |
| MAX_CARRIED_FOOD | 20 | 携带上限20份 |
| STEAL_AMOUNT | 3 | 每次偷窃最多3份 |
| STEAL_CATCH_CONFINEMENT | 3 | 偷窃被抓关禁闭3轮 |
| AIRDROP_INTERVAL | 11 | 每11轮空投一次 |
| AIRDROP_AMOUNT | 30 | 每次空投30份 |
| INITIAL_FACTION_FOOD | 20 | 每阵营初始20份 |

### 新增动作白名单

```
move, eat, rest, talk, trade, provoke, claim_food, pickup_food, steal
```

### 进食优先级

1. 先吃携带的食物（carriedFood > 0）
2. 再从营地库存吃（从自己阵营库存扣）
3. 库存为空则进食失败

---

## 实现文件清单

### 后端（Java）

| 文件 | 变更 |
|------|------|
| Agent.java | +carriedFood, +confinementTicks, +confinementReason |
| GameState.java | +foodInventory, +foodDropLocations, +相关方法 |
| GameConstants.java | +MAX_CARRIED_FOOD, +STEAL_AMOUNT, +STEAL_CATCH_CONFINEMENT, SATIETY_EAT_RECOVER=40, MAX_GAME_TICKS=60 |
| SandboxStateMachine.java | +performAirdrop(), +禁闭处理, +食物掉落, +秩序之剑掉落 |
| RuleEngine.java | +executeClaimFood(), +executePickupFood(), +executeSteal(), *executeEat() |
| JudgeService.java | 新增 - AI判官封装 |

### 前端（Python）

| 文件 | 变更 |
|------|------|
| agent/services/judge_service.py | 新增 - AI判官服务 |
| agent/routers/judge.py | 新增 - AI判官路由 |
| agent/main.py | +judge路由 |

---

*文档版本：v1.0*
*创建日期：2026-05-06*
*最后更新：2026-05-06*
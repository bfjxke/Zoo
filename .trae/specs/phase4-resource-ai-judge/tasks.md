# Tasks - Phase 4 资源博弈与AI判官系统

## 阶段1: 基础数据模型扩展

- [x] Task 1.1: 扩展Agent模型 - 添加食物携带字段(携带量)、禁闭状态字段
  - [x] SubTask 1.1.1: Agent.java添加carriedFood字段(int, 默认0)
  - [x] SubTask 1.1.2: Agent.java添加confinementTicks字段(int, 默认0)
  - [x] SubTask 1.1.3: Agent.java添加confinementReason字段(String)

- [x] Task 1.2: 扩展GameState模型 - 添加营地库存
  - [x] SubTask 1.2.1: GameState.java添加foodInventory字段(Map<String, Integer>)
  - [x] SubTask 1.2.2: 添加getFactionFoodInventory(faction)方法
  - [x] SubTask 1.2.3: 添加consumeFactionFood(faction, amount)方法

- [x] Task 1.3: 扩展GameState模型 - 添加食物掉落点
  - [x] SubTask 1.3.1: GameState.java添加foodDropLocations字段(Map<String, Integer>)
  - [x] SubTask 1.3.2: 添加食物掉落/捡起方法

## 阶段2: 资源系统实现

- [x] Task 2.1: 实现空投物资系统
  - [x] SubTask 2.1.1: SimulationScheduler添加空投检查逻辑
  - [x] SubTask 2.1.2: 每11轮在D、E节点各投放30份食物
  - [x] SubTask 2.1.3: 食物添加到GameState的resourcesMap

- [x] Task 2.2: 实现营地库存初始化
  - [x] SubTask 2.2.1: resetGameState时初始化每阵营库存为20份
  - [x] SubTask 2.2.2: 添加API获取各阵营库存状态

- [x] Task 2.3: 实现进食消耗库存逻辑
  - [x] SubTask 2.3.1: RuleEngine.executeEat修改为从库存扣食物
  - [x] SubTask 2.3.2: 库存为0时进食失败

- [x] Task 2.4: 实现食物携带系统
  - [x] SubTask 2.4.1: 添加领取食物动作(claim_food)
  - [x] SubTask 2.4.2: 添加吃携带食物逻辑
  - [x] SubTask 2.4.3: 死亡时食物掉落

- [x] Task 2.5: 实现捡起食物动作
  - [x] SubTask 2.5.1: 添加捡起食物动作(pickup_food)
  - [x] SubTask 2.5.2: 检查当前位置是否有食物掉落

## 阶段3: 偷窃与禁闭系统

- [x] Task 3.1: 实现偷窃动作
  - [x] SubTask 3.1.1: 添加steal动作到白名单
  - [x] SubTask 3.1.2: 实现偷窃检查逻辑(进入敌方基地且无第三方)
  - [x] SubTask 3.1.3: 实现偷窃执行(从敌方库存转移食物)

- [x] Task 3.2: 实现偷窃被抓判定
  - [x] SubTask 3.2.1: 检查基地是否有其他阵营Agent
  - [x] SubTask 3.2.2: 被抓时触发禁闭3轮

- [x] Task 3.3: 实现禁闭系统
  - [x] SubTask 3.3.1: SandboxStateMachine.executeTick检查禁闭状态
  - [x] SubTask 3.3.2: 禁闭状态下跳过所有动作执行
  - [x] SubTask 3.3.3: 禁闭期间属性不变
  - [x] SubTask 3.3.4: 禁闭倒计时结束恢复正常

## 阶段4: AI判官系统

- [x] Task 4.1: Python端AI判官服务
  - [x] SubTask 4.1.1: 创建agent/services/judge_service.py
  - [x] SubTask 4.1.2: 实现MiniMax API调用逻辑
  - [x] SubTask 4.1.3: 实现约60%通过率逻辑
  - [x] SubTask 4.1.4: 实现8轮缓存机制

- [x] Task 4.2: Java端调用AI判官
  - [x] SubTask 4.2.1: 添加调用Python判官服务的API
  - [x] SubTask 4.2.2: RuleEngine处理custom动作时调用判官
  - [x] SubTask 4.2.3: 处理判官返回结果(批准/拒绝/禁闭)

- [x] Task 4.3: AI判官工具类
  - [x] SubTask 4.3.1: 创建JudgeService.java封装调用逻辑
  - [x] SubTask 4.3.2: 实现判官结果缓存

## 阶段5: 秩序之剑掉落机制

- [x] Task 5.1: 实现剑掉落逻辑
  - [x] SubTask 5.1.1: 修改死亡/被抓逻辑，剑掉落在当前位置
  - [x] SubTask 5.1.2: 实现捡剑逻辑(移动到剑位置即可捡起)

## 阶段6: 游戏规则更新

- [x] Task 6.1: 更新最大回合数
  - [x] SubTask 6.1.1: MAX_GAME_TICKS从50改为60
  - [x] SubTask 6.1.2: 更新规则文档

- [x] Task 6.2: 更新规则文档
  - [x] SubTask 6.2.1: 编写详细的资源系统规则
  - [x] SubTask 6.2.2: 编写AI判官规则
  - [x] SubTask 6.2.3: 编写禁闭系统规则

## 阶段7: 前端集成与测试

- [x] Task 7.1: 前端显示营地库存
  - [x] SubTask 7.1.1: 添加库存显示组件
  - [x] SubTask 7.1.2: 实时显示各阵营库存

- [x] Task 7.2: 前端显示Agent携带食物
  - [x] SubTask 7.2.1: Agent气泡显示携带量
  - [x] SubTask 7.2.2: 显示禁闭状态

- [x] Task 7.3: 全流程测试 ✅ 编译通过

## Task Dependencies
✅ 所有任务已完成

## 实施顺序
✅ 已按顺序完成所有任务
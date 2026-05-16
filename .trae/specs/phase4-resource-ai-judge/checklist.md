# Checklist - Phase 4 资源博弈与AI判官系统

## 数据模型扩展
- [ ] Agent.java添加carriedFood字段(int, 默认0)
- [ ] Agent.java添加confinementTicks字段(int, 默认0)
- [ ] Agent.java添加confinementReason字段(String)
- [ ] GameState.java添加foodInventory字段(Map<String, Integer>)
- [ ] GameState.java添加foodDropLocations字段(Map<String, Integer>)
- [ ] 数据库schema添加相应字段

## 资源系统实现
- [ ] 空投物资逻辑：每11轮D、E节点各投放10份
- [ ] 营地库存初始化：每阵营基地20份
- [ ] 进食消耗库存：库存为0时进食失败
- [ ] 领取食物动作(claim_food)
- [ ] 吃携带食物逻辑
- [ ] 死亡时食物掉落
- [ ] 捡起食物动作(pickup_food)

## 偷窃与禁闭系统
- [ ] steal动作添加到白名单
- [ ] 偷窃成功条件检查
- [ ] 偷窃被抓判定
- [ ] 禁闭状态检查
- [ ] 禁闭期间跳过动作
- [ ] 禁闭倒计时递减

## AI判官系统
- [ ] Python judge_service.py创建
- [ ] MiniMax API调用实现
- [ ] 约60%通过率逻辑
- [ ] 8轮缓存机制
- [ ] Java端JudgeService封装
- [ ] RuleEngine调用AI判官
- [ ] 判官结果处理(批准/拒绝/禁闭)

## 秩序之剑掉落
- [ ] 死亡/被抓时剑掉落
- [ ] 移动到剑位置可捡起

## 游戏规则更新
- [ ] MAX_GAME_TICKS改为60
- [ ] 规则文档更新

## 前端集成
- [ ] 营地库存显示
- [ ] Agent携带食物显示
- [ ] 禁闭状态显示

## 全流程测试
- [ ] 空投物资测试
- [ ] 进食消耗库存测试
- [ ] 食物携带与掉落测试
- [ ] 偷窃机制测试
- [ ] 禁闭系统测试
- [ ] AI判官测试
- [ ] 秩序之剑掉落测试
- [ ] 60轮平局测试
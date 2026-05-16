# Phase 4 资源博弈与AI判官系统 - 规格说明

## Why
当前游戏缺乏资源博弈机制，阵营间无有效对抗手段，需要实现完整的资源系统、AI判官系统和阵营交互机制。

## What Changes

### 一、资源博弈系统

#### 1.1 空投物资
- **频率**: 每11轮空投一次（从第1轮开始）
- **地点**: D节点（守序广场）、E节点（激进广场）
- **数量**: 每处10份
- **规则**: 到达广场的Agent均可采集，采集后放入营地库存

#### 1.2 营地食物库存
- **初始化**: 每阵营基地初始20份
- **消耗**: Agent吃食物时从库存扣除
- **不可共享**: 各阵营库存独立
- **不可转移**: 不能跨阵营转移食物

#### 1.3 食物携带系统
- **携带上限**: 20份
- **获取方式**: 在营地领取
- **效果**: 与在营地吃效果相同
- **死亡掉落**: 死亡时食物掉落在原地，任何人可捡

#### 1.4 偷窃机制
- **触发**: 进入对方阵营基地，基地无另外两阵营的人
- **每次上限**: 3份
- **成功率**: 必定成功（满足条件时）
- **被抓条件**: 被对方阵营的人看到
- **被抓后果**: 关禁闭3轮，偷窃失败

### 二、禁闭系统
- **触发**: AI判官拒绝 / 偷窃被抓
- **期间效果**: 不能做任何动作，属性固定
- **时长**: AI判官决定（1-5轮）或偷窃被抓（3轮）
- **特殊**: 秩序之剑持有者被禁闭时，剑不掉落（除非被抓是偷窃）

### 三、AI判官系统
- **触发**: Agent执行自定义动作时
- **输入**: 当前游戏状态 + 最近游戏日志
- **通过率**: 约60%
- **缓存**: 相同行为8轮内不重复判断
- **输出**: 
  ```json
  {
    "approved": true/false,
    "confinement_ticks": 0-5,
    "reasoning": "判断理由"
  }
  ```

### 四、秩序之剑机制更新
- **持有者被抓/死亡**: 剑原地掉落，任何人可捡

### 五、游戏结束条件
- 最大回合数调整为60轮
- 60轮后无胜负触发"永无止境"结局（平局）

## Impact
- Affected specs: AI判官、自定义动作、资源系统、阵营交互
- Affected code: RuleEngine, SandboxStateMachine, Agent, GameState, SimulationScheduler, Frontend

## ADDED Requirements

### Requirement: 空投物资系统
系统每11轮自动在D、E节点投放食物

#### Scenario: 空投触发
- **WHEN** 当前Tick是11的倍数
- **THEN** 在D、E节点各增加10份食物资源

### Requirement: 营地库存系统
Agent吃食物时从营地库存扣除

#### Scenario: Agent进食
- **WHEN** Agent执行eat动作
- **THEN** 从其阵营营地库存扣除1份食物
- **AND** Agent饱食度+30（上限140）
- **IF** 营地库存为0，**THEN** 进食失败

### Requirement: 食物携带系统
Agent可携带最多20份食物

#### Scenario: 领取食物
- **WHEN** Agent在自己阵营基地执行领取动作
- **AND** 营地库存>=1
- **AND** Agent携带量<20
- **THEN** 从库存取1份，Agent携带量+1

#### Scenario: 吃携带食物
- **WHEN** Agent执行eat动作且无营地库存
- **AND** Agent携带量>=1
- **THEN** Agent携带量-1，饱食度+30

#### Scenario: 死亡掉落
- **WHEN** Agent死亡
- **THEN** 其携带的食物掉落在当前位置
- **AND** 任何Agent可捡起

### Requirement: 偷窃系统
Agent可偷取对方阵营营地食物

#### Scenario: 偷窃成功
- **WHEN** Agent进入对方阵营基地
- **AND** 基地内无另外两阵营Agent
- **AND** 对方营地库存>0
- **THEN** 偷取1-3份（最多偷3份或对方库存数量）

#### Scenario: 偷窃被抓
- **WHEN** Agent偷窃时被发现
- **THEN** 进入禁闭状态3轮
- **AND** 偷窃失败（食物不减少）

### Requirement: 禁闭系统
Agent可被禁闭

#### Scenario: 禁闭状态
- **WHEN** Agent处于禁闭状态
- **THEN** 不能执行任何动作
- **AND** 属性保持不变
- **AND** 每轮禁闭时长-1

#### Scenario: 禁闭结束
- **WHEN** 禁闭时长<=0
- **THEN** 恢复正常状态

### Requirement: AI判官系统
裁决Agent的自定义动作请求

#### Scenario: 判官批准
- **WHEN** Agent提交自定义动作
- **AND** AI判官判定合理（60%概率）
- **THEN** 返回approved=true
- **AND** 执行该动作

#### Scenario: 判官拒绝
- **WHEN** Agent提交自定义动作
- **AND** AI判官判定不合理
- **THEN** 返回approved=false
- **AND** 可能的禁闭惩罚

#### Scenario: 判官缓存
- **WHEN** Agent提交之前判官已裁决过的动作
- **AND** 距上次裁决不超过8轮
- **THEN** 直接返回上次结果

### Requirement: 秩序之剑掉落
- **WHEN** 持有秩序之剑的Agent被抓（偷窃）或死亡
- **AND** 原因不是主动放弃
- **THEN** 剑掉落在当前节点
- **AND** 任何Agent可捡起

## MODIFIED Requirements

### Requirement: 最大回合数
**原值**: 50
**新值**: 60

### Requirement: 进食规则
**修改**: 进食时必须从营地库存扣除1份
**原值**: 直接增加饱食度
**新值**: 先扣库存，库存为0则进食失败

## REMOVED Requirements

### Requirement: 旧空投逻辑
**Reason**: 原有定时空投逻辑不符合设计要求
**Migration**: 使用新的空投系统
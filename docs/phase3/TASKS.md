# Phase 3 任务分解

> GuardianEye-IIoT 沙箱动物园 - 可观测性与审计
>
> **版本**：1.0
> **日期**：2026-04-23

---

## Sprint 1：Python日志系统

### 1.1 audit_logger.py

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] 创建AuditLogger类 | P0 | 2h | 基础日志格式 |
| [x] 实现控制台输出 | P0 | 1h | StreamHandler |
| [x] 实现文件输出 | P0 | 1h | FileHandler |
| [x] 实现按天分割 | P0 | 1h | TimedRotatingFileHandler |
| [x] 实现违规日志分离 | P1 | 1h | violation.log |
| [x] 添加统一格式化 | P0 | 1h | 格式化器 |

### 1.2 集成到调度器

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] 在decide接口集成日志 | P0 | 2h | 请求时记录 |
| [x] 在decide接口集成违规 | P1 | 1h | 违规单独记录 |
| [x] 日志输出测试 | P0 | 1h | 验证格式 |

---

## Sprint 2：Java责任链模式

### 2.1 责任链模式重构

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] 定义LogHandler接口 | P0 | 1h | handler方法 |
| [x] 实现ConsoleHandler | P0 | 1h | 控制台输出 |
| [x] 实现FileHandler | P0 | 1h | 文件输出 |
| [x] 实现ViolationHandler | P1 | 1h | 违规分离 |
| [x] 实现JsonHandler | P2 | 1h | JSON格式（可选） |
| [x] 链式组装测试 | P0 | 2h | 验证责任链 |

### 2.2 日志格式

```java
// 统一格式
"[Tick #%d][%s][%s][%s] -> %s | %s[%.2f]"

// 示例
"[Tick #15][张三][lawful][move] -> center | [JUDGE_PENDING][0.6]"
```

---

## Sprint 3：MySQL表结构

### 3.1 schema.sql

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] agents表 | P0 | 0.5h | 已有，可优化索引 |
| [x] agent_states表 | P0 | 1h | 历史快照 |
| [x] action_logs表 | P0 | 1h | 动作记录 |
| [x] social_records表 | P1 | 1h | 聊天记录 |
| [x] dynamic_rules表 | P1 | 1h | AI判官规则 |
| [x] god_operations表 | P1 | 1h | 上帝干预 |
| [x] votes表 | P0 | 0.5h | 已有 |
| [x] 创建索引 | P0 | 1h | 性能优化 |

### 3.2 Repository层

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] AgentStateRepository | P0 | 1h | 历史快照 |
| [x] ActionLogRepository | P0 | 1h | 动作查询 |
| [x] SocialRecordRepository | P1 | 1h | 聊天查询 |
| [x] GodOperationRepository | P1 | 1h | 干预记录 |

---

## Sprint 4：观察者模式

### 4.1 观察者接口

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] 定义GameObserver接口 | P0 | 1h | 三个方法 |
| [x] 实现DatabaseObserver | P0 | 2h | 持久化 |
| [x] 实现WebSocketObserver | P1 | 2h | 推送 |
| [x] 实现MetricsObserver | P2 | 1h | 统计（可选） |

### 4.2 通知中心

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [x] GameNotifier类 | P0 | 1h | 通知逻辑 |
| [x] 集成到StateMachine | P0 | 2h | 执行时通知 |
| [x] 测试观察者 | P0 | 2h | 验证通知 |

---

## Sprint 5：前端监控大屏

### 5.1 基础界面

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [ ] 布局设计 | P0 | 1h | 网格布局 |
| [ ] Agent状态列表 | P0 | 2h | 实时更新 |
| [ ] 阵营统计卡片 | P0 | 1h | 三个阵营 |

### 5.2 实时功能

| 任务 | 优先级 | 预估 | 说明 |
|------|--------|------|------|
| [ ] WebSocket连接 | P0 | 2h | 实时推送 |
| [ ] 日志流组件 | P0 | 2h | 滚动日志 |
| [ ] 图表组件 | P1 | 3h | 资源统计 |

---

## 任务依赖关系

```
Sprint 1 (Python日志)
    ↓
Sprint 2 (Java责任链) ←── 可并行
Sprint 3 (MySQL表结构) ←── 可并行
    ↓
Sprint 4 (观察者模式) ←── 依赖Sprint 3
    ↓
Sprint 5 (前端大屏) ←── 依赖Sprint 4
```

---

## 预估工时汇总

| Sprint | 任务 | 预估工时 |
|--------|------|----------|
| Sprint 1 | Python日志系统 | 10h |
| Sprint 2 | Java责任链重构 | 12h |
| Sprint 3 | MySQL表结构 | 10h |
| Sprint 4 | 观察者模式重构 | 12h |
| Sprint 5 | 前端监控大屏 | 12h |
| **总计** | | **58h** |

---

## 验收标准

```
Sprint 1:
[x] 日志格式统一
[x] 控制台和文件同时输出
[x] 按天自动分割
[x] 违规日志单独记录

Sprint 2:
[x] 责任链正常工作
[x] 可添加/删除Handler
[x] 日志格式正确

Sprint 3:
[x] 所有表创建成功
[x] 索引正确
[x] 可以正常查询

Sprint 4:
[x] 观察者正常工作
[x] 数据库持久化
[x] WebSocket推送

Sprint 5:
[ ] 实时显示Agent状态
[ ] 日志流实时滚动
[ ] 阵营统计正确
```

---

*文档版本：1.0*
*最后更新：2026-04-23*

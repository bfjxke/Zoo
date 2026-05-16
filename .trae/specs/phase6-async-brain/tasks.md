# Tasks - Phase 6 异步AI大脑架构

## 阶段1: Java端异步化

- [ ] Task 1.1: 引入WebClient依赖
  - [ ] SubTask 1.1.1: 在pom.xml添加spring-boot-starter-webflux
  - [ ] SubTask 1.1.2: 保留spring-boot-starter-web保持兼容

- [ ] Task 1.2: 创建AsyncAgentBrain服务
  - [ ] SubTask 1.2.1: 创建AsyncAgentBrain.java
  - [ ] SubTask 1.2.2: 实现thinkAllAsync()并行调用
  - [ ] SubTask 1.2.3: 实现thinkAsync()单个Agent调用
  - [ ] SubTask 1.2.4: 实现fallbackDecision()降级逻辑

- [ ] Task 1.3: 创建AgentDecision模型
  - [ ] SubTask 1.3.1: 创建AgentDecision.java
  - [ ] SubTask 1.3.2: 创建AgentEvent.java事件模型

## 阶段2: Python端异步化

- [ ] Task 2.1: 重构agent_scheduler为事件驱动
  - [ ] SubTask 2.1.1: 创建agent_thinking_loop()协程
  - [ ] SubTask 2.1.2: 实现should_think()自主判断逻辑
  - [ ] SubTask 2.1.3: 实现状态变化触发思考

- [ ] Task 2.2: 创建事件通知机制
  - [ ] SubTask 2.2.1: 创建AgentNotifier类
  - [ ] SubTask 2.2.2: 实现订阅/发布模式
  - [ ] SubTask 2.2.3: 与Java WebSocket集成

## 阶段3: 集成测试

- [ ] Task 3.1: 端到端测试
  - [ ] SubTask 3.1.1: 测试异步并发调用
  - [ ] SubTask 3.1.2: 测试降级逻辑
  - [ ] SubTask 3.1.3: 测试事件通知

- [ ] Task 3.2: 性能测试
  - [ ] SubTask 3.2.1: 测试响应时间
  - [ ] SubTask 3.2.2: 测试并发度

## Task Dependencies

```
Task 1.x → Task 2.x → Task 3.x
```

## 实施顺序

1. Task 1.x → Java端异步化
2. Task 2.x → Python端异步化
3. Task 3.x → 集成测试
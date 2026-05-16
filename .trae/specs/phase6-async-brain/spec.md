# Phase 6 异步AI大脑架构 - 规格说明

## Why

当前系统存在以下问题：
1. Java端同步串行调用Python，10个Agent×2秒=20秒串行等待
2. 所有Agent被固定3秒Tick限制，无法自主决定思考时机
3. 无法实现真正的实时响应

## 目标架构

### 核心改变

| 方面 | 当前 | 目标 |
|------|------|------|
| Java调用Python | 同步串行 | 异步并行（WebClient/CompletableFuture） |
| Agent决策 | 全局Tick驱动 | Agent自主异步驱动 |
| 响应时间 | 3秒固定 | 实时响应（<1秒） |

### 技术方案

#### 1. Java端异步化（WebClient + CompletableFuture）

```java
@Service
public class AsyncAgentBrain {
    
    private final WebClient webClient;
    
    // 并行调用所有Agent的LLM
    public Flux<AgentDecision> thinkAllAsync(List<Agent> agents) {
        return Flux.fromIterable(agents)
            .flatMap(this::thinkAsync, 10)  // 并发度10
            .onErrorContinue((e, agent) -> log.error("Agent {} 决策失败", agent.getName(), e));
    }
    
    private Mono<AgentDecision> thinkAsync(Agent agent) {
        return webClient.post()
            .uri("/decide")
            .bodyValue(buildRequest(agent))
            .retrieve()
            .bodyToMono(AgentDecision.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.just(fallbackDecision(agent, e)));
    }
}
```

#### 2. Python端自主思考

```python
# 每个Agent独立的思考协程
async def agent_thinking_loop(agent_id: int, state: AgentState):
    while state.alive:
        # 自主决定思考时机（基于状态变化）
        if should_think(state):
            decision = await think(state)
            await execute(decision)
        
        # 随机等待0.5-3秒
        await asyncio.sleep(random.uniform(0.5, 3.0))

# 所有Agent并发运行
async def run_all_agents(agents):
    tasks = [agent_thinking_loop(a.id, a.state) for a in agents]
    await asyncio.gather(*tasks)
```

#### 3. 事件驱动通知

```python
# Agent状态变化时通知Java
class AgentNotifier:
    def __init__(self):
        self.subscribers = []
    
    def notify(self, event: AgentEvent):
        for sub in self.subscribers:
            sub.on_agent_event(event)
```

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Java Spring Boot                          │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ GameLoop   │───▶│ AsyncAgent  │───▶│ WebSocket  │  │
│  │ (协调器)    │    │ Brain       │    │ Push       │  │
│  └─────────────┘    └──────────────┘    └──────────────┘  │
│                            │                                │
│                     WebClient异步                         │
└────────────────────────────│────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Python FastAPI                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │  Agent1 ──▶ 思考 ──▶ 动作 ──▶ 等待 ──▶ 思考    │    │
│  │  Agent2 ──▶ 思考 ──▶ 动作 ──▶ 等待 ──▶ 思考    │    │
│  │  Agent3 ──▶ 思考 ──▶ 动作 ──▶ 等待 ──▶ 思考    │    │
│  └─────────────────────────────────────────────────┘    │
│                            │                              │
│                     asyncio.gather                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    MiniMax LLM API
```

## 影响范围

- Affected specs: Agent调度、实时响应
- Affected code:
  - `backend/.../SimulationScheduler.java`
  - `backend/.../PythonDispatcher.java`
  - `agent/.../agent_scheduler.py`

## 验收标准

1. Agent响应时间 < 2秒
2. 支持10+ Agent并发
3. 无全局Tick限制
4. 保持API兼容性
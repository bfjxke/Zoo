# GuardianEye-IIoT 沙箱动物园 - 备忘录

## Phase 5: Agent人格蒸馏（远期规划）

> 参考：CHANGELOG.md - Agent人格蒸馏：用户人格+守序派领袖

### 1. Agent人格蒸馏系统
**优先级：** 低
**预计阶段：** Phase 5
**描述：** 将用户人格特征蒸馏到守序派领袖Agent中

**功能点：**
- 用户行为模式采集
- 人格特征向量提取
- 蒸馏训练到LLM模型
- 领袖Agent人格继承

---

### 2. LangChain/LangGraph Agent架构重构

**优先级：** 中
**预计阶段：** Phase 5.5（与Agent人格蒸馏并行）
**难度：** 中等
**预估工作量：** 2-3周

**当前实现分析：**

当前Python Agent是**纯手写实现**，包含：
| 模块 | 当前实现 | 功能 |
|------|----------|------|
| 调度器 | `agent_scheduler.py` | 异步并发、模型选择、限流 |
| 记忆管理 | `memory_manager.py` | 常规记忆、重要记忆、上下文构建 |
| API客户端 | `minimax_client.py` | httpx调用、Mock降级 |
| Prompt模板 | 硬编码字符串 | SYSTEM_PROMPT_LEADER/SOLDIER/JUDGE |

**优点：**
- 零依赖，轻量级
- 完全可控，定制灵活
- 学习成本低

**缺点/困难点：**
- Prompt工程分散，难以管理和版本控制
- 工具调用（Tool Calling）需要手写
- 多Agent协作逻辑复杂（当前是串行的）
- 记忆管理缺乏标准化
- 缺乏ReAct等Agent范式的原生支持
- 难以扩展到复杂的多步骤任务

**LangChain/LangGraph vs 自研对比：**

| 方面 | 当前自研 | LangChain/LangGraph |
|------|----------|---------------------|
| 依赖 | 零依赖 | 需要安装langchain等包 |
| 代码量 | ~500行 | ~200行 |
| Prompt管理 | 硬编码 | LangSmith/LangServe |
| 工具调用 | 手写 | 内置Tool Calling |
| 多Agent协作 | 复杂 | LangGraph原生支持 |
| 记忆管理 | 手写 | 内置Memory组件 |
| ReAct支持 | 需要手写 | 内置ReAct Agent |
| 调试/追踪 | print调试 | LangSmith可视化追踪 |

**推荐方案：LangGraph为主

**理由：**
- LangGraph支持**有状态、多步骤、循环**的工作流，非常适合游戏Agent场景
- 可以定义Agent节点、边、条件分支
- 支持长期记忆和短期记忆的分离
- 与LangChain生态无缝集成

**迁移路径：**

```
Phase 5.5.1: 引入LangChain基础组件
├─ 安装langchain-core, langchain-community
├─ 将Prompt模板迁移到PromptTemplate
├─ 将API调用迁移到ChatModel
└─ 保留现有业务逻辑

Phase 5.5.2: 引入LangGraph工作流
├─ 定义Agent节点（LeaderAgent, SoldierAgent, JudgeAgent）
├─ 定义边和条件分支
├─ 实现ReAct风格的推理循环
└─ 集成Memory组件

Phase 5.5.3: 工具调用增强
├─ 定义Tool（move, eat, rest, talk等）
├─ 实现Tool Calling
├─ 添加参数验证
└─ 错误处理和降级
```

**技术栈：**
```
langchain-core           # 核心抽象
langchain-community      # 工具和集成
langchain-openai         # OpenAI兼容接口（可用于MiniMax）
langgraph                # 工作流框架
langsmith                # 追踪和调试（可选）
```

**注意事项：**
- MiniMax API可能不完全兼容LangChain的ChatModel接口，需要包装适配器
- LangGraph的图结构需要重新设计，与现有游戏逻辑结合
- 性能可能略有下降（框架开销），但可维护性大幅提升

**替代方案：**
- 仅升级到LangChain（不引入LangGraph）：适合简单场景
- 保持自研，只抽取公共模块：适合不想引入大框架的场景

---

## Phase 6: 真正AI大脑架构（远期规划）

### 1. 异步事件驱动的Agent自主决策系统

**优先级：** 低
**预计阶段：** Phase 6
**依赖：** Phase 4（3D渲染）完成后
**风险：** 高（需要Spring WebFlux迁移）
**预估工作量：** 3-4周

**问题分析：**

当前系统的局限性：
```
Java SimulationScheduler (同步·串行)
    ↓ 每3秒触发
    ↓ 遍历每个Agent（串行）
    ↓ RestTemplate.postForObject() [同步阻塞]
    ↓
Python FastAPI /decide (异步·并发)
    ↓
asyncio.gather() [真正并发]
```

**实际效果**：10个Agent × 2秒LLM调用 = **20秒串行处理**

**设计目标：**

| 方面 | 当前实现 | Phase 6目标 |
|------|----------|-------------|
| **自主性** | 被调度器3秒固定节奏驱动 | 自己决定何时思考 |
| **异步性** | Java层同步串行 | 真正异步并发 |
| **独立性** | 所有Agent集中在一次LLM调用 | 每个Agent独立思考 |
| **实时性** | 等待所有Agent决策完才执行 | 决策后可立即行动 |

**技术方案：**

#### 方案A：Spring WebFlux + WebClient（推荐）
```
┌─────────────────────────────────────────────────────────────┐
│                    Spring WebFlux                           │
│  ┌─────────────────┐  ┌─────────────────┐                 │
│  │  GameLoopService │  │  AgentBrain     │                 │
│  │  (异步游戏循环)   │  │  (每个Agent独立) │                 │
│  └────────┬────────┘  └────────┬────────┘                 │
│           │                    │                           │
│           ▼                    ▼                            │
│  ┌─────────────────────────────────────────┐               │
│  │           WebClient (异步HTTP)          │               │
│  │  同时向Python服务发送多个Agent决策请求     │               │
│  └─────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

**关键代码示例：**

```java
@Service
public class AsyncAgentBrain {
    private final WebClient webClient;

    public AsyncAgentBrain(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("http://localhost:8000")
            .build();
    }

    public Mono<AgentDecision> thinkAsync(Agent agent) {
        return webClient.post()
            .uri("/decide")
            .bodyValue(buildRequest(agent))
            .retrieve()
            .bodyToMono(AgentDecision.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.just(fallbackDecision(agent, e)));
    }

    public Flux<AgentDecision> thinkAllAsync(List<Agent> agents) {
        return Flux.fromIterable(agents)
            .flatMap(this::thinkAsync, 10)  // 并发度10
            .onErrorContinue((e, agent) -> {
                log.error("Agent {} 决策失败", agent.getName(), e);
            });
    }
}
```

#### 方案B：消息队列架构（Kafka/RabbitMQ）
```
┌──────────┐    ┌──────────┐    ┌──────────┐
│   Java   │───▶│  Kafka   │───▶│  Python  │
│  Spring  │    │  (队列)   │    │  FastAPI │
└──────────┘    └──────────┘    └──────────┘
                     │
                     ▼
              ┌──────────┐
              │  决策结果  │
              │  返回队列  │
              └──────────┘
```

**实施步骤：**

1. **阶段一**：引入WebClient，替换RestTemplate（保留同步外壳）
2. **阶段二**：迁移到Spring WebFlux响应式编程
3. **阶段三**：实现每个Agent独立的思考协程
4. **阶段四**：添加消息队列作为可选部署方案

**风险缓解：**
- 渐进式迁移，避免大规模重写
- 保留同步接口作为降级方案
- 充分测试响应式编程的背压处理

---

## Phase 4: 增强功能（待实现）

### 1. WebGL渲染引擎升级
**优先级：** 中
**描述：** 将当前的SVG地图升级为WebGL渲染引擎，提升视觉效果和性能

**推荐方案：PixiJS为主 + 少量Three.js（可选）

**方案选择理由：**
| 方案 | 优点 | 缺点 | 选择 |
|------|------|------|------|
| **PixiJS为主** | 2D性能极佳，学习曲线平缓，API简洁，与SVG替换成本低 | 3D能力有限 | ✅ **推荐** |
| Three.js | 3D能力强大，生态丰富 | 学习成本高，包体积大，复杂 | 可选加少量 |
| Phaser | 游戏引擎功能全 | 太重，与当前架构差异大 | ❌ |

**功能点（按优先级）：**
- [P1] Agent玻璃态气泡显示（阵营对应不同外观）
- [P1] 平滑移动动画（插值而非瞬移）
- [P1] 粒子特效（移动、死亡）
- [P2] 阵营实力对比曲线图
- [P2] Agent状态分布饼图
- [P2] Tick时间线事件流
- [P3] 资源分布热力图
- [P3] 相机跟随/缩放
- [P3] 实时战斗特效

**技术栈：**
```
主渲染层：PixiJS v7+
├─ Sprite/Container管理节点和Agent
├─ Ticker实现平滑动画
├─ BlurFilter实现玻璃态效果
└─ Graphics绘制连接线和特效

可选增强：Three.js（仅用于复杂3D地形）
├─ 简单3D地形块（森林、河流、山地）
└─ 视差滚动效果
```

**预估工作量：** 3-4周

---

### 2. 实时数据可视化仪表盘
**优先级：** 中
**依赖：** Phase 3 Sprint 5（未完成）
**描述：** 延续Phase 3的前端监控大屏功能

**功能点：**
- 阵营实力对比曲线图
- 资源分布热力图
- Agent状态分布饼图
- Tick时间线事件流

**预估工作量：** 1-2周

---

## 已知问题

### 前端显示问题
- [ ] **浏览器Tab关闭后旧连接未清理**
  - **预计阶段：** Phase 4
  - **问题描述：** Dashboard.vue中虽有beforeUnmount钩子，但Tab关闭时可能未被触发，导致服务端连接残留
  - **修复建议：** 添加window.onbeforeunload事件监听，在页面关闭前主动断开WebSocket连接

- [ ] **建议添加连接状态重试机制**
  - **预计阶段：** Phase 4
  - **问题描述：** WebSocket断开后没有自动重连逻辑
  - **修复建议：** 实现指数退避重连机制

### Agent行为问题
- [x] ~~Agent行为同质化~~ (已修复：集成LLM API)
- [ ] **需要更多个性化的交互事件**
  - **预计阶段：** Phase 4或Phase 5
  - **问题描述：** 当前Agent行为模式较为单一，需要更多基于性格词条的个性化交互
  - **建议：** 根据PersonalityTraits实现不同的AI决策偏好

---

## 技术债务

1. ~~Agent未调用LLM API~~ (已修复)
2. **前端轮询间隔可优化（当前1秒）**
   - **预计阶段：** Phase 4
   - **当前问题：** 1秒轮询可能造成不必要的请求
   - **优化建议：** 根据游戏状态动态调整轮询间隔（运行中1秒，暂停时可延长）

3. **日志数据未持久化到前端可视化**
   - **预计阶段：** Phase 4
   - **当前问题：** Phase 3的Sprint 5（前端监控大屏）未完成
   - **关联功能：** 见上方"实时数据可视化仪表盘"

---

## Phase 3 Sprint 5 待完成

### 前端监控大屏
**预计阶段：** Phase 4（延续Phase 3）
**优先级：** 中
**状态：** ⏸️ 未完成

**已完成：**
- ✅ 后端观察者模式实现
- ✅ 后端日志责任链实现

**待完成：**
- [ ] WebSocket推送集成
- [ ] Agent状态实时列表
- [ ] 阵营统计卡片
- [ ] 日志流实时滚动组件

---

*最后更新：2026-05-06*
*更新内容：*
- *Phase 4渲染方案改为PixiJS为主 + 可选Three.js*
- *所有待实现/待修复问题添加预计阶段*
- *Phase 3 Sprint 5补充到Phase 4中*
- *Phase 5.5新增LangChain/LangGraph Agent架构重构*

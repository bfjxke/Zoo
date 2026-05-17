# GuardianEye-IIoT 沙箱动物园 - 项目结构文档

> 本文档描述项目的目录结构和各个模块的职责。
>
> **版本历史**：
> - v1.0: 初始版本
> - v1.2: Phase 4 资源博弈系统
> - **v2.1: 当前版本** - 设计模式重构完成

---

## 一、项目结构总览

```
g:\project\zoo/
├── backend/                    # Java Spring Boot 后端
├── frontend/                   # Vue 3 前端
├── agent/                      # Python FastAPI AI Agent服务
├── docs/                       # 项目文档
│   ├── core/                  # 核心文档
│   ├── roadmap/              # 路线图
│   └── changelog/             # 变更日志
└── init_db.sql                # 数据库初始化脚本
```

---

## 二、后端结构 (backend/)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/guardianeye/iiot/
│   │   │   ├── controller/          # 控制层
│   │   │   │   ├── SandboxController.java
│   │   │   │   └── GodController.java
│   │   │   │
│   │   │   ├── service/            # 服务层
│   │   │   │   ├── SandboxStateMachine.java    # 状态机
│   │   │   │   ├── RuleEngine.java           # 规则引擎
│   │   │   │   ├── GodModeService.java      # 上帝模式
│   │   │   │   ├── SimulationScheduler.java   # 模拟调度器
│   │   │   │   ├── PersonalityService.java  # 性格服务
│   │   │   │   ├── ActionResult.java         # 动作结果
│   │   │   │   │
│   │   │   │   ├── action/            # 动作策略 (策略模式)
│   │   │   │   │   ├── ActionStrategy.java          # 策略接口
│   │   │   │   │   ├── ActionStrategyFactory.java   # 策略工厂
│   │   │   │   │   ├── MoveActionStrategy.java
│   │   │   │   │   ├── EatActionStrategy.java
│   │   │   │   │   ├── RestActionStrategy.java
│   │   │   │   │   ├── TalkActionStrategy.java
│   │   │   │   │   ├── TradeActionStrategy.java
│   │   │   │   │   ├── ProvokeActionStrategy.java
│   │   │   │   │   ├── ClaimFoodActionStrategy.java
│   │   │   │   │   ├── PickupFoodActionStrategy.java
│   │   │   │   │   └── StealActionStrategy.java
│   │   │   │   │
│   │   │   │   └── tick/             # Tick阶段 (责任链模式)
│   │   │   │       ├── TickPhase.java                # 阶段接口
│   │   │   │       ├── PassiveConsumptionPhase.java   # 被动消耗
│   │   │   │       ├── OrderSwordSpawnPhase.java    # 秩序之剑生成
│   │   │   │       ├── RespawnPhase.java           # 复活
│   │   │   │       ├── PeaceEndingPhase.java       # 和平结局
│   │   │   │       └── AirdropPhase.java          # 空投
│   │   │   │
│   │   │   ├── model/               # 实体层
│   │   │   │   ├── Agent.java              # Agent实体
│   │   │   │   ├── GameState.java            # 游戏状态
│   │   │   │   ├── GameConstants.java        # 游戏常量
│   │   │   │   ├── ActionLog.java           # 动作日志
│   │   │   │   ├── Vote.java                # 投票
│   │   │   │   ├── PersonalityTraits.java   # 性格特质
│   │   │   │   ├── AgentStatus.java        # Agent状态
│   │   │   │   ├── NodeType.java           # 节点类型
│   │   │   │   ├── LeaderValue.java       # 领袖值
│   │   │   │   ├── GameGraph.java         # 游戏图结构
│   │   │   │   ├── GraphNode.java         # 图节点
│   │   │   │   ├── GraphEdge.java         # 图边
│   │   │   │   │
│   │   │   │   └── repository/          # JPA仓库
│   │   │   │       ├── AgentRepository.java
│   │   │   │       ├── GameStateRepository.java
│   │   │   │       ├── ActionLogRepository.java
│   │   │   │       └── VoteRepository.java
│   │   │   │
│   │   │   ├── observer/             # 观察者模式
│   │   │   │   ├── GameObserver.java       # 观察者接口
│   │   │   │   ├── WebSocketObserver.java # WebSocket推送
│   │   │   │   └── DatabaseObserver.java  # 数据库观察
│   │   │   │
│   │   │   ├── logger/               # 日志系统
│   │   │   │   ├── AuditLogger.java      # 审计日志
│   │   │   │   ├── LogEntry.java        # 日志条目
│   │   │   │   ├── LogHandler.java      # 日志处理器
│   │   │   │   ├── ViolationHandler.java # 违规处理
│   │   │   │   ├── ConsoleHandler.java   # 控制台输出
│   │   │   │   └── FileHandler.java     # 文件输出
│   │   │   │
│   │   │   ├── config/               # 配置
│   │   │   │   ├── CorsConfig.java      # CORS配置
│   │   │   │   └── WebSocketConfig.java # WebSocket配置
│   │   │   │
│   │   │   └── GuardianEyeIiotApplication.java  # 启动类
│   │   │
│   │   └── resources/
│   │       └── application.yml        # Spring配置
│   │
│   └── test/                        # 测试
│       └── java/com/guardianeye/iiot/
│           └── SandboxSimulationTest.java  # 单元测试
│
├── pom.xml                          # Maven配置
└── target/                          # 编译输出
```

---

## 三、Agent服务结构 (agent/)

```
agent/
├── main.py                          # FastAPI 入口
├── requirements.txt                 # 基础依赖
├── requirements-langchain.txt        # LangChain依赖
├── .env.example                    # 环境变量示例
│
├── routers/                        # API路由
│   ├── __init__.py
│   ├── decide.py                  # Agent决策接口
│   ├── judge.py                   # AI判官接口
│   └── health.py                  # 健康检查接口
│
├── services/                       # 服务层
│   ├── __init__.py
│   ├── agent_scheduler.py         # Agent调度
│   ├── minimax_client.py         # MiniMax API客户端
│   ├── rate_limiter.py           # 限流器
│   ├── memory_manager.py          # 记忆管理
│   ├── audit_logger.py           # 审计日志
│   ├── async_agent_loop.py        # 异步Agent循环
│   └── judge_service.py           # 判官服务
│
├── graphs/                         # LangGraph 图
│   ├── __init__.py
│   ├── leader_graph.py           # 领袖决策图
│   ├── soldier_graph.py          # 士兵决策图
│   ├── judge_graph.py            # 判官决策图
│   └── shared_state.py           # 共享状态
│
├── prompts/                       # 提示词
│   ├── __init__.py
│   ├── leader_prompt.py          # 领袖提示词
│   ├── soldier_prompt.py         # 士兵提示词
│   └── judge_prompt.py           # 判官提示词
│
├── models/                        # 数据模型
│   ├── __init__.py
│   └── schemas.py                # Pydantic模型
│
├── tools/                         # 工具函数
│   ├── __init__.py
│   └── action_tools.py           # 动作工具
│
├── graphs/                        # 图结构
│   ├── __init__.py
│   ├── judge_graph.py
│   ├── leader_graph.py
│   ├── memory_manager.py
│   ├── shared_state.py
│   └── soldier_graph.py
│
└── venv/                          # Python虚拟环境
```

---

## 四、前端结构 (frontend/)

```
frontend/
├── src/
│   ├── main.js                   # 入口文件
│   ├── App.vue                   # 根组件
│   ├── api/
│   │   └── index.js             # API调用
│   ├── pages/
│   │   └── Dashboard.vue        # 仪表盘
│   ├── components/
│   │   ├── GameMap.vue         # 游戏地图
│   │   ├── AgentStatus.vue     # Agent状态
│   │   ├── FactionStats.vue     # 阵营统计
│   │   ├── LogStream.vue        # 日志流
│   │   ├── GraphNode.vue        # 图节点
│   │   ├── GraphEdge.vue        # 图边
│   │   └── GlassMapTest.vue     # 玻璃拟态地图
│   ├── stores/
│   │   └── sandbox.js          # Pinia状态管理
│   ├── graph.js                 # 图形逻辑
│   └── index.html
├── dist/                         # 构建输出
│   ├── index.html
│   └── assets/
├── package.json                  # npm配置
├── vite.config.js               # Vite配置
└── package-lock.json
```

---

## 五、文档结构 (docs/)

```
docs/
├── core/                         # 核心文档
│   ├── ARCHITECTURE.md          # 架构设计文档 (v2.1)
│   ├── PROJECT_STRUCTURE.md      # 项目结构文档
│   └── rules.md                 # 游戏规则白皮书 (v2.1)
├── roadmap/                      # 路线图
│   ├── FEATURE_MEMO.md         # 功能备忘录
│   └── FUTURE_ROADMAP.md       # 未来规划
├── changelog/                   # 变更日志
│   ├── CHANGELOG.md
│   ├── CHANGELOG_v2.md
│   ├── CHANGELOG_PHASE4.md
│   ├── CHANGELOG_PHASE5_5.md
│   └── CHANGELOG_PHASE6.md
└── v2_glassmorphism/            # v2玻璃拟态
    ├── FINAL_SUMMARY.md
    ├── IMPLEMENTATION_SUMMARY.md
    └── TEST_REPORT.md
```

---

## 六、设计模式应用位置

### 6.1 策略模式 (RuleEngine)

| 文件 | 职责 |
|------|------|
| `service/action/ActionStrategy.java` | 策略接口 |
| `service/action/ActionStrategyFactory.java` | 策略工厂 |
| `service/action/*Strategy.java` | 9个具体策略实现 |
| `service/RuleEngine.java` | 规则引擎（简化版） |

### 6.2 责任链模式 (SandboxStateMachine)

| 文件 | 职责 |
|------|------|
| `service/tick/TickPhase.java` | 阶段接口 |
| `service/tick/*Phase.java` | 5个具体阶段实现 |
| `service/SandboxStateMachine.java` | 状态机（简化版） |

### 6.3 服务层分离 (GodMode)

| 文件 | 职责 |
|------|------|
| `service/GodModeService.java` | 上帝模式业务逻辑 |
| `controller/GodController.java` | HTTP请求处理 |

---

## 七、关键文件说明

### 7.1 后端关键文件

| 文件 | 说明 |
|------|------|
| `GameConstants.java` | 游戏规则常量，所有数值定义 |
| `Agent.java` | Agent实体，包含属性和状态判定 |
| `GameState.java` | 游戏状态，包含阵营库存、秩序之剑等 |
| `SandboxController.java` | REST API主入口 |
| `GodController.java` | 上帝模式API |

### 7.2 测试相关

| 文件 | 说明 |
|------|------|
| `SandboxSimulationTest.java` | 核心逻辑单元测试 |
| `init_db.sql` | 数据库初始化脚本 |
| `schema.sql` | 数据库Schema |

---

## 八、环境变量

### 8.1 后端 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zoo
    username: root
    password: password

python:
  agent:
    url: http://localhost:8000
```

### 8.2 Agent (.env)

```bash
MINIMAX_API_KEY=your_api_key
MINIMAX_MODEL=m2.7
MINIMAX_MODEL_FAST=m2.7-flash
```

---

## 九、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v2.1 | 2026-05-17 | 策略模式+责任链模式完整实现 |
| v2.0 | 2026-05-16 | Phase 5.5 架构升级规划 |
| v1.2 | 2026-05-06 | Phase 4 资源博弈系统 |
| v1.0 | 2026-04-16 | 初始版本 |

---

*文档版本：2.1*
*最后更新：2026-05-17*
*更新内容：*
* - 添加策略模式模块结构*
* - 添加责任链模式模块结构*
* - 更新设计模式应用位置说明*
* - 更新关键文件说明*
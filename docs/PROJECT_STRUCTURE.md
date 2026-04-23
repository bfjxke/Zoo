# GuardianEye-IIoT 沙箱动物园 - 项目结构说明

> 本文档详细解释项目目录结构、各部分职责和技术选型原因。

---

## 一、项目整体架构

```
g:\project\zoo\
│
├── backend/                    # Java后端 - 游戏引擎（Spring Boot）
├── agent/                      # Python AI调度器（FastAPI）
├── frontend/                   # Vue3前端 - 用户界面
├── docs/                      # 文档目录 - 规则白皮书、项目说明
├── start.bat                  # 一键启动脚本
└── init_db.sql               # 数据库初始化脚本
```

### 技术选型理由

| 模块 | 技术 | 选型理由 |
|------|------|----------|
| 后端 | Java Spring Boot | 企业级稳定，事务管理强，适合游戏服务器 |
| AI调度 | Python FastAPI | 快速开发AI逻辑，生态丰富，易于集成LLM |
| 前端 | Vue3 | 上手快，社区活跃，状态管理简单 |
| 数据库 | MySQL | 成熟稳定，数据安全，适合持久化存储 |

---

## 二、后端目录详解 (backend/)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/guardianeye/iiot/
│   │   │       ├── GuardianEyeIiotApplication.java    # Spring Boot入口
│   │   │       ├── config/                           # 配置类
│   │   │       │   ├── CorsConfig.java               # 跨域配置
│   │   │       │   └── WebSocketConfig.java          # WebSocket配置
│   │   │       ├── controller/                        # API控制器
│   │   │       │   ├── GodController.java            # 上帝视角控制（调试用）
│   │   │       │   └── SandboxController.java        # 沙箱游戏控制
│   │   │       ├── model/                            # 数据模型
│   │   │       │   ├── Agent.java                    # Agent实体
│   │   │       │   ├── GameState.java                # 游戏状态实体
│   │   │       │   ├── ActionLog.java                # 行为日志实体
│   │   │       │   ├── LeaderValue.java              # 领袖价值观种子
│   │   │       │   ├── AgentStatus.java              # Agent状态枚举
│   │   │       │   ├── GameConstants.java            # 游戏常量配置 ★重要
│   │   │       │   └── *Repository.java             # 数据库操作接口
│   │   │       ├── service/                          # 核心业务服务
│   │   │       │   ├── SandboxStateMachine.java      # 状态机引擎 ★核心
│   │   │       │   ├── RuleEngine.java               # 规则验证引擎 ★核心
│   │   │       │   ├── TickScheduler.java            # Tick调度器
│   │   │       │   ├── PythonDispatcher.java        # Python调度器
│   │   │       │   └── WebSocketPushService.java    # WebSocket推送
│   │   │       └── repository/                        # 数据库访问层
│   │   └── resources/
│   │       └── application.yml                       # Spring配置文件
│   └── test/                                         # 测试代码
│       └── java/com/guardianeye/iiot/
│           └── SandboxSimulationTest.java           # 模拟测试
├── pom.xml                                          # Maven依赖配置
└── mvnw                                              # Maven Wrapper脚本
```

### 各模块职责详解

#### 1. config/ - 配置类

**CorsConfig.java** - 跨域配置
- 作用：允许前端访问后端API
- 为什么需要：前端运行在3000端口，后端在8080端口，需要跨域许可

**WebSocketConfig.java** - WebSocket配置
- 作用：建立长连接，实现实时状态推送
- 为什么需要：游戏状态变化时，前端需要实时更新

#### 2. model/ - 数据模型

**Agent.java** - Agent实体
```java
// Agent的核心数据结构
- id: 唯一标识
- name: 名字
- faction: 阵营（lawful/aggressive/neutral）
- stamina/satiety/health: 三项属性
- currentNode: 当前位置
- alive/fatigued/hungry: 状态标志
```

**GameConstants.java** - 游戏常量 ★最重要
- 作用：所有规则数值集中管理
- 内容：时间系统、属性配置、动作消耗、地图节点
- 为什么重要：修改此文件即可调整游戏规则

**AgentStatus.java** - Agent状态枚举
```java
public enum AgentStatus {
    NORMAL,     // 正常
    FATIGUED,   // 疲劳
    HUNGRY,     // 饥饿
    CRITICAL,   // 危急（生命值低）
    DEAD,       // 死亡
    RESPAWNING  // 复活中
}
```

#### 3. service/ - 核心服务 ★最关键

**SandboxStateMachine.java** - 状态机引擎
- 作用：管理每个Tick的结算流程
- 4阶段设计：被动消耗 → 复活处理 → AI调度 → 状态保存
- 核心方法：`executeTick()`

**RuleEngine.java** - 规则验证引擎
- 作用：验证Agent的动作是否合法
- 白名单机制：move/eat/rest/talk/trade无需审批
- 非白名单动作：返回"JUDGE_PENDING"交AI判官

**TickScheduler.java** - Tick调度器
- 作用：定时触发executeTick()
- 每30秒执行一次（可配置）

**PythonDispatcher.java** - Python调度器
- 作用：调用Python AI服务获取决策
- 通信方式：HTTP请求

#### 4. controller/ - API控制器

**GodController.java** - 上帝视角控制
- 作用：提供调试接口，让操作员干预游戏
- 接口示例：`POST /api/god/move/{agentId}/{targetNode}`

**SandboxController.java** - 沙箱游戏控制
- 作用：游戏状态查询、动作执行、开始/暂停

---

## 三、AI调度器目录详解 (agent/)

```
agent/
├── main.py                     # FastAPI入口，启动服务
├── routers/                    # API路由
│   ├── __init__.py            # 路由包初始化
│   ├── decide.py              # 决策接口（/decide）
│   └── health.py              # 健康检查（/health）
├── services/                  # 业务服务
│   ├── __init__.py            # 服务包初始化
│   ├── minimax_client.py      # MiniMax API客户端
│   └── agent_scheduler.py    # Agent调度器
├── models/                     # 数据模型
│   ├── __init__.py            # 模型包初始化
│   └── schemas.py             # Pydantic数据模型
├── requirements.txt           # Python依赖列表
├── .env.example               # 环境变量示例
└── venv/                      # Python虚拟环境（自动创建）
```

### 各模块职责详解

#### 1. main.py - FastAPI入口

```python
# 作用：启动FastAPI应用，配置路由和中间件
# 关键功能：
# - CORS中间件（允许跨域）
# - 路由注册
# - 健康检查端点
```

#### 2. routers/decide.py - 决策入口

```python
@router.post("/decide")
async def decide(request: DecisionRequest):
    # 接收：Agent状态列表
    # 处理：为每个Agent生成决策
    # 返回：决策列表（move/eat/rest/talk）
```

#### 3. services/minimax_client.py - AI客户端

```python
class MiniMaxClient:
    async def get_decision(self, prompt, agent_context):
        # 构建prompt（给AI的指令）
        # 调用MiniMax API
        # 解析返回的决策
        # 包含Mock降级：API不可用时返回模拟决策
```

#### 4. services/agent_scheduler.py - Agent调度器

```python
class AgentScheduler:
    async def dispatch_decisions(self, agents, tick):
        # 批量处理所有Agent的决策
        # 控制请求频率（避免API限流）
        # 错误处理（单个失败不影响其他）
```

---

## 四、前端目录详解 (frontend/)

```
frontend/
├── src/
│   ├── main.js                 # Vue应用入口
│   ├── App.vue                 # 主页面组件 ★核心
│   ├── api/                    # API调用层
│   │   └── index.js           # Axios封装
│   ├── stores/                 # 状态管理（Pinia）
│   │   └── sandbox.js        # 游戏状态store
│   ├── components/             # 公共组件
│   │   ├── GodMap.vue         # 上帝视角地图
│   │   └── AgentStatus.vue   # Agent状态面板
│   └── assets/                # 静态资源
│       └── styles.css        # 全局样式
├── public/                     # 静态文件
│   └── index.html             # HTML模板
├── package.json              # npm依赖配置
├── vite.config.js            # Vite配置
└── README.md                 # 前端说明
```

### 核心组件说明

#### App.vue - 主页面

```
┌─────────────────────────────────────────────┐
│  Header: 游戏标题 + 控制按钮               │
├────────────────────────┬────────────────────┤
│                        │                    │
│   上帝视角地图          │   Agent状态面板    │
│   (GodMap.vue)         │   (AgentStatus)    │
│                        │                    │
│   - 7个节点可视化       │   - Agent列表      │
│   - Agent位置标记       │   - 属性进度条     │
│   - 连线表示相邻关系    │   - 状态标签       │
│                        │                    │
├────────────────────────┴────────────────────┤
│  Footer: Tick计数 + 游戏状态                │
└─────────────────────────────────────────────┘
```

#### stores/sandbox.js - 状态管理

```javascript
// 使用Pinia管理游戏状态
// 主要状态：
// - agents: Agent列表
// - gameState: 当前游戏状态
// - selectedAgent: 当前选中的Agent
// - tickCount: 当前Tick数
```

---

## 五、文档目录详解 (docs/)

```
docs/
├── rules.md                   # 游戏规则白皮书 ★核心规则文档
├── PROJECT_STRUCTURE.md       # 项目结构说明（本文件）
├── FEATURE_MEMO.md            # 功能备忘录（待实现功能）
├── CHANGELOG.md               # 改动日志（版本记录）
└── map.md                     # 地图可视化 ★直观展示
```

**rules.md** - 游戏规则白皮书
- 包含所有游戏规则的详细说明
- 数值配置、消耗公式、状态判定
- 是项目的"规则宪法"

---

## 五、docs/ 文档目录详解

```
docs/
├── rules.md                   # 游戏规则白皮书 ★核心规则文档
├── PROJECT_STRUCTURE.md       # 项目结构说明（本文件）
└── FEATURE_MEMO.md            # 功能备忘录（待实现功能清单）
```

### 各文档职责

| 文档 | 内容 | 重要性 |
|------|------|--------|
| rules.md | 游戏所有规则、数值、计算公式的详细说明 | ★★★★★ |
| CHANGELOG.md | 版本改动记录、代码改动清单 | ★★★★★ |
| PROJECT_STRUCTURE.md | 目录结构、技术选型、数据流图 | ★★★★ |
| map.md | ASCII地图可视化，直观展示 | ★★★★ |
| FEATURE_MEMO.md | 用户提出的点子、待实现功能、下一步计划 | ★★★ |

---

## 六、关键文件速查

| 文件路径 | 职责 | 重要性 |
|----------|------|--------|
| backend/.../GameConstants.java | 游戏所有规则数值 | ★★★★★ |
| backend/.../SandboxStateMachine.java | Tick执行引擎 | ★★★★★ |
| backend/.../RuleEngine.java | 动作验证引擎 | ★★★★ |
| agent/main.py | AI服务入口 | ★★★★ |
| agent/services/minimax_client.py | AI决策生成 | ★★★★ |
| frontend/src/App.vue | 前端主界面 | ★★★★ |
| docs/rules.md | 规则文档 | ★★★★ |
| init_db.sql | 数据库初始化 | ★★★ |
| start.bat | 启动脚本 | ★★★ |

---

## 七、数据流图

```
┌──────────────────────────────────────────────────────────────┐
│                         游戏主循环                            │
│                                                              │
│  ┌─────────────┐    30秒     ┌─────────────────────────┐   │
│  │ TickScheduler │ ───────→ │ SandboxStateMachine      │   │
│  └─────────────┘           │                           │   │
│                              │  阶段1: applyPassiveConsumption  │
│                              │  阶段2: processRespawn            │
│                              │  阶段3: PythonDispatcher.request  │
│                              │  阶段4: 保存状态                  │
│                              └───────────────┬─────────────┘   │
│                                              │                 │
│  ┌─────────────┐    HTTP     ┌─────────────▼─────────────┐   │
│  │ 前端展示     │ ←────────── │ Python AI调度器            │   │
│  │ (Vue)        │  WebSocket  │ (FastAPI)                  │   │
│  └─────────────┘ ←────────── │                             │   │
│                              │  miniMax_client.py          │   │
│                              │  agent_scheduler.py         │   │
│                              └─────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 八、依赖关系图

```
启动脚本 (start.bat)
    │
    ├── 创建Python虚拟环境
    │       │
    │       └── agent/requirements.txt
    │
    ├── 启动Java后端
    │       │
    │       └── backend/pom.xml (Maven)
    │               │
    │               └── Spring Boot
    │                       ├── SandboxStateMachine
    │                       ├── RuleEngine
    │                       └── GameConstants
    │
    └── 启动前端
            │
            └── frontend/package.json (npm)
                    │
                    └── Vue3 + Pinia + Axios
```

---

*文档版本：1.0*
*最后更新：2026-04-16*
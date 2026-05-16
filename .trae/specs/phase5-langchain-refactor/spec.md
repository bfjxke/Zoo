# Phase 5.5 LangChain/LangGraph Agent架构重构 - 规格说明

## Why
当前Python Agent是纯手写实现，存在以下问题：
- Prompt工程分散，难以管理和版本控制
- 工具调用（Tool Calling）需要手写
- 多Agent协作逻辑复杂
- 缺乏ReAct等Agent范式的原生支持

## What Changes

### 一、核心变更

#### 1.1 引入LangChain基础组件

将现有手写实现迁移到LangChain生态：
- `langchain-core` - 核心抽象
- `langchain-community` - 工具和集成
- `langchain-openai` - OpenAI兼容接口（用于MiniMax适配）

#### 1.2 引入LangGraph工作流

用LangGraph重构Agent调度逻辑：
- 定义Agent节点（LeaderAgent, SoldierAgent, JudgeAgent）
- 定义边和条件分支
- 实现ReAct风格的推理循环
- 集成Memory组件

#### 1.3 保持功能不变

重构后的功能必须与当前实现完全一致：
- 异步并发调度（asyncio.gather）
- 模型分层策略（m2.7/m2-her）
- 记忆管理（16回合常规+永久重要）
- 限流器（TokenBucketRateLimiter）

### 二、技术架构

#### 2.1 新文件结构

```
agent/
├── services/
│   ├── __init__.py
│   ├── agent_scheduler.py      # 重构为LangGraph工作流
│   ├── memory_manager.py      # 重构为LangChain Memory
│   ├── minimax_client.py      # 保留，添加适配器
│   ├── judge_service.py       # 保留
│   └── langchain_adapter.py   # 新增：MiniMax适配器
│
├── graphs/
│   ├── __init__.py
│   ├── leader_graph.py        # 新增：领袖Agent工作流
│   ├── soldier_graph.py       # 新增：小兵Agent工作流
│   └── shared_state.py        # 新增：共享状态定义
│
├── prompts/
│   ├── __init__.py
│   ├── leader_prompt.py       # 新增：领袖Prompt模板
│   ├── soldier_prompt.py      # 新增：小兵Prompt模板
│   └── judge_prompt.py        # 新增：判官Prompt模板
│
└── tools/
    ├── __init__.py
    ├── move_tool.py           # 新增：移动工具
    ├── eat_tool.py            # 新增：进食工具
    ├── rest_tool.py           # 新增：休息工具
    └── claim_food_tool.py     # 新增：领取食物工具
```

#### 2.2 LangGraph状态定义

```python
@dataclass
class AgentState:
    agent_id: int
    agent_name: str
    agent_faction: str
    personality: str
    
    # 当前状态
    stamina: int
    satiety: int
    health: int
    current_node: str
    
    # 上下文
    memory: List[Dict]          # LangChain Memory
    recent_logs: List[str]      # 最近日志
    tick: int
    
    # 决策
    action: Optional[str] = None
    target: Optional[str] = None
    reasoning: Optional[str] = None
```

#### 2.3 Agent节点定义

```python
# 领袖Agent工作流
leader_graph = StateGraph(AgentState)
leader_graph.add_node("observe", observe_node)
leader_graph.add_node("reason", reason_node)
leader_graph.add_node("act", act_node)
leader_graph.add_edge("observe", "reason")
leader_graph.add_edge("reason", "act")
```

## Impact
- Affected specs: Agent调度、记忆管理、决策生成
- Affected code: 
  - `agent/services/agent_scheduler.py`
  - `agent/services/memory_manager.py`
  - `agent/services/minimax_client.py`

## ADDED Requirements

### Requirement: LangGraph Agent工作流
系统 SHALL 使用LangGraph实现Agent决策工作流

#### Scenario: 领袖Agent决策
- **WHEN** 需要为领袖Agent生成决策
- **THEN** 使用LeaderGraph工作流：
  1. observe节点 - 收集当前状态和记忆
  2. reason节点 - 使用ReAct推理
  3. act节点 - 生成动作

#### Scenario: 小兵Agent决策
- **WHEN** 需要为小兵Agent生成决策
- **THEN** 使用SoldierGraph工作流（简化版）

### Requirement: LangChain Memory组件
系统 SHALL 使用LangChain Memory管理Agent记忆

#### Scenario: 记忆存储
- **WHEN** Agent执行动作后
- **THEN** 将结果存入LangChain Memory组件

#### Scenario: 记忆检索
- **WHEN** Agent需要做决策
- **THEN** 从Memory组件检索相关记忆

### Requirement: Tool Calling
系统 SHALL 使用LangChain Tool装饰器定义工具

#### Scenario: 定义移动工具
- **WHEN** 定义Agent可执行的动作
- **THEN** 使用@tool装饰器包装

## MODIFIED Requirements

### Requirement: 异步并发调度
**原实现**: asyncio.gather + 手动任务管理
**新实现**: LangGraph的异步执行 + 内置并发

### Requirement: 模型选择
**原实现**: 在scheduler中硬编码
**新实现**: 在prompt中动态选择模型

## REMOVED Requirements

### Requirement: 手写Prompt模板
**Reason**: 使用LangChain PromptTemplate统一管理
**Migration**: 迁移到prompts/目录

## 验收标准

1. 所有现有功能保持不变
2. 代码量减少至少40%
3. 支持Tool Calling
4. 支持ReAct推理
5. 支持Memory持久化
6. 编译和测试通过
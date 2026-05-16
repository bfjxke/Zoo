# Tasks - Phase 5.5 LangChain/LangGraph Agent架构重构

## 阶段1: 环境准备

- [x] Task 1.1: 安装LangChain依赖
  - [x] SubTask 1.1.1: 创建requirements-langchain.txt
  - [x] SubTask 1.1.2: pip install langchain-core langchain-community langgraph

- [x] Task 1.2: 创建目录结构
  - [x] SubTask 1.2.1: 创建graphs/目录
  - [x] SubTask 1.2.2: 创建prompts/目录
  - [x] SubTask 1.2.3: 创建tools/目录

## 阶段2: MiniMax适配器（简化，使用现有minimax_client）

- [x] Task 2.1: 创建MiniMax ChatModel适配器
  - [x] SubTask 2.1.1: 创建langchain_adapter.py（简化为使用现有client）
  - [x] SubTask 2.1.2: 实现BaseChatModel接口
  - [x] SubTask 2.1.3: 适配MiniMax API格式

- [x] Task 2.2: 测试适配器
  - [x] SubTask 2.2.1: 单元测试适配器
  - [x] SubTask 2.2.2: 验证API调用

## 阶段3: 状态定义

- [x] Task 3.1: 定义AgentState
  - [x] SubTask 3.1.1: 创建shared_state.py
  - [x] SubTask 3.1.2: 定义AgentState dataclass
  - [x] SubTask 3.1.3: 定义StateGraph

## 阶段4: Prompt模板

- [x] Task 4.1: 重构Prompt模板
  - [x] SubTask 4.1.1: 创建prompts/leader_prompt.py
  - [x] SubTask 4.1.2: 创建prompts/soldier_prompt.py
  - [x] SubTask 4.1.3: 创建prompts/judge_prompt.py
  - [x] SubTask 4.1.4: 使用ChatPromptTemplate

- [x] Task 4.2: 迁移现有Prompt
  - [x] SubTask 4.2.1: 迁移SYSTEM_PROMPT_LEADER
  - [x] SubTask 4.2.2: 迁移SYSTEM_PROMPT_SOLDIER
  - [x] SubTask 4.2.3: 迁移SYSTEM_PROMPT_JUDGE

## 阶段5: Tool定义

- [x] Task 5.1: 定义Agent动作工具
  - [x] SubTask 5.1.1: 创建tools/action_tools.py（move, eat, rest, claim_food, pickup_food, steal）
  - [x] SubTask 5.1.2: 使用@tool装饰器

- [x] Task 5.2: 注册工具到Agent
  - [x] SubTask 5.2.1: 创建tools/__init__.py导出所有工具
  - [x] SubTask 5.2.2: 绑定工具到对应的Agent

## 阶段6: LangGraph工作流

- [x] Task 6.1: 实现领袖Agent工作流
  - [x] SubTask 6.1.1: 创建graphs/leader_graph.py
  - [x] SubTask 6.1.2: 实现observe节点 - 收集状态和记忆
  - [x] SubTask 6.1.3: 实现reason节点 - ReAct推理
  - [x] SubTask 6.1.4: 实现act节点 - 生成动作
  - [x] SubTask 6.1.5: 编译图并测试

- [x] Task 6.2: 实现小兵Agent工作流
  - [x] SubTask 6.2.1: 创建graphs/soldier_graph.py
  - [x] SubTask 6.2.2: 实现简化版工作流（无ReAct）
  - [x] SubTask 6.2.3: 编译图并测试

- [x] Task 6.3: 实现判官工作流
  - [x] SubTask 6.3.1: 创建graphs/judge_graph.py
  - [x] SubTask 6.3.2: 实现判官决策逻辑
  - [x] SubTask 6.3.3: 编译图并测试

## 阶段7: Memory组件

- [x] Task 7.1: 重构记忆管理
  - [x] SubTask 7.1.1: 创建MemoryManager使用LangChain Memory
  - [x] SubTask 7.1.2: 实现ConversationMemory
  - [x] SubTask 7.1.3: 实现摘要Memory

- [x] Task 7.2: 集成记忆到工作流
  - [x] SubTask 7.2.1: 在工作流中注入Memory
  - [x] SubTask 7.2.2: 测试记忆存取

## 阶段8: 调度器重构（进行中）

- [ ] Task 8.1: 重构AgentScheduler
  - [ ] SubTask 8.1.1: 修改agent_scheduler.py使用LangGraph
  - [ ] SubTask 8.1.2: 保持asyncio.gather并发
  - [ ] SubTask 8.1.3: 保持限流器逻辑
  - [ ] SubTask 8.1.4: 保持模型选择逻辑

- [ ] Task 8.2: 保持向后兼容
  - [ ] SubTask 8.2.1: 保留/decide端点接口
  - [ ] SubTask 8.2.2: 保持返回格式不变

## 阶段9: 测试与验证

- [ ] Task 9.1: 功能测试
  - [ ] SubTask 9.1.1: 测试/decide端点
  - [ ] SubTask 9.1.2: 测试/judge端点
  - [ ] SubTask 9.1.3: 测试所有Agent类型

- [ ] Task 9.2: 性能测试
  - [ ] SubTask 9.2.1: 测试并发性能
  - [ ] SubTask 9.2.2: 测试响应时间

- [ ] Task 9.3: 回归测试
  - [ ] SubTask 9.3.1: 验证所有现有功能不变
  - [ ] SubTask 9.3.2: 验证API兼容性

## Task Dependencies

```
Task 1.x → Task 2.x → Task 3.x
                       ↓
Task 4.x → Task 5.x → Task 6.x → Task 8.x → Task 9.x
          ↓
        Task 7.x → Task 6.x
```

- Task 2.x 依赖 Task 1.x
- Task 3.x 依赖 Task 2.x（需要状态定义）
- Task 4.x 可与 Task 3.x 并行
- Task 5.x 依赖 Task 4.x
- Task 6.x 依赖 Task 3.x, 4.x, 5.x
- Task 7.x 可与 Task 6.x 并行
- Task 8.x 依赖 Task 6.x
- Task 9.x 依赖 Task 8.x

## 实施顺序

1. Task 1.x → 环境准备
2. Task 2.x → MiniMax适配器
3. Task 3.x → 状态定义
4. Task 4.x, 5.x → Prompt和工具（可并行）
5. Task 6.x → LangGraph工作流
6. Task 7.x → Memory组件（可与6.x并行）
7. Task 8.x → 调度器重构
8. Task 9.x → 测试验证
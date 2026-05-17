# LangChain/LangGraph Agent架构重构 - Phase 5.5

## 开发进度

### ✅ 已完成

#### 阶段1: 环境准备
- [x] requirements-langchain.txt 创建
- [x] 虚拟环境安装（阿里云镜像源）
- [x] 目录结构创建（graphs/, prompts/, tools/）

#### 阶段2: 状态定义
- [x] shared_state.py - AgentState dataclass定义
- [x] GraphState TypedDict定义

#### 阶段3: Prompt模板（LangChain ChatPromptTemplate）
- [x] leader_prompt.py - 领袖Prompt模板（ChatPromptTemplate + MessagesPlaceholder）
- [x] soldier_prompt.py - 小兵Prompt模板（阵营参数化 partial）
- [x] judge_prompt.py - 判官Prompt模板

#### 阶段4: Tool定义（LangChain @tool装饰器）
- [x] action_tools.py - 8个工具（move, eat, rest, talk, claim_food, pickup_food, steal, provoke）
- [x] @tool装饰器使用，含参数文档
- [x] ALL_TOOLS列表 + TOOL_MAP字典

#### 阶段5: LangGraph工作流（集成LLM调用）
- [x] leader_graph.py - 领袖Agent工作流（observe→think→act，ReAct推理循环）
- [x] soldier_graph.py - 小兵Agent工作流（decide，集成LLM + 围困状态处理）
- [x] judge_graph.py - 判官工作流（judge，集成LLM + JSON解析）

#### 阶段6: Memory组件
- [x] memory_manager.py - AgentMemoryManager + GlobalMemoryStore
- [x] 重要记忆永久保存 + 常规记忆16回合滚动

#### 阶段7: 调度器重构
- [x] agent_scheduler.py 集成LangGraph工作流
- [x] _to_agent_state() 统一转换函数
- [x] _select_graph_for_role() 角色路由
- [x] make_decision_with_graph() 核心决策函数
- [x] dispatch_all_agents() 保持asyncio.gather并发
- [x] 保持限流器逻辑（TokenBucketRateLimiter）
- [x] 保持模型选择逻辑（m2.7/m2-her）

#### 阶段8: 服务层更新
- [x] judge_service.py 重构为LangGraph工作流
- [x] __init__.py 文件更新（graphs/, prompts/, tools/）

#### 阶段9: MiniMax适配器
- [x] minimax_chat_model.py - BaseChatModel子类
- [x] create_minimax_chat_model() 工厂函数
- [x] 支持m2.7和m2-her两种模型

#### 阶段10: 验证
- [x] 所有模块导入验证通过
- [x] FastAPI应用启动验证通过
- [x] 路由注册验证通过（/decide, /judge/judge, /health）

---

## 新增/修改文件清单

```
agent/
├── graphs/                          # LangGraph工作流
│   ├── __init__.py                 # 导出所有Graph和State
│   ├── shared_state.py             # AgentState dataclass
│   ├── leader_graph.py            # 领袖ReAct工作流（observe→think→act）
│   ├── soldier_graph.py           # 小兵决策工作流（decide）
│   ├── judge_graph.py             # 判官裁决工作流（judge）
│   └── memory_manager.py          # Memory组件
│
├── prompts/                        # LangChain Prompt模板
│   ├── __init__.py
│   ├── leader_prompt.py          # 领袖ChatPromptTemplate
│   ├── soldier_prompt.py         # 小兵ChatPromptTemplate（阵营参数化）
│   └── judge_prompt.py           # 判官ChatPromptTemplate
│
├── tools/                          # LangChain Tool定义
│   ├── __init__.py
│   └── action_tools.py            # 8个@tool定义 + ALL_TOOLS + TOOL_MAP
│
├── services/
│   ├── agent_scheduler.py         # 重构：LangGraph工作流调度
│   ├── judge_service.py           # 重构：LangGraph判官服务
│   └── minimax_chat_model.py      # 新增：BaseChatModel适配器
│
└── requirements-langchain.txt     # LangChain依赖
```

---

## 技术亮点

### 1. MiniMax ChatModel适配器

将MiniMax API适配为LangChain标准接口，使LangGraph能直接调用：

```python
class MiniMaxChatModel(BaseChatModel):
    model: str = Field(default="m2.7")
    api_key: str = Field(default_factory=lambda: os.getenv("MINIMAX_API_KEY", ""))
    
    def _generate(self, messages, stop=None, run_manager=None, **kwargs):
        # 将LangChain消息格式转为MiniMax API格式
        # 调用MiniMax API
        # 返回ChatResult
```

### 2. ReAct推理循环（Leader Graph）

领袖Agent使用完整的ReAct（观察-思考-行动）推理循环：

```
observe → think → act → END

observe: 收集状态、记忆、日志
think:  LLM推理，生成反思和计划
act:    LLM决策，生成动作和目标
```

### 3. 阵营参数化Prompt

士兵Prompt支持阵营动态参数化：

```python
def get_soldier_prompt(faction: str = "lawful") -> ChatPromptTemplate:
    faction_name = FACTION_NAMES.get(faction, faction)
    return ChatPromptTemplate.from_messages([...]).partial(faction_name=faction_name)
```

### 4. 围困状态短路

所有Graph都实现了围困状态的短路处理，避免不必要的LLM调用：

```python
if confinement_ticks > 0:
    return {"action": "confined", "target": None, ...}
```

### 5. Tool Calling集成

8个工具使用LangChain @tool装饰器定义，支持参数文档和类型提示：

```python
@tool
def steal_tool(target_faction: str) -> dict:
    """偷窃对方阵营的食物。被抓住会被围困。"""
    return {"action": "steal", "target": target_faction, ...}
```

### 6. Memory组件

```python
AgentMemoryManager:
- important_memories: 永久记忆（steal/talk等关键事件）
- regular_memories: 16回合滚动记忆
- chat_history: LangChain消息历史
```

---

## 模型选择策略

| 角色 | 模型 | Temperature | 说明 |
|------|------|-------------|------|
| Leader | m2.7 | 0.7 | 标准推理，适中创造性 |
| Soldier | m2-her | 0.8 | 角色扮演模型，高创造性 |
| Judge | m2.7 | 0.3 | 标准推理，低创造性保证公正 |

---

## API兼容性

所有API接口保持不变：
- `POST /decide` - Agent决策调度
- `POST /judge/judge` - 判官裁决
- `GET /health` - 健康检查

---

*文档版本：v2.0*
*创建日期：2026-05-16*
*最后更新：2026-05-17*
*更新内容：Phase 5.5全部完成，包括调度器重构、服务层更新、导入验证*

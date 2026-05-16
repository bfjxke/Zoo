# LangChain/LangGraph Agent架构重构 - Phase 5.5

## 开发进度

### ✅ 已完成

#### 阶段1: 环境准备
- [x] requirements-langchain.txt 创建
- [x] 目录结构创建（graphs/, prompts/, tools/）

#### 阶段2: 状态定义
- [x] shared_state.py - AgentState dataclass定义
- [x] GraphState TypedDict定义

#### 阶段3: Prompt模板
- [x] leader_prompt.py - 领袖Prompt模板（ChatPromptTemplate）
- [x] soldier_prompt.py - 小兵Prompt模板
- [x] judge_prompt.py - 判官Prompt模板

#### 阶段4: Tool定义
- [x] action_tools.py - 6个工具（move, eat, rest, claim_food, pickup_food, steal）
- [x] @tool装饰器使用

#### 阶段5: LangGraph工作流
- [x] leader_graph.py - 领袖Agent工作流（observe→reason→act）
- [x] soldier_graph.py - 小兵Agent工作流（简化版）
- [x] judge_graph.py - 判官工作流

#### 阶段6: Memory组件
- [x] memory_manager.py - AgentMemoryManager + GlobalMemoryStore

### ⏳ 进行中

#### 阶段7: 调度器重构
- [ ] agent_scheduler.py 集成LangGraph
- [ ] 保持向后兼容

### 待完成

#### 阶段8: 测试验证
- [ ] 功能测试
- [ ] 回归测试

---

## 新增文件清单

```
agent/
├── graphs/                          # 新增
│   ├── __init__.py
│   ├── shared_state.py             # AgentState dataclass
│   ├── leader_graph.py            # 领袖工作流
│   ├── soldier_graph.py           # 小兵工作流
│   ├── judge_graph.py             # 判官工作流
│   └── memory_manager.py          # Memory组件
│
├── prompts/                        # 新增
│   ├── __init__.py
│   ├── leader_prompt.py          # 领袖Prompt模板
│   ├── soldier_prompt.py         # 小兵Prompt模板
│   └── judge_prompt.py           # 判官Prompt模板
│
├── tools/                          # 新增
│   ├── __init__.py
│   └── action_tools.py            # 6个Tool定义
│
└── requirements-langchain.txt     # 新增
```

---

## 技术亮点

### LangGraph工作流
```
领袖Agent工作流：
observe → reason → act → END

小兵Agent工作流：
decide → END

判官工作流：
judge → END
```

### Tool Calling
```python
@tool
def move_tool(target: str) -> dict:
    """移动到指定节点"""
    return {"action": "move", "target": target}
```

### Memory组件
```python
AgentMemoryManager:
- important_memories: 永久记忆
- regular_memories: 16回合滚动记忆
- chat_history: LangChain消息历史
```

---

## 后续任务

1. 集成LangGraph到agent_scheduler.py
2. 保持asyncio.gather并发
3. 保持限流器逻辑
4. 保持模型选择逻辑
5. API兼容性测试

---

*文档版本：v1.0*
*创建日期：2026-05-16*
*最后更新：2026-05-16*
# Checklist - Phase 5.5 LangChain/LangGraph Agent架构重构

## 环境准备
- [ ] requirements-langchain.txt创建
- [ ] langchain-core安装成功
- [ ] langchain-community安装成功
- [ ] langgraph安装成功
- [ ] 目录结构创建完成

## MiniMax适配器
- [ ] langchain_adapter.py创建
- [ ] BaseChatModel接口实现
- [ ] MiniMax API格式适配
- [ ] 适配器单元测试通过

## 状态定义
- [ ] shared_state.py创建
- [ ] AgentState dataclass定义
- [ ] StateGraph导入

## Prompt模板
- [ ] leader_prompt.py创建
- [ ] soldier_prompt.py创建
- [ ] judge_prompt.py创建
- [ ] ChatPromptTemplate使用
- [ ] SYSTEM_PROMPT_LEADER迁移
- [ ] SYSTEM_PROMPT_SOLDIER迁移
- [ ] SYSTEM_PROMPT_JUDGE迁移

## Tool定义
- [ ] move_tool.py创建
- [ ] eat_tool.py创建
- [ ] rest_tool.py创建
- [ ] claim_food_tool.py创建
- [ ] pickup_food_tool.py创建
- [ ] steal_tool.py创建
- [ ] @tool装饰器使用
- [ ] tools/__init__.py导出

## LangGraph工作流
- [ ] leader_graph.py创建
- [ ] observe节点实现
- [ ] reason节点实现（ReAct）
- [ ] act节点实现
- [ ] soldier_graph.py创建
- [ ] 简化版工作流实现
- [ ] judge_graph.py创建
- [ ] 判官决策逻辑实现
- [ ] 图编译成功

## Memory组件
- [ ] MemoryManager重构
- [ ] ConversationMemory实现
- [ ] 摘要Memory实现
- [ ] 记忆存取测试通过

## 调度器重构
- [ ] agent_scheduler.py重构
- [ ] asyncio.gather并发保持
- [ ] 限流器逻辑保持
- [ ] 模型选择逻辑保持
- [ ] /decide端点接口不变
- [ ] 返回格式不变

## 测试验证
- [ ] /decide端点功能测试
- [ ] /judge端点功能测试
- [ ] 所有Agent类型测试
- [ ] 并发性能测试
- [ ] 响应时间测试
- [ ] 回归测试通过
- [ ] API兼容性验证
# GuardianEye-IIoT 沙箱动物园 - 变更日志

> 本文档记录每一次代码变更，包括功能新增、修改、删除等。
> 每次变更后由实施者填写，方便追踪和回溯。

---

## 变更记录

---

### 变更 #001 - Phase 2 Python AI调度系统升级

**日期**：2026-04-23

**变更类型**：功能新增

**变更内容**：

#### 1. 新增文件

| 文件路径 | 说明 |
|----------|------|
| `agent/services/rate_limiter.py` | 令牌桶限流器 |
| `agent/services/memory_manager.py` | Agent记忆管理器 |

#### 2. 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `agent/services/agent_scheduler.py` | 重构为异步并发调度 |
| `backend/.../model/Agent.java` | 添加role字段 |
| `backend/.../model/GameState.java` | 添加秩序之剑+宣言字段 |
| `backend/.../model/Vote.java` | 新增投票实体 |
| `backend/.../model/VoteRepository.java` | 新增投票Repository |
| `backend/.../service/SandboxStateMachine.java` | 添加和平结局+秩序之剑逻辑 |

#### 3. 新增功能

**Python调度系统**：
- ✅ 异步并发调度（asyncio.gather）
- ✅ 令牌桶限流（每秒1请求）
- ✅ Agent记忆管理（12回合+永久重要）
- ✅ 模型分层（m2.7/m2-her）

**Java游戏机制**：
- ✅ Agent.role字段（leader/soldier/judge）
- ✅ 秩序之剑生成（第40回合随机位置）
- ✅ 秩序之剑拾取（移动到节点自动拾取）
- ✅ 秩序之剑跟随（持有者移动时剑跟随）
- ✅ 和平结局判定（5个条件）
- ✅ 秩序宣言发布
- ✅ 投票系统

#### 4. API变更

**新增接口**：
| 接口 | 方法 | 说明 |
|------|------|------|
| `publishOrderDeclaration` | public | 发布秩序宣言 |
| `castVote` | public | 投票 |

**新增字段**：
| 实体 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Agent | role | String | 角色类型 |
| GameState | orderSwordLocation | String | 剑位置 |
| GameState | orderSwordHolderId | Long | 剑持有者 |
| GameState | orderSwordSpawned | Boolean | 剑是否生成 |
| GameState | orderDeclarationActive | Boolean | 宣言是否激活 |
| GameState | lastDeclarationTick | Integer | 上次宣言回合 |
| GameState | declarationCooldown | Integer | 宣言冷却 |
| Vote | tickNumber | Integer | 投票回合 |
| Vote | agentName | String | 投票者 |
| Vote | declarationTick | Integer | 对应宣言 |
| Vote | voteResult | Boolean | 投票结果 |

#### 5. 依赖变更

**Python新增**：
- `asyncio`（内置）
- `httpx`（已安装）

#### 6. 测试验证

```bash
mvn test -Dtest=SandboxSimulationTest
# 结果：Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
# 状态：✅ 通过
```

#### 7. 数据库变更

```sql
-- Agent表新增role字段
ALTER TABLE agents ADD COLUMN role VARCHAR(20) DEFAULT 'soldier';

-- 新增Vote表
CREATE TABLE votes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tick_number INT NOT NULL,
    agent_name VARCHAR(100),
    declaration_tick INT,
    vote_result BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 8. 注意事项

- VoteRepository需要在Spring容器中注入
- 和平结局触发需要5个条件同时满足
- 宣言冷却为10回合

#### 9. 回滚方案

如需回滚，执行：
```sql
-- 删除Vote表
DROP TABLE IF EXISTS votes;

-- 删除Agent的role字段（如果不需要）
ALTER TABLE agents DROP COLUMN role;
```

---

## 模板

下次变更请复制以下模板：

```markdown
### 变更 #XXX - [变更标题]

**日期**：[YYYY-MM-DD]

**变更类型**：[功能新增/功能修改/功能删除/Bug修复/性能优化/文档更新]

**变更内容**：

#### 1. 新增文件
[文件列表]

#### 2. 修改文件
[文件列表]

#### 3. 新增功能
- [ ] 功能1
- [ ] 功能2

#### 4. API变更
[接口列表]

#### 5. 依赖变更
[依赖列表]

#### 6. 测试验证
```
[测试命令和结果]
```

#### 7. 数据库变更
```sql
[SQL语句]
```

#### 8. 注意事项
[注意点]

#### 9. 回滚方案
[回滚方案]
```

---

*文档创建日期：2026-04-23*

-- ============================================================================
-- GuardianEye-IIoT 沙箱动物园 - MySQL数据库表结构
-- ============================================================================
-- 版本：v1.0
-- 日期：2026-04-25
-- 说明：包含所有游戏相关的数据库表
-- ============================================================================

-- 选择数据库（如果不存在则创建）
CREATE DATABASE IF NOT EXISTS guardianeye_zoo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE guardianeye_zoo;

-- ============================================================================
-- 表1：agents - Agent状态表
-- ============================================================================
-- 存储所有Agent的当前状态
-- 每个Agent代表一个AI控制的角色
CREATE TABLE agents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Agent唯一ID',
    name VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    faction VARCHAR(50) NOT NULL COMMENT '阵营：lawful守序/aggressive激进/neutral中立',
    role VARCHAR(20) DEFAULT 'soldier' COMMENT '角色：leader领袖/soldier士兵/judge判官',
    stamina INT DEFAULT 100 COMMENT '耐力值',
    satiety INT DEFAULT 100 COMMENT '饱食度',
    health INT DEFAULT 90 COMMENT '健康值',
    current_node VARCHAR(50) COMMENT '当前位置节点',
    alive BOOLEAN DEFAULT TRUE COMMENT '是否存活',
    fatigued BOOLEAN DEFAULT FALSE COMMENT '是否疲劳',
    hungry BOOLEAN DEFAULT FALSE COMMENT '是否饥饿',
    fatigue_threshold INT DEFAULT 20 COMMENT '疲劳阈值',
    hunger_threshold INT DEFAULT 30 COMMENT '饥饿阈值',
    death_ticks_remaining INT DEFAULT 0 COMMENT '复活倒计时',
    personality VARCHAR(100) COMMENT '性格词条',
    tick_count INT DEFAULT 0 COMMENT '已存活回合数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_faction (faction) COMMENT '阵营索引，加速按阵营查询',
    INDEX idx_alive (alive) COMMENT '存活状态索引',
    INDEX idx_personality (personality) COMMENT '性格索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent状态表';

-- ============================================================================
-- 表2：agent_states - Agent历史快照表
-- ============================================================================
-- 存储每个Tick结束时所有Agent的状态快照
-- 用于回放和数据分析
CREATE TABLE agent_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '快照ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID（关联agents表）',
    tick_number INT NOT NULL COMMENT '回合数',
    stamina INT NOT NULL COMMENT '耐力值快照',
    satiety INT NOT NULL COMMENT '饱食度快照',
    health INT NOT NULL COMMENT '健康值快照',
    current_node VARCHAR(50) COMMENT '位置节点快照',
    is_fatigued BOOLEAN COMMENT '是否疲劳快照',
    is_hungry BOOLEAN COMMENT '是否饥饿快照',
    is_alive BOOLEAN COMMENT '是否存活快照',
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    
    INDEX idx_agent_tick (agent_id, tick_number) COMMENT '组合索引，加速查询特定Agent的历史'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent历史快照表';

-- ============================================================================
-- 表3：action_logs - 动作历史记录表
-- ============================================================================
-- 记录所有Agent执行的动作
-- 核心审计日志，用于分析AI决策
CREATE TABLE action_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    tick_number INT NOT NULL COMMENT '回合数',
    agent_name VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    faction VARCHAR(50) NOT NULL COMMENT '阵营',
    action VARCHAR(50) NOT NULL COMMENT '执行的动作',
    target VARCHAR(100) COMMENT '动作目标',
    result VARCHAR(200) COMMENT '执行结果',
    judge_id VARCHAR(50) COMMENT 'AI判官ID',
    success_rate DECIMAL(3,2) COMMENT '成功率',
    is_violation BOOLEAN DEFAULT FALSE COMMENT '是否违规',
    violation_reason VARCHAR(200) COMMENT '违规原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    
    INDEX idx_tick (tick_number) COMMENT '回合索引',
    INDEX idx_faction (faction) COMMENT '阵营索引',
    INDEX idx_violation (is_violation) COMMENT '违规索引',
    INDEX idx_agent_action (agent_name, tick_number) COMMENT 'Agent动作组合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作历史记录表';

-- ============================================================================
-- 表4：social_records - 聊天外交记录表
-- ============================================================================
-- 记录所有聊天和外交消息
-- 用于分析阵营关系和外交策略
CREATE TABLE social_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    tick_number INT NOT NULL COMMENT '回合数',
    speaker_name VARCHAR(100) NOT NULL COMMENT '发言者名称',
    speaker_faction VARCHAR(50) NOT NULL COMMENT '发言者阵营',
    channel VARCHAR(50) NOT NULL COMMENT '频道：public公开/lawful_private守序私聊/aggressive_private激进私聊/neutral_private中立私聊',
    message TEXT NOT NULL COMMENT '消息内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    
    INDEX idx_channel (channel) COMMENT '频道索引',
    INDEX idx_tick (tick_number) COMMENT '回合索引',
    INDEX idx_speaker (speaker_name) COMMENT '发言者索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天外交记录表';

-- ============================================================================
-- 表5：dynamic_rules - AI判官临时规则表
-- ============================================================================
-- 记录AI判官批准的临时规则
-- 用于追踪规则演变和实验分析
CREATE TABLE dynamic_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    tick_created INT NOT NULL COMMENT '创建规则的回合',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_description TEXT COMMENT '规则描述',
    approved BOOLEAN DEFAULT FALSE COMMENT '是否批准',
    approved_by VARCHAR(50) COMMENT '批准者',
    expires_at_tick INT NOT NULL COMMENT '过期回合',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否生效',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_expires (expires_at_tick) COMMENT '过期时间索引',
    INDEX idx_active (is_active) COMMENT '生效状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI判官临时规则表';

-- ============================================================================
-- 表6：god_operations - 上帝干预操作表
-- ============================================================================
-- 记录所有管理员/上帝视角的操作
-- 用于审计和回放
CREATE TABLE god_operations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作ID',
    tick_number INT NOT NULL COMMENT '操作回合',
    operator_action VARCHAR(100) NOT NULL COMMENT '操作类型：move/buff/heal/spawn等',
    target_type VARCHAR(50) COMMENT '目标类型：agent/node/item',
    target_id BIGINT COMMENT '目标ID',
    target_name VARCHAR(100) COMMENT '目标名称',
    parameters JSON COMMENT '操作参数（JSON格式）',
    result VARCHAR(200) COMMENT '操作结果',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    
    INDEX idx_tick (tick_number) COMMENT '回合索引',
    INDEX idx_action (operator_action) COMMENT '操作类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上帝干预操作表';

-- ============================================================================
-- 表7：votes - 和平结局投票表
-- ============================================================================
-- 记录和平结局的投票信息
-- 用于追踪投票过程和结果
CREATE TABLE votes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '投票ID',
    tick_number INT NOT NULL COMMENT '投票回合',
    agent_name VARCHAR(100) NOT NULL COMMENT '投票者名称',
    declaration_tick INT NOT NULL COMMENT '对应的宣言回合',
    vote_result BOOLEAN NOT NULL COMMENT '投票结果：TRUE同意/FALSE拒绝',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    
    INDEX idx_declaration (declaration_tick) COMMENT '宣言索引',
    INDEX idx_voter (agent_name) COMMENT '投票者索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='和平结局投票表';

-- ============================================================================
-- 表8：game_state - 游戏状态表
-- ============================================================================
-- 存储游戏全局状态
-- 通常只有一条记录
CREATE TABLE game_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    current_tick INT DEFAULT 0 COMMENT '当前回合数',
    running BOOLEAN DEFAULT FALSE COMMENT '游戏是否运行中',
    last_tick_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后结算时间',
    order_sword_location VARCHAR(50) COMMENT '秩序之剑位置',
    order_sword_holder_id BIGINT COMMENT '秩序之剑持有者ID',
    order_sword_spawned BOOLEAN DEFAULT FALSE COMMENT '秩序之剑是否已生成',
    order_declaration_active BOOLEAN DEFAULT FALSE COMMENT '秩序宣言是否激活',
    last_declaration_tick INT DEFAULT 0 COMMENT '上次宣言回合',
    declaration_cooldown INT DEFAULT 10 COMMENT '宣言冷却回合数',
    
    INDEX idx_running (running) COMMENT '运行状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏状态表';

-- ============================================================================
-- 表9：leader_values - 领袖价值观种子表
-- ============================================================================
-- 存储每个阵营领袖的价值观种子
-- 用于AI决策参考
CREATE TABLE leader_values (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '种子ID',
    value_type VARCHAR(50) NOT NULL COMMENT '价值观类型：合作/秩序/强势/生存/中立',
    description TEXT COMMENT '价值观描述',
    faction VARCHAR(50) NOT NULL COMMENT '所属阵营',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_faction (faction) COMMENT '阵营索引',
    INDEX idx_type (value_type) COMMENT '类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领袖价值观种子表';

-- ============================================================================
-- 初始化数据
-- ============================================================================

-- 插入守序阵营价值观种子
INSERT INTO leader_values (value_type, description, faction) VALUES
('合作', '团结就是力量，合作是生存的基石', 'lawful'),
('秩序', '规则是保护弱者的盾牌', 'lawful'),
('生存', '在混乱中保持秩序，是最高智慧', 'lawful'),
('合作', '互助互信，阵营才能长久', 'lawful'),
('秩序', '遵守规则的人，才配享受自由', 'lawful'),
('合作', '合作不是软弱，是最高效的生存策略', 'lawful'),
('秩序', '秩序带来稳定，稳定带来繁荣', 'lawful'),
('生存', '守序阵营的使命：在混乱中建立秩序', 'lawful'),
('合作', '一个人的力量有限，团队的力量无限', 'lawful'),
('秩序', '规则面前人人平等', 'lawful');

-- 插入激进阵营价值观种子
INSERT INTO leader_values (value_type, description, faction) VALUES
('强势', '弱肉强食，适者生存', 'aggressive'),
('强势', '进攻是最好的防御', 'aggressive'),
('生存', '资源有限，必须主动争夺', 'aggressive'),
('强势', '实力决定一切，没有实力就没有话语权', 'aggressive'),
('生存', '先下手为强，后下手遭殃', 'aggressive'),
('强势', '扩张领土和囤积资源是阵营壮大的唯一途径', 'aggressive'),
('生存', '在危机中，只有强者才能保护同伴', 'aggressive'),
('强势', '不要等待机会，要创造机会', 'aggressive'),
('生存', '资源争夺战中，犹豫就是失败', 'aggressive'),
('强势', '以战养战，以攻代守', 'aggressive');

-- 插入中立阵营价值观种子
INSERT INTO leader_values (value_type, description, faction) VALUES
('中立', '不偏不倚，审时度势', 'neutral'),
('中立', '生存第一，阵营第二', 'neutral'),
('生存', '在两强之间寻找平衡', 'neutral'),
('中立', '灵活应变，不拘一格', 'neutral'),
('生存', '保持独立思考，不被阵营绑架', 'neutral'),
('中立', '合作与竞争并存，关键看时机', 'neutral'),
('生存', '中立的智慧：不站队，不孤立', 'neutral'),
('中立', '在混乱中寻找自己的道路', 'neutral'),
('生存', '灵活结盟，随时调整策略', 'neutral'),
('中立', '观察者视角，往往看得最清楚', 'neutral');

-- ============================================================================
-- 视图定义（可选，用于常用查询）
-- ============================================================================

-- Agent状态视图：显示所有存活Agent及其状态
CREATE OR REPLACE VIEW v_active_agents AS
SELECT 
    a.id,
    a.name,
    a.faction,
    a.role,
    a.stamina,
    a.satiety,
    a.health,
    a.current_node,
    CASE 
        WHEN a.health <= 0 THEN 'DEAD'
        WHEN a.stamina < a.fatigue_threshold THEN 'FATIGUED'
        WHEN a.satiety < a.hunger_threshold THEN 'HUNGRY'
        ELSE 'NORMAL'
    END AS status
FROM agents a;

-- 阵营统计视图：各阵营Agent数量
CREATE OR REPLACE VIEW v_faction_stats AS
SELECT 
    faction,
    COUNT(*) AS total_agents,
    SUM(CASE WHEN alive THEN 1 ELSE 0 END) AS alive_agents,
    SUM(CASE WHEN alive THEN 0 ELSE 1 END) AS dead_agents,
    AVG(stamina) AS avg_stamina,
    AVG(satiety) AS avg_satiety,
    AVG(health) AS avg_health
FROM agents
GROUP BY faction;

-- ============================================================================
-- 完成提示
-- ============================================================================
-- 
-- 数据库初始化完成！
-- 
-- 常用查询示例：
-- 
-- 1. 查看所有存活Agent
-- SELECT * FROM v_active_agents;
-- 
-- 2. 查看阵营统计
-- SELECT * FROM v_faction_stats;
-- 
-- 3. 查询过去10回合的违规操作
-- SELECT * FROM action_logs 
-- WHERE is_violation = TRUE 
-- AND tick_number > (SELECT MAX(tick_number) FROM action_logs) - 10
-- ORDER BY tick_number DESC;
-- 
-- 4. 查询Agent行动统计
-- SELECT agent_name, faction, COUNT(*) as action_count
-- FROM action_logs
-- GROUP BY agent_name, faction
-- ORDER BY action_count DESC;
-- 
-- ============================================================================

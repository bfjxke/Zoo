-- GuardianEye-IIoT 沙箱动物园 数据库初始化脚本
-- 使用前请确保 MySQL 8.0 已安装并运行

CREATE DATABASE IF NOT EXISTS guardianeye_zoo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE guardianeye_zoo;

-- 领袖价值观种子数据（20条）
INSERT INTO leader_values (category, content, faction) VALUES
('合作', '团结就是力量，合作是生存的基石', 'lawful'),
('秩序', '规则是保护弱者的盾牌', 'lawful'),
('生存', '在混乱中保持秩序，是最高智慧', 'lawful'),
('合作', '互助互信，阵营才能长久', 'lawful'),
('秩序', '遵守规则的人，才配享受自由', 'lawful'),
('合作', '合作不是软弱，是最高效的生存策略', 'lawful'),
('秩序', '秩序带来稳定，稳定带来繁荣', 'lawful'),
('生存', '守序阵营的使命：在混乱中建立秩序', 'lawful'),
('合作', '一个人的力量有限，团队的力量无限', 'lawful'),
('秩序', '规则面前人人平等', 'lawful'),
('强势', '弱肉强食，适者生存', 'aggressive'),
('强势', '进攻是最好的防御', 'aggressive'),
('生存', '资源有限，必须主动争夺', 'aggressive'),
('强势', '实力决定一切，没有实力就没有话语权', 'aggressive'),
('生存', '先下手为强，后下手遭殃', 'aggressive'),
('强势', '扩张领土和囤积资源是阵营壮大的唯一途径', 'aggressive'),
('生存', '在危机中，只有强者才能保护同伴', 'aggressive'),
('强势', '不要等待机会，要创造机会', 'aggressive'),
('生存', '资源争夺战中，犹豫就是失败', 'aggressive'),
('强势', '以战养战，以攻代守', 'aggressive'),
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

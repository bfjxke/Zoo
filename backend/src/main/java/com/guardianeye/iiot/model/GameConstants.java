package com.guardianeye.iiot.model;

import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * 游戏核心常量配置 - GuardianEye-IIoT 沙箱动物园
 * ============================================================================
 *
 * 本文件定义了游戏的所有核心规则数值，是整个系统的"规则宪法"。
 * 所有其他代码在执行游戏逻辑时，都必须参照这里的常量。
 *
 * 为什么单独成立一个常量类？
 * - 集中管理：所有规则数值在一处，便于查看和修改
 * - 避免魔数：不在代码中写死数字，用常量名代替
 * - 易于调整：通过修改常量即可平衡游戏，无需改代码
 */
public final class GameConstants {

    private GameConstants() {}

    // ========================================================================
    // 第一部分：时间系统
    // ========================================================================

    /**
     * 回合(Tick)间隔时间：30秒
     *
     * 设计理念：
     * - 30秒足够让人类观察者理解发生了什么
     * - 足够短，让游戏保持紧张感
     * - 足够长，让AI有充足时间做决策
     *
     * Tick是游戏的基本时间单位，1 Tick = 30秒真实时间
     */
    public static final int TICK_INTERVAL_SECONDS = 30;

    // ========================================================================
    // 第二部分：Agent基础属性
    // ========================================================================

    /**
     * Agent初始耐力：100点
     * 耐力(Stamina)代表Agent的体力，决定能执行多少动作
     */
    public static final int STAMINA_INITIAL = 100;

    /**
     * Agent初始饱食度：100点
     * 饱食度(Satiety)代表Agent的饥饿程度，0以下会扣健康
     */
    public static final int SATIETY_INITIAL = 100;

    /**
     * Agent初始健康值：100点
     * 健康(Health)代表Agent的生命，归零即死亡
     */
    public static final int HEALTH_INITIAL = 100;

    /**
     * 饱食度上限（含Buff）：140点
     *
     * 设计理念：
     * - 正常上限100点为"饱"
     * - 进食Buff可让饱食度超过100，最高到140
     * - 超过100的部分代表"过度饱食"，提供耐力恢复加速
     * - 40点的缓冲空间（超出100的40点）让玩家有囤积食物的动力
     */
    public static final int SATIETY_MAX_WITH_BUFF = 140;

    // ========================================================================
    // 第三部分：动作消耗与恢复
    // ========================================================================

    /**
     * 基础耐力消耗：10点/回合
     *
     * 计算方式：每回合自动扣除10点耐力
     * - 无Buff无惩罚时：100点耐力可支撑10回合（约5分钟）
     * - 有疲劳惩罚时：消耗增加到15点，只能支撑约6.6回合
     *
     * 这个消耗确保游戏不会无限进行，强制Agent需要管理资源
     */
    public static final int STAMINA_BASE_COST = 10;

    /**
     * 移动耐力消耗：15点/次
     *
     * 比基础消耗多50%，模拟"移动比站着更累"
     * - 正常状态：移动一次消耗15点
     * - 有疲劳惩罚（×1.5）：移动一次消耗22.5点（实际扣23）
     * - 有Buff（×0.7）：移动一次消耗10.5点（实际扣11）
     */
    public static final int STAMINA_MOVE_COST = 15;

    /**
     * 休息恢复耐力：20点/次
     *
     * 设计理念：
     * - 休息一次可恢复20点耐力
     * - 但如果处于疲劳/饥饿状态，恢复效果会打折
     * - 正常状态：恢复20点
     * - 疲劳状态：恢复 20/1.5 ≈ 13点
     * - 疲劳+饥饿：恢复 20/(1.5×1.5) ≈ 8点
     *
     * 这样设计让状态不好的Agent恢复变慢，增加生存压力
     */
    public static final int STAMINA_REST_RECOVER = 20;

    /**
     * 基础饱食度消耗：5点/回合
     *
     * 比耐力消耗慢，模拟"不运动就饿得慢"
     * - 无惩罚时：100点饱食度可支撑20回合（约10分钟）
     * - 有饥饿惩罚时：消耗增加到7.5点，只能支撑约13回合
     *
     * 注意：饥饿惩罚不仅增加消耗，还会扣健康！
     */
    public static final int SATIETY_BASE_COST = 5;

    /**
     * 进食恢复饱食度：30点/次
     *
     * 设计理念：
     * - 一份食物恢复30点饱食度
     * - 从0吃到100需要吃4份食物
     * - 从100吃到140需要再吃2份食物
     *
     * 为什么是30点？因为它让"4份刚好吃饱"成为一个自然的目标
     */
    public static final int SATIETY_EAT_RECOVER = 30;

    /**
     * 饥饿扣血：5点/回合
     *
     * 当饱食度低于30时，每回合扣5点健康
     *
     * 饥饿惩罚链：
     * 饱食度耗尽 -> 每回合扣5健康 + 消耗增加50% -> 健康归零 -> 死亡
     */
    public static final int HEALTH_HUNGER_DAMAGE = 5;

    // ========================================================================
    // 第四部分：状态阈值
    // ========================================================================

    /**
     * 疲劳阈值：20点
     * 当耐力低于20时，Agent进入"疲劳状态"
     *
     * 疲劳状态效果：
     * - 耐力消耗增加50%（10点变成15点）
     * - 饱食度消耗增加50%（5点变成7.5点）
     * - 休息恢复效果降低（20点变成13点）
     */
    public static final int FATIGUE_THRESHOLD = 20;

    /**
     * 饥饿阈值：30点
     * 当饱食度低于30时，Agent进入"饥饿状态"
     *
     * 饥饿状态效果：
     * - 耐力消耗增加50%
     * - 饱食度消耗增加50%
     * - 每回合扣5点健康
     */
    public static final int HUNGER_THRESHOLD = 30;

    /**
     * 惩罚倍率：1.5倍
     *
     * 状态不好时，消耗增加的倍率
     * 疲劳和饥饿的惩罚可以叠加（最高×2.25）
     */
    public static final double PENALTY_MULTIPLIER = 1.5;

    /**
     * 饱食Buff恢复加速：0.7倍
     *
     * 当饱食度 > 100时，耐力恢复速度×0.7（加快50%）
     * 休息本应恢复20点，有Buff时恢复 20/0.7 ≈ 28点
     *
     * 为什么用0.7而不是1.5？
     * 因为恢复是"除以倍率"，0.7意味着"加快到1.43倍"
     */
    public static final double SATIETY_BUFF_RECOVERY_MULTIPLIER = 0.7;

    /**
     * Buff触发阈值：100点
     * 饱食度超过此值时，触发"饱餐Buff"
     */
    public static final int SATIETY_BUFF_THRESHOLD = 100;

    // ========================================================================
    // 第五部分：死亡与复活
    // ========================================================================

    /**
     * 复活所需回合数：3回合
     *
     * Agent死亡后需要等待3个回合才能复活
     * 这段时间是"死亡冷却期"，对手有机会做其他事
     */
    public static final int RESPAWN_TICKS = 3;

    /**
     * 复活后属性保留：50%
     *
     * 设计理念：
     * - 死亡不是无代价的，需要付出50%属性作为惩罚
     * - 复活后只有半管血，需要时间恢复
     * - 防止"死亡=无代价"的博弈失衡
     *
     * 复活后：
     * - 耐力 = 100 × 50% = 50点
     * - 饱食度 = 100 × 50% = 50点
     * - 健康 = 100 × 50% = 50点
     * - 疲劳/饥饿状态清除
     * - 强制回到阵营基地
     */
    public static final int RESPAWN_STAT_PERCENT = 50;

    // ========================================================================
    // 第六部分：自定义动作与AI判官
    // ========================================================================

    /**
     * 自定义动作耐力消耗：30点
     *
     * 当AI执行不在白名单中的动作时，需要消耗30点耐力
     * 这是一个"尝试费"，防止AI胡乱尝试
     */
    public static final int CUSTOM_ACTION_COST = 30;

    /**
     * 默认成功率：60%
     *
     * 白名单外的动作需要AI判官裁决
     * AI判官默认给60%的成功率
     * 实际成功率由AI判官根据情境调整
     */
    public static final double DEFAULT_SUCCESS_RATE = 0.6;

    /**
     * 自定义规则有效期：20回合
     *
     * AI判官批准的临时规则有效期为20回合
     * 超过后需要重新申请
     */
    public static final int CUSTOM_RULE_EXPIRE_TICKS = 20;

    // ========================================================================
    // 第七部分：白名单动作
    // ========================================================================

    /**
     * 允许的动作白名单
     *
     * 以下5个动作是"预设合法"的，不需要AI判官审批：
     * - move：移动到相邻节点
     * - eat：进食恢复饱食度
     * - rest：休息恢复耐力
     * - talk：在频道发言
     * - trade：与其他Agent交易
     */
    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "move", "eat", "rest", "talk", "trade"
    );

    // ========================================================================
    // 第八部分：地图节点
    // ========================================================================

    /**
     * 所有节点列表
     *
     * 地图共7个节点：
     * - 3个阵营基地：base_lawful（守序）、base_aggressive（强势）、base_neutral（中立）
     * - 1个中心节点：center（战略要冲）
     * - 3个野外资源点：forest（森林）、river（河流）、mountain（山地）
     */
    public static final Set<String> ALL_NODES = Set.of(
            "base_lawful", "base_aggressive", "base_neutral",
            "center", "forest", "river", "mountain"
    );

    /**
     * 节点相邻关系
     *
     * 设计理念：辐射状结构
     * - 每个阵营基地只连接center和自己的野外
     * - center连接所有6个其他节点
     * - 野外节点只连接自己的阵营基地和center
     *
     * 这样设计的博弈效果：
     * - 想去其他阵营，必须经过center（暴露风险）
     * - center成为必争之地
     * - 每个阵营有"后花园"（自己的野外）可以躲藏
     */
    public static final Map<String, Set<String>> ADJACENT_NODES = Map.of(
            "base_lawful",   Set.of("center", "forest"),
            "base_aggressive", Set.of("center", "mountain"),
            "base_neutral",  Set.of("center", "river"),
            "center",        Set.of("base_lawful", "base_aggressive", "base_neutral", "forest", "river", "mountain"),
            "forest",        Set.of("base_lawful", "center"),
            "river",         Set.of("base_neutral", "center"),
            "mountain",      Set.of("base_aggressive", "center")
    );

    /**
     * 阵营对应的基地节点
     *
     * 每个阵营有且只有一个基地
     * 复活、阵营私聊都必须在这里
     */
    public static final Map<String, String> FACTION_BASE = Map.of(
            "lawful",     "base_lawful",
            "aggressive", "base_aggressive",
            "neutral",    "base_neutral"
    );

    // ========================================================================
    // 第九部分：通讯频道
    // ========================================================================

    /**
     * 可用的发言频道
     *
     * 共有4个频道：
     * - lawful_private：守序阵营内部频道
     * - aggressive_private：强势阵营内部频道
     * - neutral_private：中立阵营内部频道
     * - public：公开频道（所有人可见）
     *
     * 阵营私聊的限制：
     * - 只能在阵营基地发言
     * - 只有同阵营成员能接收
     */
    public static final Set<String> FACTION_CHANNELS = Set.of(
            "lawful_private", "aggressive_private", "neutral_private", "public"
    );
}
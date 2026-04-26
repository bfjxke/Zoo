package com.guardianeye.iiot.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * 游戏核心常量配置 - GuardianEye-IIoT 沙箱动物园
 * ============================================================================
 *
 * 本文件定义了游戏的所有核心规则数值，是整个系统的"规则宪法"。
 * 所有其他代码在执行游戏逻辑时，都必须参照这里的常量。
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

    /**
     * 最大游戏回合数：50回合
     *
     * 当游戏达到50回合时触发"永无止境结局"
     * 50回合约等于25分钟的游戏时长
     */
    public static final int MAX_GAME_TICKS = 50;

    /**
     * 秩序之剑生成回合：40回合
     *
     * 游戏进行到40回合时，秩序之剑会在随机位置生成
     * 给各阵营20回合（10分钟）的时间来争夺
     */
    public static final int ORDER_SWORD_SPAWN_TICK = 40;

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
     * Agent初始健康值：90点
     *
     * 设计理念：
     * - 相比原来的100点，降低了10点
     * - 配合饥饿扣血20点/回合的设计
     * - 营造"容易死"的紧张氛围
     * - 90点配合饥饿4.5回合死亡的节奏
     */
    public static final int HEALTH_INITIAL = 90;

    /**
     * Agent生命值上限：90点
     * 健康(Health)代表Agent的生命，归零即死亡
     */
    public static final int HEALTH_MAX = 90;

    /**
     * 饱食度上限（含Buff）：140点
     *
     * 设计理念：
     * - 正常上限100点为"饱"
     * - 进食Buff可让饱食度超过100，最高到140
     * - 超过100的部分代表"过度饱食"，提供耐力恢复加速和额外回血
     * - 40点的缓冲空间让玩家有囤积食物的动力
     */
    public static final int SATIETY_MAX_WITH_BUFF = 140;

    // ========================================================================
    // 第三部分：动作消耗与恢复
    // ========================================================================

    /**
     * 基础耐力消耗：10点/回合
     *
     * 计算方式：每回合自动扣除10点耐力
     * - 无Buff无惩罚时：100点耐力可支撑10回合
     * - 有疲劳惩罚时：消耗增加到15点
     *
     * 这个消耗确保游戏不会无限进行
     */
    public static final int STAMINA_BASE_COST = 10;

    /**
     * 移动耐力消耗：15点/次
     *
     * 比基础消耗多50%，模拟"移动比站着更累"
     */
    public static final int STAMINA_MOVE_COST = 15;

    /**
     * 休息恢复耐力：20点/次
     *
     * 设计理念：
     * - 休息一次可恢复20点耐力
     * - 如果处于疲劳状态，恢复效果会打折
     * - 如果有饱食Buff，恢复效果会加速
     *
     * 恢复公式：20 / 状态惩罚 / Buff加成
     */
    public static final int STAMINA_REST_RECOVER = 20;

    /**
     * 基础饱食度消耗：5点/回合
     *
     * 比耐力消耗慢，模拟"不运动就饿得慢"
     * - 100点饱食度可支撑20回合
     *
     
     */
    public static final int SATIETY_BASE_COST = 5;

    /**
     * 进食恢复饱食度：30点/次
     *
     * 一份食物恢复30点饱食度
     * 从0吃到100需要吃4份食物
     * 从100吃到140需要再吃2份食物
     */
    public static final int SATIETY_EAT_RECOVER = 30;

    // ========================================================================
    // 第四部分：生命值系统
    // ========================================================================

    /**
     * 饥饿扣血：20点/回合
     *
     * 当饱食度低于30时，每回合扣20点健康
     *
     * 设计理念：
     * - 大幅提高饥饿惩罚，营造"容易死"的环境
     * - 配合生命上限90的设计：饥饿状态下最多撑4.5回合
     * - 相比原来的5点，提高了4倍
     *
     * 饥饿惩罚链：
     * 饱食度<30 -> 每回合扣20健康 -> 约4-5回合后死亡
     */
    public static final int HEALTH_HUNGER_DAMAGE = 20;

    /**
     * 饱食回血阈值：30点
     *
     * 当饱食度 > 30时，每回合回复健康
     * 作用是给玩家一个"安全区"，只要不是太饿就能慢慢恢复
     */
    public static final int HEALTH_REGEN_SATIETY_THRESHOLD = 30;

    /**
     * 饱食回血量：5点/回合
     *
     * 当饱食度 > 30时，每回合回复5点健康
     * 配合90点生命上限：约18回合可以从1血回满
     */
    public static final int HEALTH_REGEN_NORMAL = 5;

    /**
     * 饱食Buff回血量：10点/回合
     *
     * 当饱食度 > 100时，每回合回复10点健康
     * 比普通回血快一倍，激励玩家囤积食物
     */
    public static final int HEALTH_REGEN_BUFF = 10;

    // ========================================================================
    // 第五部分：状态阈值
    // ========================================================================

    /**
     * 疲劳阈值：20点
     * 当耐力低于20时，Agent进入"疲劳状态"
     *
     * 疲劳状态效果：
     * - 耐力消耗增加50%（10点变成15点）
     * - 休息恢复效果降低（20点变成13点）
     *
     * 饥饿状态不再影响饱食度消耗
     */
    public static final int FATIGUE_THRESHOLD = 20;

    /**
     * 饥饿阈值：30点
     * 当饱食度低于30时，Agent进入"饥饿状态"
     *
     * 饥饿状态效果：
     * - 每回合扣20点健康
     * - 耐力消耗增加50%
     */
    public static final int HUNGER_THRESHOLD = 30;

    /**
     * 惩罚倍率：1.5倍
     *
     * 状态不好时，消耗增加的倍率
     * 疲劳和饥饿的惩罚可以叠加
     */
    public static final double PENALTY_MULTIPLIER = 1.5;

    /**
     * 饱食Buff恢复倍率：约0.588
     *
     * 当饱食度 > 100时，耐力恢复加速1.7倍
     * 计算公式：恢复量 = 基础恢复 / 0.588 ≈ 基础恢复 × 1.7
     * 例如：休息20点 / 0.588 ≈ 34点
     */
    public static final double SATIETY_BUFF_RECOVERY_MULTIPLIER = 0.588;

    /**
     * Buff触发阈值：100点
     * 饱食度超过此值时，触发"饱餐Buff"
     */
    public static final int SATIETY_BUFF_THRESHOLD = 100;

    // ========================================================================
    // 第六部分：死亡与复活
    // ========================================================================

    /**
     * 复活所需回合数：3回合
     *
     * Agent死亡后需要等待3个回合才能复活
     */
    public static final int RESPAWN_TICKS = 3;

    /**
     * 复活后属性保留：50%
     *
     * 死亡不是无代价的，需要付出50%属性作为惩罚
     * 复活后：
     * - 耐力 = 100 × 50% = 50点
     * - 饱食度 = 100 × 50% = 50点
     * - 健康 = 90 × 50% = 45点
     */
    public static final int RESPAWN_STAT_PERCENT = 50;

    // ========================================================================
    // 第七部分：AI判官系统
    // ========================================================================

    /**
     * 自定义动作耐力消耗：30点
     *
     * 当AI执行不在白名单中的动作时，需要消耗30点耐力
     */
    public static final int CUSTOM_ACTION_COST = 30;

    /**
     * 默认成功率：60%
     *
     * 白名单外的动作需要AI判官裁决
     */
    public static final double DEFAULT_SUCCESS_RATE = 0.6;

    /**
     * 自定义规则有效期：8回合
     *
     * 相比原来的20回合，缩短到8回合
     * 快速变换规则，增加游戏趣味性
     */
    public static final int CUSTOM_RULE_EXPIRE_TICKS = 8;

    // ========================================================================
    // 第八部分：白名单动作
    // ========================================================================

    /**
     * 允许的动作白名单
     *
     * 以下动作是"预设合法"的，不需要AI判官审批：
     * - move：移动到相邻节点
     * - eat：进食恢复饱食度
     * - rest：休息恢复耐力
     * - talk：在频道发言
     * - trade：与其他Agent交易
     * - provoke：挑衅（v1.1新增）
     */
    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "move", "eat", "rest", "talk", "trade", "provoke"
    );

    // ========================================================================
    // 第九部分：地图节点
    // ========================================================================

    /**
     * 所有节点列表
     *
     * 地图共8个节点：
     * - 3个阵营基地：A（守序）、B（中立）、C（激进）
     * - 2个广场：D、E
     * - 3个野外资源点：F（森林）、G（河流）、H（山地）
     */
    public static final Set<String> ALL_NODES = Set.of(
            "A", "B", "C", "D", "E", "F", "G", "H"
    );

    /**
     * 节点相邻关系（对称结构）
     *
     * A（守序）连接 D、F
     * B（中立）连接 D、E、G
     * C（激进）连接 E、H
     * D（左广场）连接 A、B、E、F、G
     * E（右广场）连接 B、C、D、G、H
     * F（森林）连接 A、D
     * G（河流）连接 B、D、E、H
     * H（山地）连接 C、E、G
     */
    public static final Map<String, Set<String>> ADJACENT_NODES = Map.of(
            "A", Set.of("D", "F"),
            "B", Set.of("D", "E", "G"),
            "C", Set.of("E", "H"),
            "D", Set.of("A", "B", "E", "F", "G"),
            "E", Set.of("B", "C", "D", "G", "H"),
            "F", Set.of("A", "D"),
            "G", Set.of("B", "D", "E", "H"),
            "H", Set.of("C", "E", "G")
    );

    /**
     * 阵营对应的基地节点
     */
    public static final Map<String, String> FACTION_BASE = Map.of(
            "lawful",     "A",
            "aggressive", "C",
            "neutral",    "B"
    );

    // ========================================================================
    // 第十部分：通讯频道
    // ========================================================================

    /**
     * 可用的发言频道
     *
     * 阵营私聊的限制：
     * - 只能在阵营基地节点发言
     * - 只有同阵营成员能接收
     */
    public static final Set<String> FACTION_CHANNELS = Set.of(
            "lawful_private", "aggressive_private", "neutral_private", "public"
    );

    // ========================================================================
    // 第十一部分：和平结局系统
    // ========================================================================

    /**
     * 和平结局触发最小回合数：40回合
     *
     * 和平结局需要游戏进行至少40回合
     * 配合秩序之剑生成时间
     */
    public static final int PEACE_ENDING_MIN_TICKS = 40;

    /**
     * 守序阵营全员Buff：10%
     *
     * 当守序阵营持有秩序之剑时
     * 所有守序阵营Agent获得10%的全属性加成
     */
    public static final double ORDER_FACTION_BUFF = 0.1;

    /**
     * 秩序宣言冷却：10回合
     *
     * 发布守序宣言后需要等待10回合才能再次发布
     */
    public static final int ORDER_DECLARATION_COOLDOWN = 10;
    
    // ========================================================================
    // 第十二部分：图结构集成（v2.0新增）
    // ========================================================================
    
    /**
     * 获取GameGraph单例实例
     * 用于替代硬编码的节点和邻接关系
     */
    public static GameGraph getGameGraph() {
        return GameGraph.getInstance();
    }
    
    /**
     * 获取指定节点的相邻节点列表
     * @param nodeId 节点ID
     * @return 相邻节点ID列表
     */
    public static List<String> getAdjacentNodes(String nodeId) {
        return getGameGraph().getAdjacentNodes(nodeId);
    }
    
    /**
     * 检查两个节点是否相邻
     * @param node1 节点1
     * @param node2 节点2
     * @return 是否相邻
     */
    public static boolean areAdjacent(String node1, String node2) {
        return getGameGraph().areAdjacent(node1, node2);
    }
    
    /**
     * 获取指定阵营的基地节点ID
     * @param faction 阵营名称
     * @return 基地节点ID
     */
    public static String getFactionBaseNode(String faction) {
        return FACTION_BASE.get(faction);
    }
    
    /**
     * 获取所有节点ID列表
     * @return 所有节点ID
     */
    public static Set<String> getAllNodeIds() {
        return getGameGraph().getNodes().keySet();
    }
    
    /**
     * 获取所有节点对象列表
     * @return 所有GraphNode对象
     */
    public static List<GraphNode> getAllGraphNodes() {
        return getGameGraph().getAllNodes();
    }
    
    /**
     * 获取所有边列表
     * @return 所有GraphEdge对象
     */
    public static List<GraphEdge> getAllGraphEdges() {
        return getGameGraph().getEdges();
    }
    
    /**
     * 获取中心节点
     * @return 中心节点对象
     */
    public static GraphNode getCenterNode() {
        return getGameGraph().getCenter();
    }
    
    /**
     * 获取指定节点对象
     * @param nodeId 节点ID
     * @return GraphNode对象
     */
    public static GraphNode getGraphNode(String nodeId) {
        return getGameGraph().getNode(nodeId);
    }
}

package com.guardianeye.iiot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityTraits {
    private String id;
    private String name;
    private String description;
    private String emoji;
    private List<String> tendencies;
    private double aggressionModifier;
    private double cooperationModifier;
    private double survivalModifier;
    private double loyaltyModifier;
    
    public static List<PersonalityTraits> getAllTraits() {
        return List.of(
            new PersonalityTraits("brave", "勇敢", "无畏冲锋，面对危险不退缩", "⚔️",
                List.of("主动进攻", "保护队友", "冲锋陷阵"), 1.2, 0.8, 1.0, 1.2),
            
            new PersonalityTraits("cautious", "谨慎", "谋定而后动，三思而后行", "🛡",
                List.of("避免冲突", "防守优先", "收集情报"), 0.6, 1.0, 1.3, 0.9),
            
            new PersonalityTraits("cunning", "狡诈", "兵者诡道，智取为上", "🎭",
                List.of("欺骗战术", "伺机而动", "借刀杀人"), 0.8, 0.6, 1.4, 0.5),
            
            new PersonalityTraits("loyal", "忠诚", "忠诚不绝对，就是绝对不忠诚", "🛡️",
                List.of("服从指挥", "守护基地", "不离不弃"), 0.9, 1.3, 0.8, 1.5),
            
            new PersonalityTraits("rebellious", "叛逆", "规则是用来打破的", "🔥",
                List.of("无视指令", "独自行动", "挑战权威"), 1.3, 0.4, 1.1, 0.3),
            
            new PersonalityTraits("greedy", "贪婪", "资源是越多越好", "💰",
                List.of("囤积资源", "抢夺物资", "贸易优先"), 1.0, 0.5, 1.2, 0.6),
            
            new PersonalityTraits("peaceful", "和平", "能不动手就不动手", "☮️",
                List.of("避免战斗", "促进和解", "调解争端"), 0.3, 1.5, 0.9, 1.0),
            
            new PersonalityTraits("adventurous", "冒险", "风险与机遇并存", "🗺",
                List.of("探索未知", "挑战强敌", "开拓领地"), 1.1, 0.7, 0.8, 0.7),
            
            new PersonalityTraits("strategic", "战略", "运筹帷幄之中，决胜千里之外", "♟️",
                List.of("全局思考", "资源规划", "长期布局"), 0.8, 1.1, 1.3, 1.0),
            
            new PersonalityTraits("charismatic", "魅力", "一句话就能说服别人", "🗣️",
                List.of("外交谈判", "鼓舞士气", "联盟组建"), 0.5, 1.4, 0.9, 1.2),
            
            new PersonalityTraits("feral", "野性", "回归本能，适者生存", "🐺",
                List.of("弱肉强食", "单打独斗", "领地意识"), 1.5, 0.3, 1.0, 0.4),
            
            new PersonalityTraits("wise", "睿智", "知识就是力量", "📚",
                List.of("情报收集", "分析局势", "传授经验"), 0.6, 1.2, 1.2, 1.1)
        );
    }
    
    public static PersonalityTraits getById(String id) {
        return getAllTraits().stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(getAllTraits().get(0));
    }
    
    public static List<PersonalityTraits> getTraitsByFaction(String faction) {
        switch (faction) {
            case "lawful":
                return List.of(
                    getById("brave"), getById("cautious"), getById("loyal"),
                    getById("strategic"), getById("charismatic"), getById("wise")
                );
            case "aggressive":
                return List.of(
                    getById("feral"), getById("greedy"), getById("rebellious"),
                    getById("adventurous"), getById("cunning"), getById("brave")
                );
            case "neutral":
                return List.of(
                    getById("cunning"), getById("peaceful"), getById("strategic"),
                    getById("charismatic"), getById("adventurous"), getById("wise")
                );
            default:
                return getAllTraits();
        }
    }
}

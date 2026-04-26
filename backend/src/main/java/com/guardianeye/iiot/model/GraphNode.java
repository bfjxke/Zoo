package com.guardianeye.iiot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {
    private String id;
    private NodeType type;
    private String faction;
    private String label;
    private String icon;
    private int x;
    private int y;
    
    public GraphNode(String id, NodeType type, String faction, String label, int x, int y) {
        this.id = id;
        this.type = type;
        this.faction = faction;
        this.label = label;
        this.icon = type.getIcon();
        this.x = x;
        this.y = y;
    }
    
    public boolean isBase() {
        return type == NodeType.BASE;
    }
    
    public boolean isCenter() {
        return type == NodeType.CENTER;
    }
    
    public boolean isWilderness() {
        return type == NodeType.WILDERNESS;
    }
    
    public boolean hasFaction() {
        return faction != null && !faction.isEmpty();
    }
    
    public String getDisplayLabel() {
        if (label != null && !label.isEmpty()) {
            return label;
        }
        return switch (type) {
            case BASE -> switch (faction) {
                case "lawful" -> "守序基地";
                case "aggressive" -> "激进基地";
                case "neutral" -> "中立基地";
                default -> "基地";
            };
            case CENTER -> "中心区域";
            case WILDERNESS -> switch (id) {
                case "forest" -> "森林";
                case "mountain" -> "山地";
                case "river" -> "河流";
                default -> "野外";
            };
        };
    }
}

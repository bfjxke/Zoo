package com.guardianeye.iiot.model;

public enum NodeType {
    BASE("基地", "🏛"),
    CENTER("中心", "⭐"),
    WILDERNESS("野外", "🌲");
    
    private final String label;
    private final String icon;
    
    NodeType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getIcon() {
        return icon;
    }
}

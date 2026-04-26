package com.guardianeye.iiot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {
    private String sourceId;
    private String targetId;
    private int weight;
    
    public GraphEdge(String sourceId, String targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.weight = 1;
    }
    
    public boolean connects(String nodeId) {
        return sourceId.equals(nodeId) || targetId.equals(nodeId);
    }
    
    public String getOtherEnd(String nodeId) {
        if (sourceId.equals(nodeId)) {
            return targetId;
        } else if (targetId.equals(nodeId)) {
            return sourceId;
        }
        return null;
    }
    
    public boolean isValid() {
        return sourceId != null && !sourceId.isEmpty() 
            && targetId != null && !targetId.isEmpty()
            && !sourceId.equals(targetId);
    }
}

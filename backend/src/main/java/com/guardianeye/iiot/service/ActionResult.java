package com.guardianeye.iiot.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActionResult {
    private boolean success;
    private String message;
    private String judgeId;
    private Double successRate;
    
    public static ActionResult success(String message) {
        return new ActionResult(true, message, null, null);
    }
    
    public static ActionResult failure(String message) {
        return new ActionResult(false, message, null, null);
    }
    
    public static ActionResult pending(String judgeId, Double successRate) {
        return new ActionResult(false, "需提交AI判官判定", judgeId, successRate);
    }
}
package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "game_state")
public class GameState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer currentTick = 0;

    @Column(nullable = false)
    private Boolean running = false;

    @Column(nullable = false)
    private LocalDateTime lastTickTime = LocalDateTime.now();

    // ==================== 秩序之剑系统 ====================

    @Column
    private String orderSwordLocation;  // 剑所在节点

    @Column
    private Long orderSwordHolderId;  // 持有者Agent ID

    @Column(nullable = false)
    private Boolean orderSwordSpawned = false;  // 剑是否已生成

    // ==================== 和平结局系统 ====================

    @Column(nullable = false)
    private Boolean orderDeclarationActive = false;  // 宣言是否激活

    @Column(nullable = false)
    private Integer lastDeclarationTick = 0;  // 上次宣言回合

    @Column(nullable = false)
    private Integer declarationCooldown = 10;  // 宣言冷却回合数

    // ==================== Getter和Setter ====================

    public String getOrderSwordLocation() {
        return orderSwordLocation;
    }

    public void setOrderSwordLocation(String orderSwordLocation) {
        this.orderSwordLocation = orderSwordLocation;
    }

    public Long getOrderSwordHolderId() {
        return orderSwordHolderId;
    }

    public void setOrderSwordHolderId(Long orderSwordHolderId) {
        this.orderSwordHolderId = orderSwordHolderId;
    }

    public Boolean getOrderSwordSpawned() {
        return orderSwordSpawned;
    }

    public void setOrderSwordSpawned(Boolean orderSwordSpawned) {
        this.orderSwordSpawned = orderSwordSpawned;
    }

    public Boolean getOrderDeclarationActive() {
        return orderDeclarationActive;
    }

    public void setOrderDeclarationActive(Boolean orderDeclarationActive) {
        this.orderDeclarationActive = orderDeclarationActive;
    }

    public Integer getLastDeclarationTick() {
        return lastDeclarationTick;
    }

    public void setLastDeclarationTick(Integer lastDeclarationTick) {
        this.lastDeclarationTick = lastDeclarationTick;
    }

    public Integer getDeclarationCooldown() {
        return declarationCooldown;
    }

    public void setDeclarationCooldown(Integer declarationCooldown) {
        this.declarationCooldown = declarationCooldown;
    }
}

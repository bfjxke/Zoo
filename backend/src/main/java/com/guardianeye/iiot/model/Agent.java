package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String faction;

    @Column(nullable = false)
    private String role = "soldier";  // leader/soldier/judge

    @Column(nullable = false)
    private Integer stamina = 100;

    @Column(nullable = false)
    private Integer satiety = 100;

    @Column(nullable = false)
    private Integer health = 100;

    @Column(nullable = false)
    private String currentNode = "base";

    @Column(nullable = false)
    private Boolean alive = true;

    @Column(nullable = false)
    private Integer deathTicksRemaining = 0;

    @Column(nullable = false)
    private Boolean fatigued = false;

    @Column(nullable = false)
    private Boolean hungry = false;

    @Column(nullable = false)
    private Integer tickCount = 0;

    public boolean isFatigued() {
        return stamina < 20;
    }

    public boolean isHungry() {
        return satiety < 30;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public void applyFatigueMultiplier() {
        if (isFatigued()) {
            fatigued = true;
        }
        if (isHungry()) {
            hungry = true;
        }
    }
}

package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Table(name = "agent_states")
public class AgentState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private Integer tickNumber;

    @Column(nullable = false)
    private Integer stamina;

    @Column(nullable = false)
    private Integer satiety;

    @Column(nullable = false)
    private Integer health;

    @Column
    private String currentNode;

    @Column
    private Boolean isFatigued;

    @Column
    private Boolean isHungry;

    @Column(nullable = false)
    private Boolean isAlive;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();
}

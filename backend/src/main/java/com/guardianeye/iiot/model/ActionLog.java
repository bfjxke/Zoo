package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "action_logs")
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer tickNumber;

    @Column(nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String faction;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String result;

    private String judgeId;

    private Double successRate;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}

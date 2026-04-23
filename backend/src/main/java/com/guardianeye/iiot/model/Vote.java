package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "votes")
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer tickNumber;  // 投票回合

    @Column(nullable = false)
    private String agentName;  // 投票者

    @Column(nullable = false)
    private Integer declarationTick;  // 对应的宣言回合

    @Column(nullable = false)
    private Boolean voteResult;  // true=同意, false=拒绝

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

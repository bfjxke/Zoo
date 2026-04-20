package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "leader_values")
public class LeaderValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String faction;
}

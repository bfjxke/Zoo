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
}

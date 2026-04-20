package com.guardianeye.iiot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
    java.util.List<ActionLog> findByTickNumberOrderByTimestampAsc(Integer tickNumber);
    java.util.List<ActionLog> findByAgentNameOrderByTimestampDesc(String agentName);
}

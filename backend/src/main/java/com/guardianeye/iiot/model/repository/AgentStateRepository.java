package com.guardianeye.iiot.model.repository;

import com.guardianeye.iiot.model.AgentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AgentStateRepository extends JpaRepository<AgentState, Long> {
    
    List<AgentState> findByTickNumber(Integer tickNumber);
    
    List<AgentState> findByAgentIdOrderByTickNumberDesc(Long agentId);
    
    @Query("SELECT a FROM AgentState a WHERE a.agentId = :agentId AND a.tickNumber > :tick ORDER BY a.tickNumber DESC")
    List<AgentState> findRecentByAgentId(@Param("agentId") Long agentId, @Param("tick") Integer tick);
}

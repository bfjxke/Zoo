package com.guardianeye.iiot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    java.util.List<Agent> findByFaction(String faction);
    java.util.List<Agent> findByAliveTrue();
    java.util.Optional<Agent> findByName(String name);
    java.util.List<Agent> findByRole(String role);
}

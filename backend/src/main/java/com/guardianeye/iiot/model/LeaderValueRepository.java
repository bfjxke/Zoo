package com.guardianeye.iiot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderValueRepository extends JpaRepository<LeaderValue, Long> {
    java.util.List<LeaderValue> findByFaction(String faction);
}

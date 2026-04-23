package com.guardianeye.iiot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    // 根据宣言回合查找投票记录
    List<Vote> findByDeclarationTick(Integer declarationTick);
    // 根据投票回合查找投票记录
    List<Vote> findByTickNumber(Integer tickNumber);
}

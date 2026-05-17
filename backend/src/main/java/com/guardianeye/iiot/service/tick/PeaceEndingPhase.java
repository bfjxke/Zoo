package com.guardianeye.iiot.service.tick;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.AgentRepository;
import com.guardianeye.iiot.model.GameState;
import com.guardianeye.iiot.model.Vote;
import com.guardianeye.iiot.model.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PeaceEndingPhase implements TickPhase {
    
    private final AgentRepository agentRepository;
    private final VoteRepository voteRepository;
    
    @Override
    public void execute(GameState gameState, int tick) {
        if (!gameState.getOrderDeclarationActive()) {
            return;
        }
        
        if (gameState.getCurrentTick() < 40) {
            return;
        }
        
        List<Agent> allAgents = agentRepository.findAll();
        boolean lawfulAlive = allAgents.stream().anyMatch(a -> "lawful".equals(a.getFaction()) && a.getAlive());
        boolean aggressiveAlive = allAgents.stream().anyMatch(a -> "aggressive".equals(a.getFaction()) && a.getAlive());
        boolean neutralAlive = allAgents.stream().anyMatch(a -> "neutral".equals(a.getFaction()) && a.getAlive());
        
        if (!lawfulAlive || !aggressiveAlive || !neutralAlive) {
            return;
        }
        
        if (gameState.getOrderSwordHolderId() == null) {
            return;
        }
        
        List<Vote> votes = voteRepository.findByDeclarationTick(gameState.getLastDeclarationTick());
        if (votes.isEmpty()) {
            return;
        }
        
        long agreeCount = votes.stream().filter(Vote::getVoteResult).count();
        long totalCount = votes.size();
        double agreeRate = (double) agreeCount / totalCount;
        
        if (agreeRate <= 0.5) {
            return;
        }
        
        log.info("========== [和平结局] 守序阵营胜利！激进阵营 {}% 同意 ==========", (int)(agreeRate * 100));
        gameState.setRunning(false);
    }
    
    @Override
    public int getOrder() {
        return 4;
    }
    
    @Override
    public String getName() {
        return "peaceEnding";
    }
}
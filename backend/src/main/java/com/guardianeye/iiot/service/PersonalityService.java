package com.guardianeye.iiot.service;

import com.guardianeye.iiot.model.Agent;
import com.guardianeye.iiot.model.PersonalityTraits;
import com.guardianeye.iiot.model.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalityService {
    
    private final AgentRepository agentRepository;
    private final Random random = new Random();
    
    public void assignRandomPersonality(Agent agent) {
        List<PersonalityTraits> factionTraits = PersonalityTraits.getTraitsByFaction(agent.getFaction());
        PersonalityTraits selected = factionTraits.get(random.nextInt(factionTraits.size()));
        
        agent.setPersonality(selected.getId());
        agentRepository.save(agent);
        
        log.info("[性格分配] {} ({}阵营) 获得了性格: {} - {}",
            agent.getName(),
            agent.getFaction(),
            selected.getEmoji(),
            selected.getName());
    }
    
    public void assignRandomPersonalityToAll() {
        List<Agent> allAgents = agentRepository.findAll();
        
        for (Agent agent : allAgents) {
            if (agent.getPersonality() == null || agent.getPersonality().isEmpty()) {
                assignRandomPersonality(agent);
            }
        }
        
        log.info("[性格分配] 已为所有 {} 个Agent分配性格", allAgents.size());
    }
    
    public String generatePersonalityDescription(Agent agent) {
        PersonalityTraits trait = agent.getPersonalityTrait();
        if (trait == null) {
            return "一个普通的动物";
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append(agent.getName()).append("的性格是「")
            .append(trait.getEmoji()).append(trait.getName()).append("」：")
            .append(trait.getDescription()).append("。");
        
        desc.append("行为倾向：");
        for (int i = 0; i < Math.min(3, trait.getTendencies().size()); i++) {
            if (i > 0) desc.append("、");
            desc.append(trait.getTendencies().get(i));
        }
        desc.append("。");
        
        return desc.toString();
    }
}

package com.example.jobservice.match.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "candidate.skills.stub", havingValue = "true")
public class StubCandidateSkillClient implements CandidateSkillClient {

    private final Map<UUID, List<CandidateSkillScore>> skillsMap = new ConcurrentHashMap<>();

    public void setSkills(UUID candidateUserId, List<CandidateSkillScore> skills) {
        skillsMap.put(candidateUserId, skills != null ? List.copyOf(skills) : Collections.emptyList());
    }

    public void clear() {
        skillsMap.clear();
    }

    @Override
    public List<CandidateSkillScore> getSkills(UUID candidateUserId) {
        return skillsMap.getOrDefault(candidateUserId, Collections.emptyList());
    }
}


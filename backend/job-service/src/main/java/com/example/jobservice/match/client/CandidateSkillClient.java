package com.example.jobservice.match.client;

import java.util.List;
import java.util.UUID;

public interface CandidateSkillClient {

    List<CandidateSkillScore> getSkills(UUID candidateUserId);
}


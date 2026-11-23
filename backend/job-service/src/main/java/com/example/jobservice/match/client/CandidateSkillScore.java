package com.example.jobservice.match.client;

public record CandidateSkillScore(
        String skillId,
        String skillCode,
        String skillName,
        Integer verifiedScore,
        Integer selfScore
) {
}


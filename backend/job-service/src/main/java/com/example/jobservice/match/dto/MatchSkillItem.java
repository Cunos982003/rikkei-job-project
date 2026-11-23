package com.example.jobservice.match.dto;

public record MatchSkillItem(
        String skill,
        String skillCode,
        boolean required,
        int minScore,
        int candidateScore,
        int gap
) {
}


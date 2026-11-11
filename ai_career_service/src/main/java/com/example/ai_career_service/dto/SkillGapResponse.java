package com.example.ai_career_service.dto;

public record SkillGapResponse(
        String skillName,
        Integer verifiedIndex,
        int requiredIndex,
        int gap,
        boolean required) {
}

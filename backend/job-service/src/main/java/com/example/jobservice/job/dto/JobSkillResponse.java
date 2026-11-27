package com.example.jobservice.job.dto;

public record JobSkillResponse(
        String skillId,
        String skillCode,
        String skillName,
        boolean required,
        boolean isMandatory,
        int minScore
) {
    public JobSkillResponse(String skillId, String skillCode, String skillName, boolean required, int minScore) {
        this(skillId, skillCode, skillName, required, required, minScore);
    }
}


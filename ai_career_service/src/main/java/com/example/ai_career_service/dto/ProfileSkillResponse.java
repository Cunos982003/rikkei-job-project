package com.example.ai_career_service.dto;

import java.util.List;
import java.util.UUID;

public record ProfileSkillResponse(
        UUID skillId,
        String skillCode,
        String skillName,
        String group,
        Short verifiedScore,
        Short selfScore,
        String level,
        List<Object> evidences) {
}

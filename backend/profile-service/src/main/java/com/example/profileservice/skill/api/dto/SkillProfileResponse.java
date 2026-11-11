package com.example.profileservice.skill.api.dto;

import java.util.List;
import java.util.UUID;

public record SkillProfileResponse(
        UUID skillId,
        String skillCode,
        String skillName,
        String group,
        Short verifiedScore,
        Short selfScore,
        String level,
        List<SkillEvidenceResponse> evidences
) {}
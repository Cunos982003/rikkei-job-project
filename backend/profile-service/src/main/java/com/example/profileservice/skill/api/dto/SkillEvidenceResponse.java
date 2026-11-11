package com.example.profileservice.skill.api.dto;

import java.time.OffsetDateTime;

public record SkillEvidenceResponse(
        String source,
        String title,
        short score,
        OffsetDateTime recordedAt,
        boolean verified
) {}
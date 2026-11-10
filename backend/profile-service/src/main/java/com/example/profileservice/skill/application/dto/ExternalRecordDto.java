package com.example.profileservice.skill.application.dto;

import java.time.OffsetDateTime;
import java.io.Serializable;

public record ExternalRecordDto(
        String source,
        String sourceCode,
        String title,
        Short score,
        OffsetDateTime recordedAt
) implements Serializable {}
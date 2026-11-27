package com.example.jobservice.match.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MatchResultResponse(
        UUID jobId,
        UUID userId,
        double score,
        boolean belowMinimum,
        List<MatchSkillItem> met,
        List<MatchSkillItem> missing,
        OffsetDateTime computedAt,
        boolean stale
) {
}


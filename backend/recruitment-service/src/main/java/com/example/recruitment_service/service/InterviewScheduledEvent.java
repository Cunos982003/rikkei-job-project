package com.example.recruitment_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewScheduledEvent(
        UUID scheduleId,
        UUID roundId,
        UUID recruitmentId,
        String roundName,
        String candidateEmail,
        List<String> hrRecipients,
        Instant startAt,
        Instant endAt,
        String meetUrl,
        String icsContent) {
}

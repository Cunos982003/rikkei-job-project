package com.example.recruitment_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewReminderEvent(
        UUID scheduleId,
        UUID roundId,
        String roundName,
        String milestone,
        String candidateEmail,
        List<String> hrRecipients,
        Instant startAt,
        Instant endAt,
        String meetUrl) {
}

package com.example.recruitment_service.dto;

import java.time.Instant;

public record InterviewSlotInput(
        Instant startAt,
        Instant endAt) {
}

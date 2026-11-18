package com.example.recruitment_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.enums.InterviewSlotStatus;

public record InterviewSlotResponse(
        UUID id,
        UUID roundId,
        Instant startAt,
        Instant endAt,
        InterviewSlotStatus status) {

    public static InterviewSlotResponse from(InterviewSlot slot) {
        return new InterviewSlotResponse(
                slot.getId(),
                slot.getRound().getId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getStatus());
    }
}

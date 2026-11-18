package com.example.recruitment_service.dto;

import java.util.List;
import java.util.UUID;

public record RescheduleResponse(
        UUID roundId,
        int cancelledSchedulesCount,
        int cancelledSlotsCount,
        List<InterviewSlotResponse> slots) {
}

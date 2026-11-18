package com.example.recruitment_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PickInterviewSlotRequest(
        @NotNull(message = "slotId is required")
        UUID slotId) {
}

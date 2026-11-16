package com.example.recruitment_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @NotNull UUID cvId,
        @Size(max = 5000) String coverLetter) {
}

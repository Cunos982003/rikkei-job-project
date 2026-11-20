package com.example.recruitment_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.Size;

public record ApplyRequest(
        UUID cvId,
        @Size(max = 5000) String coverLetter) {
}

package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInterviewRoundRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String interviewerEmail,
        LocalDateTime scheduledAt) {
}

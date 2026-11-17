package com.example.recruitment_service.dto;

import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull RecruitmentStatus toStatus,
        @Size(max = 1000) String note) {
}

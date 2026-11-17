package com.example.recruitment_service.dto;

import com.example.recruitment_service.enums.InterviewRoundResult;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewRoundRequest(
        @NotNull InterviewRoundResult result,
        @Min(0) @Max(100) Integer score) {
}

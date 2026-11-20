package com.example.ai_career_service.dto.interview;

import jakarta.validation.constraints.NotBlank;

public record SubmitAnswerRequest(
        @NotBlank(message = "Câu trả lời không được để trống")
        String answer
) {}

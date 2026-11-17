package com.example.recruitment_service.dto;

import java.time.LocalDateTime;

public record CreateInterviewRoundRequest(LocalDateTime scheduledAt) {
}

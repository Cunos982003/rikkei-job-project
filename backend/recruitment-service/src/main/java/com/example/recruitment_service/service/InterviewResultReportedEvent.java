package com.example.recruitment_service.service;

import java.util.UUID;

import com.example.recruitment_service.enums.InterviewRoundResult;

public record InterviewResultReportedEvent(
        UUID recruitmentId,
        UUID roundId,
        Integer roundNo,
        String roundName,
        String candidateEmail,
        InterviewRoundResult result,
        Integer score,
        String note) {
}

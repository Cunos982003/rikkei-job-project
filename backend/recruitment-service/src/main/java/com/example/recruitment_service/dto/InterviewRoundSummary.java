package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.enums.InterviewRoundResult;

public record InterviewRoundSummary(
        UUID id,
        Integer roundNo,
        LocalDateTime scheduledAt,
        InterviewRoundResult result,
        Integer score) {

    public static InterviewRoundSummary from(InterviewRound round) {
        return new InterviewRoundSummary(
                round.getId(),
                round.getRoundNo(),
                round.getScheduledAt(),
                round.getResult(),
                round.getScore());
    }
}

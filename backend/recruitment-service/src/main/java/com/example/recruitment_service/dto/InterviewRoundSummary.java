package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.enums.InterviewRoundResult;

public record InterviewRoundSummary(
        UUID id,
        Integer roundNo,
        String name,
        String interviewerEmail,
        LocalDateTime scheduledAt,
        InterviewRoundResult result,
        Integer score,
        String note,
        List<InterviewSlotResponse> slots,
        InterviewScheduleResponse schedule) {

    public InterviewRoundSummary(
            UUID id,
            Integer roundNo,
            String name,
            String interviewerEmail,
            LocalDateTime scheduledAt,
            InterviewRoundResult result,
            Integer score,
            String note) {
        this(id, roundNo, name, interviewerEmail, scheduledAt, result, score, note, List.of(), null);
    }

    public static InterviewRoundSummary from(InterviewRound round) {
        return from(round, List.of(), null);
    }

    public static InterviewRoundSummary from(
            InterviewRound round,
            List<InterviewSlotResponse> slots,
            InterviewScheduleResponse schedule) {
        return new InterviewRoundSummary(
                round.getId(),
                round.getRoundNo(),
                round.getName(),
                round.getInterviewerEmail(),
                round.getScheduledAt(),
                round.getResult(),
                round.getScore(),
                round.getNote(),
                slots != null ? slots : List.of(),
                schedule);
    }
}

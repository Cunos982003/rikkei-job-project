package com.example.recruitment_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.enums.InterviewScheduleStatus;

public record InterviewScheduleResponse(
        UUID id,
        UUID roundId,
        UUID slotId,
        Instant startAt,
        Instant endAt,
        String meetUrl,
        Instant reminded24hAt,
        Instant reminded1hAt,
        InterviewScheduleStatus status) {

    public static InterviewScheduleResponse from(InterviewSchedule schedule) {
        return new InterviewScheduleResponse(
                schedule.getId(),
                schedule.getRound().getId(),
                schedule.getSlot().getId(),
                schedule.getSlot().getStartAt(),
                schedule.getSlot().getEndAt(),
                schedule.getMeetUrl(),
                schedule.getReminded24hAt(),
                schedule.getReminded1hAt(),
                schedule.getStatus());
    }
}

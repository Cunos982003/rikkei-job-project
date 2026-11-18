package com.example.recruitment_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.enums.InterviewScheduleStatus;

public record CompanyInterviewResponse(
        UUID scheduleId,
        UUID roundId,
        UUID recruitmentId,
        UUID jobId,
        UUID candidateId,
        String candidateEmail,
        Integer roundNo,
        String roundName,
        String interviewerEmail,
        Instant startAt,
        Instant endAt,
        String meetUrl,
        InterviewScheduleStatus status,
        Instant reminded24hAt,
        Instant reminded1hAt) {

    public static CompanyInterviewResponse from(InterviewSchedule schedule) {
        var round = schedule.getRound();
        var recruitment = round.getRecruitment();
        var slot = schedule.getSlot();
        return new CompanyInterviewResponse(
                schedule.getId(),
                round.getId(),
                recruitment.getId(),
                recruitment.getJob_id(),
                recruitment.getUser_id(),
                recruitment.getCandidate_email(),
                round.getRoundNo(),
                round.getName(),
                round.getInterviewerEmail(),
                slot.getStartAt(),
                slot.getEndAt(),
                schedule.getMeetUrl(),
                schedule.getStatus(),
                schedule.getReminded24hAt(),
                schedule.getReminded1hAt());
    }
}

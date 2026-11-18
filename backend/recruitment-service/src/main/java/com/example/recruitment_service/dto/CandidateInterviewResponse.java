package com.example.recruitment_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.RecruitmentStatus;

public record CandidateInterviewResponse(
        UUID roundId,
        UUID recruitmentId,
        UUID jobId,
        UUID companyId,
        Integer roundNo,
        String roundName,
        String interviewerEmail,
        InterviewRoundResult result,
        Integer score,
        String note,
        RecruitmentStatus applicationStatus,
        List<InterviewSlotResponse> slots,
        InterviewScheduleResponse schedule) {
}

package com.example.recruitment_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.recruitment_service.enums.InterviewRoundResult;

public record CandidateInterviewResponse(
        UUID roundId,
        UUID recruitmentId,
        UUID jobId,
        UUID companyId,
        Integer roundNo,
        String roundName,
        String interviewerEmail,
        InterviewRoundResult result,
        List<InterviewSlotResponse> slots,
        InterviewScheduleResponse schedule) {
}

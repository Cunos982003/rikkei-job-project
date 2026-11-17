package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.RecruitmentStatus;

public record RecruitmentStaffSummary(
        UUID id,
        UUID jobId,
        UUID candidateId,
        String candidateEmail,
        RecruitmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static RecruitmentStaffSummary from(Recruitment recruitment) {
        return new RecruitmentStaffSummary(
                recruitment.getId(),
                recruitment.getJob_id(),
                recruitment.getUser_id(),
                recruitment.getCandidate_email(),
                recruitment.getStatus(),
                recruitment.getCreated_at(),
                recruitment.getUpdated_at());
    }
}

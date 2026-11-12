package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.RecruitmentStatus;

public record RecruitmentSummary(
        UUID id,
        UUID jobId,
        UUID cvId,
        String cvSnapshotUrl,
        String coverLetter,
        RecruitmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static RecruitmentSummary from(Recruitment recruitment) {
        return new RecruitmentSummary(
                recruitment.getId(),
                recruitment.getJob_id(),
                recruitment.getCv_id(),
                recruitment.getCv_snapshot_url(),
                recruitment.getCover_letter(),
                recruitment.getStatus(),
                recruitment.getCreated_at(),
                recruitment.getUpdated_at());
    }
}

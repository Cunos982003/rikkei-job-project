package com.example.recruitment_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.RecruitmentStatus;

public record RecruitmentHistoryItem(
        UUID id,
        RecruitmentStatus fromStatus,
        RecruitmentStatus toStatus,
        UUID actorId,
        String note,
        LocalDateTime createdAt) {

    public static RecruitmentHistoryItem from(RecruitmentStatusHistory history) {
        return new RecruitmentHistoryItem(
                history.getId(),
                history.getFrom_status(),
                history.getTo_status(),
                history.getActor_id(),
                history.getNote(),
                history.getCreated_at());
    }
}

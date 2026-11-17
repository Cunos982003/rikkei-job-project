package com.example.recruitment_service.service;

import java.util.UUID;

import com.example.recruitment_service.enums.RecruitmentStatus;

public record RecruitmentStatusChangedEvent(
        UUID recruitmentId,
        String candidateEmail,
        RecruitmentStatus fromStatus,
        RecruitmentStatus toStatus) {
}

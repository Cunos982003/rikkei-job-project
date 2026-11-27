package com.example.recruitment_service.service;

import java.util.List;
import java.util.UUID;

public record RecruitmentSubmittedEvent(
        UUID recruitmentId,
        UUID jobId,
        List<String> hrEmails) {
}

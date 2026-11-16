package com.example.recruitment_service.dto;

import java.util.List;

public record RecruitmentDetails(
        RecruitmentSummary recruitment,
        List<RecruitmentHistoryItem> history) {
}

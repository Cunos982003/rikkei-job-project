package com.example.recruitment_service.dto;

import java.util.List;
import java.util.UUID;

public record RecruitmentReviewDetails(
        RecruitmentSummary recruitment,
        UUID candidateId,
        String candidateEmail,
        String coverLetter,
        String cvSnapshotUrl,
        double score,
        List<InterviewRoundSummary> rounds,
        List<RecruitmentHistoryItem> history,
        OfferResponse offer,
        EmploymentRecordResponse employmentRecord) {
    public RecruitmentReviewDetails(
            RecruitmentSummary recruitment,
            UUID candidateId,
            String candidateEmail,
            String coverLetter,
            String cvSnapshotUrl,
            double score,
            List<InterviewRoundSummary> rounds,
            List<RecruitmentHistoryItem> history) {
        this(recruitment, candidateId, candidateEmail, coverLetter, cvSnapshotUrl, score, rounds, history, null, null);
    }
}

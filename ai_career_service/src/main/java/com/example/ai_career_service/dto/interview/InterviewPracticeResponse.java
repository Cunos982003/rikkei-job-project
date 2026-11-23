package com.example.ai_career_service.dto.interview;

import com.example.ai_career_service.model.InterviewPractice;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewPracticeResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String status,
        Double overallScore,
        String summary,
        Instant createdAt,
        List<PracticeQuestionResponse> questions
) {
    public static InterviewPracticeResponse from(InterviewPractice p) {
        return new InterviewPracticeResponse(
                p.getId(),
                p.getJobId(),
                p.getJobTitle(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getOverallScore(),
                p.getSummary(),
                p.getCreatedAt(),
                p.getQuestions() != null ? p.getQuestions().stream().map(PracticeQuestionResponse::from).toList() : List.of()
        );
    }
}

package com.example.ai_career_service.dto.interview;

import com.example.ai_career_service.model.PracticeQuestion;
import java.util.UUID;

public record PracticeQuestionResponse(
        UUID id,
        int qOrder,
        String question,
        String type,
        String answer,
        Integer score,
        String feedback,
        String suggestion
) {
    public static PracticeQuestionResponse from(PracticeQuestion q) {
        return new PracticeQuestionResponse(
                q.getId(),
                q.getQOrder(),
                q.getQuestion(),
                q.getType() != null ? q.getType().name() : null,
                q.getAnswer(),
                q.getScore(),
                q.getFeedback(),
                q.getSuggestion()
        );
    }
}

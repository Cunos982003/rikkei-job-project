package com.example.ai_career_service.dto.chat;

import com.example.ai_career_service.model.ChatConversation;
import com.example.ai_career_service.model.ConversationMode;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        ConversationMode mode,
        UUID jobId,
        UUID practiceId,
        Instant createdAt
) {
    public static ConversationResponse from(ChatConversation c) {
        return new ConversationResponse(
                c.getId(),
                c.getTitle(),
                c.getMode(),
                c.getJobId(),
                c.getPracticeId(),
                c.getCreatedAt()
        );
    }
}

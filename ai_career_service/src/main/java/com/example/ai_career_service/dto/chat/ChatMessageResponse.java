package com.example.ai_career_service.dto.chat;

import com.example.ai_career_service.model.ChatMessage;
import com.example.ai_career_service.model.MessageRole;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID conversationId,
        MessageRole role,
        String content,
        int tokens,
        Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getRole(),
                m.getContent(),
                m.getTokens(),
                m.getCreatedAt()
        );
    }
}

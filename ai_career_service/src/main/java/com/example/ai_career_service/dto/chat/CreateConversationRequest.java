package com.example.ai_career_service.dto.chat;

import com.example.ai_career_service.model.ConversationMode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateConversationRequest(
        @NotNull(message = "Chế độ trò chuyện không được để trống")
        ConversationMode mode,
        UUID jobId,
        String title
) {}

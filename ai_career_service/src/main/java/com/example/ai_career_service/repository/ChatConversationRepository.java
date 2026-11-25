package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.ChatConversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    List<ChatConversation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ChatConversation> findByIdAndUserId(UUID id, UUID userId);
}

package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.AIDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIDraftRepository extends JpaRepository<AIDraft, UUID> {

    Optional<AIDraft> findByIdAndUserId(UUID id, UUID userId);

    List<AIDraft> findAllByCvIdAndUserIdOrderByCreatedAtDesc(UUID cvId, UUID userId);
}

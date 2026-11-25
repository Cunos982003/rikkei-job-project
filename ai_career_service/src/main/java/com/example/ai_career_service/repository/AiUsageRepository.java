package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.AiUsage;
import com.example.ai_career_service.model.AiUsageId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageRepository extends JpaRepository<AiUsage, AiUsageId> {
    Optional<AiUsage> findByIdUserIdAndIdDate(UUID userId, LocalDate date);
}

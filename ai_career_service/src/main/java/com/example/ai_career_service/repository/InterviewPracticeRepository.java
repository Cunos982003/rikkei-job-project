package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.InterviewPractice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewPracticeRepository extends JpaRepository<InterviewPractice, UUID> {
    List<InterviewPractice> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<InterviewPractice> findByIdAndUserId(UUID id, UUID userId);
}

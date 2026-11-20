package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.PracticeQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, UUID> {
    List<PracticeQuestion> findByPracticeIdOrderByQOrderAsc(UUID practiceId);
}

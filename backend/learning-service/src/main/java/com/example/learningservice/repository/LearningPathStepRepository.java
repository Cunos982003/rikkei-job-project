package com.example.learningservice.repository;

import com.example.learningservice.model.entity.LearningPathStep;
import com.example.learningservice.model.entity.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningPathStepRepository extends JpaRepository<LearningPathStep, UUID> {

    List<LearningPathStep> findByLearningPathIdOrderByStepOrderAsc(UUID pathId);

    List<LearningPathStep> findByLearningPathIdAndStatus(UUID pathId, StepStatus status);

    Optional<LearningPathStep> findByLearningPathIdAndLmsCourseIdAndSkillId(
            UUID pathId,
            String lmsCourseId,
            String skillId
    );

    List<LearningPathStep> findByLearningPathIdAndLmsCourseId(UUID pathId, String lmsCourseId);
}


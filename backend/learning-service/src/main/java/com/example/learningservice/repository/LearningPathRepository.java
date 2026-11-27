package com.example.learningservice.repository;

import com.example.learningservice.model.entity.LearningPath;
import com.example.learningservice.model.entity.LearningPathStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    Optional<LearningPath> findByUserIdAndStatus(UUID userId, LearningPathStatus status);

    List<LearningPath> findAllByUserIdAndStatus(UUID userId, LearningPathStatus status);

    List<LearningPath> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE LearningPath lp SET lp.status = :newStatus WHERE lp.userId = :userId AND lp.status = :currentStatus")
    int updateStatusByUserIdAndCurrentStatus(
            @Param("userId") UUID userId,
            @Param("currentStatus") LearningPathStatus currentStatus,
            @Param("newStatus") LearningPathStatus newStatus
    );
}


package com.example.learningservice.repository;

import com.example.learningservice.model.entity.GapResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GapResultRepository extends JpaRepository<GapResult, UUID> {

    List<GapResult> findByUserIdAndTargetIdOrderByGapDesc(UUID userId, String targetId);

    @Modifying
    @Query("DELETE FROM GapResult g WHERE g.userId = :userId AND g.targetId = :targetId")
    void deleteByUserIdAndTargetId(@Param("userId") UUID userId, @Param("targetId") String targetId);
}


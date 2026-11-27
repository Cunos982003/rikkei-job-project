package com.example.learningservice.repository;

import com.example.learningservice.model.entity.GapResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface GapResultRepository extends JpaRepository<GapResult, UUID> {

    List<GapResult> findByUserId(UUID userId);

    List<GapResult> findByUserIdIn(Collection<UUID> userIds);

    List<GapResult> findByUserIdAndTargetIdOrderByGapDesc(UUID userId, String targetId);

    @Query("SELECT g.skillId, g.skillName, COUNT(g) FROM GapResult g WHERE g.gap > 0 GROUP BY g.skillId, g.skillName ORDER BY COUNT(g) DESC, g.skillName ASC")
    List<Object[]> findTopMissingSkills();

    @Modifying
    @Query("DELETE FROM GapResult g WHERE g.userId = :userId AND g.targetId = :targetId")
    void deleteByUserIdAndTargetId(@Param("userId") UUID userId, @Param("targetId") String targetId);
}


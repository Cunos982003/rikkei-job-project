package com.example.profileservice.skill.infrastructure;

import com.example.profileservice.skill.domain.SkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, UUID> {
    List<SkillEvidence> findByUserIdAndSkillId(UUID userId, UUID skillId);
    List<SkillEvidence> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM SkillEvidence WHERE userId = :userId AND skill.id = :skillId AND source = :source AND sourceRef = :sourceRef")
    int deleteByUserSkillSourceRef(
        @Param("userId") UUID userId,
        @Param("skillId") UUID skillId,
        @Param("source") String source,
        @Param("sourceRef") String sourceRef
    );

    // Upsert via save() — unique constraint handles idempotency
}
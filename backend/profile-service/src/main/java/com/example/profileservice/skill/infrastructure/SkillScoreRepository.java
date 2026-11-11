package com.example.profileservice.skill.infrastructure;

import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.domain.SkillScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillScoreRepository extends JpaRepository<SkillScore, SkillScoreId> {
    List<SkillScore> findByUserId(UUID userId);
    Optional<SkillScore> findByUserIdAndSkillId(UUID userId, UUID skillId);
}
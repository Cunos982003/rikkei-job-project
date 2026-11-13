package com.example.learningservice.repository;

import com.example.learningservice.model.entity.PositionSkill;
import com.example.learningservice.model.entity.PositionSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionSkillRepository extends JpaRepository<PositionSkill, PositionSkillId> {

    List<PositionSkill> findByTargetPositionId(UUID positionId);
}


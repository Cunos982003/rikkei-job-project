package com.example.profileservice.skill.infrastructure;

import com.example.profileservice.skill.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByGroup(String group);
    Optional<Skill> findByCode(String code);
}
package com.example.profileservice.repository;

import com.example.profileservice.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByGroup(String group);
    Optional<Skill> findByCode(String code);
}
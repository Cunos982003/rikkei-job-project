package com.example.profileservice.repository;

import com.example.profileservice.entity.SkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SkillMappingRepository extends JpaRepository<SkillMapping, UUID> {
    List<SkillMapping> findBySourceAndSourceCode(String source, String sourceCode);
}
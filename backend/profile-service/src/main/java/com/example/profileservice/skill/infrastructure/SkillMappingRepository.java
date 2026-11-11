package com.example.profileservice.skill.infrastructure;

import com.example.profileservice.skill.domain.SkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SkillMappingRepository extends JpaRepository<SkillMapping, UUID> {
    List<SkillMapping> findBySourceAndSourceCode(String source, String sourceCode);
}
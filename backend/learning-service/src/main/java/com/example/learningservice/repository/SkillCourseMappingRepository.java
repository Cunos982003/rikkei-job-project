package com.example.learningservice.repository;

import com.example.learningservice.model.entity.SkillCourseMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillCourseMappingRepository extends JpaRepository<SkillCourseMapping, UUID> {

    List<SkillCourseMapping> findBySkillId(String skillId);

    List<SkillCourseMapping> findBySkillIdIn(Collection<String> skillIds);

    Optional<SkillCourseMapping> findBySkillIdAndLmsCourseId(String skillId, String lmsCourseId);
}


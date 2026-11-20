package com.example.jobservice.job.catalog;

import java.util.List;
import java.util.Optional;

public interface SkillCatalogClient {

    List<CatalogSkill> listSkills();

    Optional<CatalogSkill> findByIdOrCode(String idOrCode);
}


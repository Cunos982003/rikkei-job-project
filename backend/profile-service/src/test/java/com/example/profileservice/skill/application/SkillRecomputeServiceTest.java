package com.example.profileservice.skill.application;

import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.domain.SkillScoreId;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillMappingRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class SkillRecomputeServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("profile_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SkillRecomputeService service;

    @Autowired
    SkillMappingRepository mappingRepo;

    @Autowired
    SkillEvidenceRepository evidenceRepo;

    @Autowired
    SkillScoreRepository scoreRepo;

    @Autowired
    SkillRepository skillRepo;

    @Test
    void recomputeSkills_createsVerifiedEvidencesAndCalculatesScore() {
        // Given: Create a user and seed some skills
        UUID userId = UUID.randomUUID();

        // Create external records for JAVA01=80, SPRING01=70
        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Course", (short) 80, OffsetDateTime.now().minusDays(30)),
                new ExternalRecordDto("LMS", "SPRING01", "Spring Boot Course", (short) 70, OffsetDateTime.now().minusDays(60))
        );

        // When: recompute skills
        service.recomputeSkills(userId, externalRecords);

        // Then: skill_evidences should have 4 rows (JAVA_CORE x2, SPRING_BOOT x1, SQL x1)
        List<SkillEvidence> evidences = evidenceRepo.findByUserId(userId);
        assertThat(evidences).hasSize(4);

        // Verify specific skills have evidences
        Skill javaCore = skillRepo.findByCode("JAVA_CORE").orElseThrow();
        Skill springBoot = skillRepo.findByCode("SPRING_BOOT").orElseThrow();
        Skill sql = skillRepo.findByCode("SQL").orElseThrow();

        List<SkillEvidence> javaEvidences = evidenceRepo.findByUserIdAndSkillId(userId, javaCore.getId());
        assertThat(javaEvidences).hasSize(2); // JAVA01 maps to JAVA_CORE (0.7) and SQL (0.3)

        List<SkillEvidence> springEvidences = evidenceRepo.findByUserIdAndSkillId(userId, springBoot.getId());
        assertThat(springEvidences).hasSize(1); // SPRING01 maps to SPRING_BOOT (0.8)

        List<SkillEvidence> sqlEvidences = evidenceRepo.findByUserIdAndSkillId(userId, sql.getId());
        assertThat(sqlEvidences).hasSize(1); // JAVA01 maps to SQL (0.3)

        // And: skill_scores should have verified_score calculated
        SkillScore javaScore = scoreRepo.findByUserIdAndSkillId(userId, javaCore.getId()).orElseThrow();
        assertThat(javaScore.getVerifiedScore()).isNotNull();
        assertThat(javaScore.getVerifiedScore()).isBetween((short) 70, (short) 85); // Weighted average with decay

        SkillScore springScore = scoreRepo.findByUserIdAndSkillId(userId, springBoot.getId()).orElseThrow();
        assertThat(springScore.getVerifiedScore()).isNotNull();

        SkillScore sqlScore = scoreRepo.findByUserIdAndSkillId(userId, sql.getId()).orElseThrow();
        assertThat(sqlScore.getVerifiedScore()).isNotNull();
    }

    @Test
    void recomputeSkills_selfOnlySkill_hasNullVerifiedScore() {
        UUID userId = UUID.randomUUID();

        // Create a self-reported evidence directly (not via recompute)
        Skill skill = skillRepo.findByCode("JAVA_CORE").orElseThrow();
        SkillEvidence selfEvidence = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source("SELF")
                .sourceRef("SELF_JAVA")
                .title("Self-reported Java")
                .score((short) 90)
                .recordedAt(OffsetDateTime.now())
                .verified(false)
                .build();
        evidenceRepo.save(selfEvidence);

        // Recompute with no external records
        service.recomputeSkills(userId, List.of());

        // Should have selfScore but null verifiedScore
        SkillScore score = scoreRepo.findByUserIdAndSkillId(userId, skill.getId()).orElseThrow();
        assertThat(score.getVerifiedScore()).isNull();
        assertThat(score.getSelfScore()).isEqualTo((short) 90);
        assertThat(score.getLevel()).isEqualTo("Nâng cao"); // 90 >= 85
    }

    @Test
    void recomputeSkills_idempotent_noDuplicatesOnRerun() {
        UUID userId = UUID.randomUUID();

        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Course", (short) 80, OffsetDateTime.now().minusDays(30))
        );

        // First recompute
        service.recomputeSkills(userId, externalRecords);

        // Second recompute with same data
        service.recomputeSkills(userId, externalRecords);

        // Should not create duplicates
        Skill javaCore = skillRepo.findByCode("JAVA_CORE").orElseThrow();
        List<SkillEvidence> evidences = evidenceRepo.findByUserIdAndSkillId(userId, javaCore.getId());
        assertThat(evidences).hasSize(1); // Only one evidence per unique key
    }

    @Test
    void recomputeSkills_timeDecay_boundary365vs366() {
        UUID userId = UUID.randomUUID();

        // Evidence within 1 year (365 days) - should have decay 1.0
        OffsetDateTime withinYear = OffsetDateTime.now().minusDays(364);
        List<ExternalRecordDto> recentRecords = List.of(
                new ExternalRecordDto("EXAM", "JAVA_MID", "Java Midterm", (short) 80, withinYear)
        );

        service.recomputeSkills(userId, recentRecords);

        Skill javaCore = skillRepo.findByCode("JAVA_CORE").orElseThrow();
        SkillScore recentScore = scoreRepo.findByUserIdAndSkillId(userId, javaCore.getId()).orElseThrow();

        // Evidence older than 1 year (366 days) - should have decay 0.5
        UUID userId2 = UUID.randomUUID();
        OffsetDateTime olderThanYear = OffsetDateTime.now().minusDays(366);
        List<ExternalRecordDto> oldRecords = List.of(
                new ExternalRecordDto("EXAM", "JAVA_MID", "Java Midterm", (short) 80, olderThanYear)
        );

        service.recomputeSkills(userId2, oldRecords);

        SkillScore oldScore = scoreRepo.findByUserIdAndSkillId(userId2, javaCore.getId()).orElseThrow();

        // Recent score should be higher (no decay) than old score (0.5 decay)
        assertThat(recentScore.getVerifiedScore()).isGreaterThan(oldScore.getVerifiedScore());
    }
}
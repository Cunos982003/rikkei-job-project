package com.example.profileservice.skill.domain;

import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class SkillEvidenceRepositoryTest {

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
    SkillEvidenceRepository repo;

    @Autowired
    SkillRepository skillRepo;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Skill createAndSaveSkill(String code, String name, String group) {
        Skill skill = Skill.builder()
                .code(code)
                .name(name)
                .group(group)
                .build();
        return skillRepo.save(skill);
    }

    @Test
    void uniqueConstraint_preventsDuplicateEvidences() {
        UUID userId = UUID.randomUUID();
        Skill skill = createAndSaveSkill("TEST_SKILL", "Test Skill", "Test");

        // First insert
        SkillEvidence ev1 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source("LMS")
                .sourceRef("JAVA01")
                .title("Java Course")
                .score((short) 80)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();
        repo.saveAndFlush(ev1);

        // Second insert with same unique key should fail
        SkillEvidence ev2 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source("LMS")
                .sourceRef("JAVA01")
                .title("Java Course Updated")
                .score((short) 90)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();

        assertThatThrownBy(() -> repo.saveAndFlush(ev2))
                .isInstanceOf(Exception.class);
    }

    @Test
    void upsertBySourceAndSourceRef_updatesExistingEvidence() {
        UUID userId = UUID.randomUUID();
        Skill skill = createAndSaveSkill("TEST_SKILL_2", "Test Skill 2", "Test");

        // First insert
        SkillEvidence ev1 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source("LMS")
                .sourceRef("JAVA01")
                .title("Java Course")
                .score((short) 80)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();
        repo.save(ev1);

        // Upsert: delete existing then save new
        repo.deleteByUserSkillSourceRef(userId, skill.getId(), "LMS", "JAVA01");

        SkillEvidence ev2 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source("LMS")
                .sourceRef("JAVA01")
                .title("Java Course Updated")
                .score((short) 90)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();
        repo.save(ev2);

        // Should only have one record with updated values
        List<SkillEvidence> evidences = repo.findByUserIdAndSkillId(userId, skill.getId());
        assertThat(evidences).hasSize(1);
        assertThat(evidences.get(0).getScore()).isEqualTo((short) 90);
        assertThat(evidences.get(0).getTitle()).isEqualTo("Java Course Updated");
    }

    @Test
    void findByUserIdAndSkillId_returnsEvidences() {
        UUID userId = UUID.randomUUID();
        Skill skill1 = createAndSaveSkill("TEST_SKILL_1", "Test Skill 1", "Test");
        Skill skill2 = createAndSaveSkill("TEST_SKILL_2", "Test Skill 2", "Test");

        SkillEvidence ev1 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill1)
                .source("LMS")
                .sourceRef("JAVA01")
                .title("Java Course")
                .score((short) 80)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();
        repo.save(ev1);

        SkillEvidence ev2 = SkillEvidence.builder()
                .userId(userId)
                .skill(skill2)
                .source("EXAM")
                .sourceRef("JAVA_MID")
                .title("Java Midterm")
                .score((short) 85)
                .recordedAt(java.time.OffsetDateTime.now())
                .verified(true)
                .build();
        repo.save(ev2);

        List<SkillEvidence> skill1Evidences = repo.findByUserIdAndSkillId(userId, skill1.getId());
        assertThat(skill1Evidences).hasSize(1);
        assertThat(skill1Evidences.get(0).getSource()).isEqualTo("LMS");

        List<SkillEvidence> skill2Evidences = repo.findByUserIdAndSkillId(userId, skill2.getId());
        assertThat(skill2Evidences).hasSize(1);
        assertThat(skill2Evidences.get(0).getSource()).isEqualTo("EXAM");

        // User with no evidences
        List<SkillEvidence> empty = repo.findByUserIdAndSkillId(UUID.randomUUID(), skill1.getId());
        assertThat(empty).isEmpty();
    }
}
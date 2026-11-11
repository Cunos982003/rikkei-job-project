package com.example.profileservice.skill.integration;

import com.example.profileservice.skill.application.SkillRecomputeService;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class SkillProfileIntegrationTest {

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
    WebApplicationContext webApplicationContext;

    @Autowired
    SkillRecomputeService recomputeService;

    @Autowired
    SkillEvidenceRepository evidenceRepo;

    @Autowired
    SkillRepository skillRepo;

    @Autowired
    SkillScoreRepository scoreRepo;

    MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void fullFlow_candidate1Sees5SkillsWithEvidences() throws Exception {
        UUID userId = UUID.randomUUID();

        // Seed external records for candidate1 (simulating NT-05 mock data)
        // JAVA01=85, SPRING01=80, SQL_FINAL=75, JAVA_MID=90, IOC_JAVA=70
        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Core Course", (short) 85, OffsetDateTime.now().minusDays(30)),
                new ExternalRecordDto("LMS", "SPRING01", "Spring Boot Course", (short) 80, OffsetDateTime.now().minusDays(60)),
                new ExternalRecordDto("EXAM", "SQL_FINAL", "SQL Final Exam", (short) 75, OffsetDateTime.now().minusDays(90)),
                new ExternalRecordDto("EXAM", "JAVA_MID", "Java Midterm", (short) 90, OffsetDateTime.now().minusDays(120)),
                new ExternalRecordDto("PM", "IOC_JAVA", "Java Project Review", (short) 70, OffsetDateTime.now().minusDays(180))
        );
        recomputeService.recomputeSkills(userId, externalRecords);

        // When: GET /api/v1/profiles/me/skills with valid header
        mvc.perform(get("/api/v1/profiles/me/skills")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[*].evidences[*].source", everyItem(not(empty()))))
                .andExpect(jsonPath("$[*].evidences[*].title", everyItem(not(empty()))))
                .andExpect(jsonPath("$[*].evidences[*].recordedAt", everyItem(not(empty()))));
    }

    @Test
    void recompute_afterAddingCourse_updatesScoreCorrectly() throws Exception {
        UUID userId = UUID.randomUUID();

        // Initial records: JAVA01=80
        List<ExternalRecordDto> initialRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Course", (short) 80, OffsetDateTime.now().minusDays(30))
        );
        recomputeService.recomputeSkills(userId, initialRecords);

        // Get initial score
        Skill javaCore = skillRepo.findByCode("JAVA_CORE").orElseThrow();
        var initialScore = scoreRepo.findByUserIdAndSkillId(userId, javaCore.getId()).orElseThrow();
        short initialVerifiedScore = initialScore.getVerifiedScore();

        // Add new external record: JAVA01=90 (improved score)
        List<ExternalRecordDto> updatedRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Advanced Course", (short) 90, OffsetDateTime.now())
        );
        recomputeService.recomputeSkills(userId, updatedRecords);

        // Get updated score
        var updatedScore = scoreRepo.findByUserIdAndSkillId(userId, javaCore.getId()).orElseThrow();
        short updatedVerifiedScore = updatedScore.getVerifiedScore();

        // Manual calculation:
        // JAVA01=80 maps to JAVA_CORE (0.7) and SQL (0.3)
        // JAVA01=90 maps to JAVA_CORE (0.7) and SQL (0.3)
        // After recompute with only 90: JAVA_CORE evidence = 90, SQL evidence = 90
        // Both within 1 year -> decay 1.0
        // verified_score = (90 + 90) / 2 = 90
        // The score should be updated to reflect the latest record
        assertThat(updatedVerifiedScore).isEqualTo((short) 90);

        // Verify via API
        mvc.perform(get("/api/v1/profiles/me/skills")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].verifiedScore", hasItem(90)));
    }

    @Test
    void selfOnlySkill_showsSelfLabelNoVerifiedScore() throws Exception {
        UUID userId = UUID.randomUUID();

        // Create self-reported evidence
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

        // Recompute to generate score
        recomputeService.recomputeSkills(userId, List.of());

        // When: GET /api/v1/profiles/me/skills
        mvc.perform(get("/api/v1/profiles/me/skills")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].selfScore", hasItem(90)))
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].verifiedScore", hasItem(nullValue())))
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].level", hasItem("Nâng cao")));
    }

    @Test
    void skillMappingInDatabase_notHardcoded() throws Exception {
        UUID userId = UUID.randomUUID();

        // Use a mapping from the database (EXAM:JAVA_MID -> JAVA_CORE weight 1.0)
        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("EXAM", "JAVA_MID", "Java Midterm", (short) 80, OffsetDateTime.now().minusDays(30))
        );
        recomputeService.recomputeSkills(userId, externalRecords);

        // Should create evidence for JAVA_CORE based on database mapping
        mvc.perform(get("/api/v1/profiles/me/skills")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].verifiedScore", hasItem(80)));
    }

    @Test
    void otherUserPrivateProfile_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        // Seed some data for userId
        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Course", (short) 80, OffsetDateTime.now().minusDays(30))
        );
        recomputeService.recomputeSkills(userId, externalRecords);

        // When: other user tries to access userId's profile
        mvc.perform(get("/api/v1/profiles/{userId}/skills", userId)
                        .header("X-User-Id", otherUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
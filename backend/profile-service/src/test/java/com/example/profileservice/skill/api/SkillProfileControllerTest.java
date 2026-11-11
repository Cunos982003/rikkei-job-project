package com.example.profileservice.skill.api;

import com.example.profileservice.skill.application.SkillRecomputeService;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class SkillProfileControllerTest {

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

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void getMySkills_returnsGroupedSkillsWithEvidences() throws Exception {
        UUID userId = UUID.randomUUID();

        // Seed external records
        List<ExternalRecordDto> externalRecords = List.of(
                new ExternalRecordDto("LMS", "JAVA01", "Java Course", (short) 80, OffsetDateTime.now().minusDays(30)),
                new ExternalRecordDto("LMS", "SPRING01", "Spring Boot Course", (short) 70, OffsetDateTime.now().minusDays(60))
        );
        recomputeService.recomputeSkills(userId, externalRecords);

        // When: GET /api/v1/profiles/me/skills with valid JWT (simulated via header)
        mvc.perform(get("/api/v1/profiles/me/skills")
                        .header("X-User-Id", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].skillCode", hasItems("JAVA_CORE", "SPRING_BOOT", "SQL")))
                .andExpect(jsonPath("$[*].verifiedScore", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].level", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].evidences", everyItem(not(empty()))));
    }

    @Test
    void getUserSkills_unauthorized_returns401() throws Exception {
        UUID userId = UUID.randomUUID();

        // When: GET /api/v1/profiles/{userId}/skills without auth header
        mvc.perform(get("/api/v1/profiles/{userId}/skills", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserSkills_selfReportedSkill_showsLabel() throws Exception {
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
}
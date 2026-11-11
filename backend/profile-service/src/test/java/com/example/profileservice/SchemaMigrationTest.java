package com.example.profileservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SchemaMigrationTest {

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
    JdbcTemplate jdbcTemplate;

    @Test
    void schemaHasRequiredTablesAndColumns() {
        // Check skills table
        List<Map<String, Object>> skillsColumns = jdbcTemplate.queryForList(
            "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'skills'");
        assertThat(skillsColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("id", "code", "name", "group");
        assertThat(skillsColumns).extracting(m -> m.get("data_type"))
            .contains("uuid", "character varying", "character varying", "character varying");

        // Check skill_mappings table
        List<Map<String, Object>> mappingsColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'skill_mappings'");
        assertThat(mappingsColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("id", "source", "source_code", "skill_id", "weight");

        // Check skill_evidences table
        List<Map<String, Object>> evidencesColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'skill_evidences'");
        assertThat(evidencesColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("id", "user_id", "skill_id", "source", "source_ref", "title", "score", "recorded_at", "verified");

        // Check skill_scores table
        List<Map<String, Object>> scoresColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'skill_scores'");
        assertThat(scoresColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("user_id", "skill_id", "verified_score", "self_score", "level", "updated_at");

        // Check indexes exist
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename IN ('skill_evidences', 'skill_mappings')");
        assertThat(indexes).extracting(m -> m.get("indexname"))
            .contains("idx_skill_evidences_user_skill", "idx_skill_mappings_source_code");

        // Check seed data
        Integer skillCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skills", Integer.class);
        assertThat(skillCount).isGreaterThanOrEqualTo(20);

        Integer mappingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skill_mappings", Integer.class);
        assertThat(mappingCount).isGreaterThanOrEqualTo(8);

        // Check candidate_profiles table
        List<Map<String, Object>> candidateProfilesColumns = jdbcTemplate.queryForList(
            "SELECT column_name, column_default FROM information_schema.columns WHERE table_name = 'candidate_profiles'");
        assertThat(candidateProfilesColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("user_id", "full_name", "dob", "gender", "phone", "email", "address", "avatar_url", "headline", "summary", "program", "class_code", "course_name", "visibility", "updated_at");

        // Check visibility default value
        List<Map<String, Object>> visibilityDefault = jdbcTemplate.queryForList(
            "SELECT column_default FROM information_schema.columns WHERE table_name = 'candidate_profiles' AND column_name = 'visibility'");
        assertThat(visibilityDefault).isNotEmpty();
        assertThat(visibilityDefault.get(0).get("column_default")).isEqualTo("'PRIVATE'::character varying");

        // Check educations table
        List<Map<String, Object>> educationsColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'educations'");
        assertThat(educationsColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("id", "user_id", "school", "major", "from_date", "to_date", "gpa", "note");

        // Check certificates table
        List<Map<String, Object>> certificatesColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'certificates'");
        assertThat(certificatesColumns).extracting(m -> m.get("column_name"))
            .containsExactlyInAnyOrder("id", "user_id", "name", "issuer", "issued_at", "expires_at", "file_url", "file_type");

        // Check indexes on user_id for educations and certificates
        List<Map<String, Object>> profileIndexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename IN ('educations', 'certificates')");
        assertThat(profileIndexes).extracting(m -> m.get("indexname"))
            .contains("idx_educations_user_id", "idx_certificates_user_id");
    }
}
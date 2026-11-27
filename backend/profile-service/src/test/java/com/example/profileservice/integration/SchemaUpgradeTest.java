package com.example.profileservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class SchemaUpgradeTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("profile_db")
            .withUsername("postgres")
            .withPassword("postgres");

    private String jdbcUrl;
    private String username;
    private String password;

    @BeforeEach
    void setup() {
        jdbcUrl = postgres.getJdbcUrl();
        username = postgres.getUsername();
        password = postgres.getPassword();
    }

    @Test
    void schemaSqlCanUpgradeHs02DatabaseToHs05() throws Exception {
        // 1. Create an HS-02 style database (candidate_profiles without consent columns and old CHECK)
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // HS-02 schema: candidate_profiles without consent_at, consent_withdrawn_at
            // and with CHECK constraint only allowing PRIVATE, PUBLIC, RECRUITER
            stmt.execute("""
                CREATE TABLE candidate_profiles (
                    user_id UUID PRIMARY KEY,
                    full_name VARCHAR(200),
                    dob DATE,
                    gender VARCHAR(20),
                    phone VARCHAR(50),
                    email VARCHAR(200),
                    address VARCHAR(500),
                    avatar_url VARCHAR(500),
                    headline VARCHAR(500),
                    summary TEXT,
                    program VARCHAR(100),
                    class_code VARCHAR(50),
                    course_name VARCHAR(200),
                    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'PUBLIC', 'RECRUITER')),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
            """);

            // Insert a test profile with PRIVATE visibility (HS-02 default)
            stmt.execute("""
                INSERT INTO candidate_profiles (user_id, full_name, visibility)
                VALUES ('11111111-1111-1111-1111-111111111111', 'Test User', 'PRIVATE')
            """);
        }

        // 2. Apply schema.sql (which now has ALTER TABLE statements)
        String schemaSql = readSchemaSql();
        executeSqlScript(jdbcUrl, username, password, schemaSql);

        // 3. Verify the upgrade worked: consent columns exist, CHECK allows PENDING/REJECTED
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // Check consent_at column exists
            ResultSet consentAtRs = conn.getMetaData().getColumns(null, "public", "candidate_profiles", "consent_at");
            assertThat(consentAtRs.next()).isTrue();
            String consentAtType = consentAtRs.getString("TYPE_NAME");
            assertThat(consentAtType).matches("timestamptz|timestamp with time zone");

            // Check consent_withdrawn_at column exists
            ResultSet consentWithdrawnRs = conn.getMetaData().getColumns(null, "public", "candidate_profiles", "consent_withdrawn_at");
            assertThat(consentWithdrawnRs.next()).isTrue();
            String consentWithdrawnType = consentWithdrawnRs.getString("TYPE_NAME");
            assertThat(consentWithdrawnType).matches("timestamptz|timestamp with time zone");

            // Check CHECK constraint now allows PENDING and REJECTED
            ResultSet checkRs = stmt.executeQuery("""
                SELECT cc.check_clause
                FROM information_schema.check_constraints cc
                JOIN information_schema.constraint_column_usage ccu ON cc.constraint_name = ccu.constraint_name
                WHERE ccu.table_name = 'candidate_profiles' AND ccu.column_name = 'visibility'
            """);
            assertThat(checkRs.next()).isTrue();
            String checkClause = checkRs.getString("check_clause");
            assertThat(checkClause).contains("PENDING");
            assertThat(checkClause).contains("REJECTED");
            assertThat(checkClause).contains("RECRUITER");
            assertThat(checkClause).contains("PRIVATE");
            assertThat(checkClause).contains("PUBLIC");

            // Verify we can insert PENDING and REJECTED (which would fail with old CHECK)
            stmt.execute("INSERT INTO candidate_profiles (user_id, visibility) VALUES ('22222222-2222-2222-2222-222222222222', 'PENDING')");
            stmt.execute("INSERT INTO candidate_profiles (user_id, visibility) VALUES ('33333333-3333-3333-3333-333333333333', 'REJECTED')");

            // Verify existing profile still has PRIVATE visibility
            ResultSet existingRs = stmt.executeQuery("SELECT visibility FROM candidate_profiles WHERE user_id = '11111111-1111-1111-1111-111111111111'");
            assertThat(existingRs.next()).isTrue();
            assertThat(existingRs.getString("visibility")).isEqualTo("PRIVATE");
        }
    }

    @Test
    void schemaSqlIsIdempotentCanRunTwice() throws Exception {
        // 1. Apply schema.sql once
        String schemaSql = readSchemaSql();
        executeSqlScript(jdbcUrl, username, password, schemaSql);

        // 2. Apply schema.sql again (should not fail)
        executeSqlScript(jdbcUrl, username, password, schemaSql);

        // 3. Verify final state is correct
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // Check consent columns exist
            ResultSet consentAtRs = conn.getMetaData().getColumns(null, "public", "candidate_profiles", "consent_at");
            assertThat(consentAtRs.next()).isTrue();

            ResultSet consentWithdrawnRs = conn.getMetaData().getColumns(null, "public", "candidate_profiles", "consent_withdrawn_at");
            assertThat(consentWithdrawnRs.next()).isTrue();

            // Check CHECK constraint is correct
            ResultSet checkRs = stmt.executeQuery("""
                SELECT cc.check_clause
                FROM information_schema.check_constraints cc
                JOIN information_schema.constraint_column_usage ccu ON cc.constraint_name = ccu.constraint_name
                WHERE ccu.table_name = 'candidate_profiles' AND ccu.column_name = 'visibility'
            """);
            assertThat(checkRs.next()).isTrue();
            String checkClause = checkRs.getString("check_clause");
            assertThat(checkClause).contains("PENDING").contains("REJECTED");

            // Verify profile_reviews table exists
            ResultSet reviewTables = conn.getMetaData().getTables(null, "public", "profile_reviews", new String[]{"TABLE"});
            assertThat(reviewTables.next()).isTrue();
        }
    }

    private String readSchemaSql() throws IOException {
        // Read schema.sql from main resources
        Path schemaPath = Path.of("src/main/resources/schema.sql");
        if (!Files.exists(schemaPath)) {
            // Try from project root
            schemaPath = Path.of("backend/profile-service/src/main/resources/schema.sql");
        }
        return Files.readString(schemaPath);
    }

    private void executeSqlScript(String jdbcUrl, String username, String password, String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // Split by semicolon and execute each statement
            String[] statements = sql.split(";(?=\\s*(?:CREATE|ALTER|DROP|INSERT|INDEX|COMMENT|\\s*$))");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }
}
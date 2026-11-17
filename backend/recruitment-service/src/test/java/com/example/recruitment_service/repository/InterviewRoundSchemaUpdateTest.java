package com.example.recruitment_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recruitment_schema_update;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class InterviewRoundSchemaUpdateTest {

    private static final String DATABASE_URL = "jdbc:h2:mem:recruitment_schema_update;DB_CLOSE_DELAY=-1";

    static {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE recruitments (
                        id UUID PRIMARY KEY,
                        job_id UUID NOT NULL,
                        user_id UUID NOT NULL,
                        cv_id UUID,
                        cv_snapshot_url VARCHAR(255),
                        cover_letter VARCHAR(255),
                        status INTEGER NOT NULL,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP
                    )
                    """);
        } catch (SQLException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void addsInterviewRoundsTableToExistingRecruitmentSchema() {
        Integer tableCount = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'PUBLIC' AND table_name = 'INTERVIEW_ROUNDS'",
                Integer.class);

        assertThat(tableCount).isEqualTo(1);
    }
}

package com.example.profileservice.integration;

import com.example.profileservice.dto.LmsStudentDto;
import com.example.profileservice.client.LmsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Luồng HS-05 phía cán bộ: danh sách chờ duyệt, duyệt, trả lại kèm lý do, và phân quyền.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class StaffReviewIntegrationTest {

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

    @MockitoBean
    LmsClient lmsClient;

    @Autowired
    WebApplicationContext webApplicationContext;

    MockMvc mvc;

    private static final UUID STAFF_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /** Tạo hồ sơ ứng viên rồi đưa vào trạng thái PENDING. */
    private UUID submittedCandidate() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "Nguyễn Văn An", "an@rikkeijobs.test", "Java Fullstack", "RA-JAVA-2026", "Java")));

        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/profiles/me/publish")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consent\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PENDING"));

        return userId;
    }

    @Test
    void pending_list_shows_submitted_profile_with_submitted_at() throws Exception {
        UUID userId = submittedCandidate();

        mvc.perform(get("/api/v1/staff/profile-reviews")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.userId=='" + userId + "')].fullName")
                        .value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.content[?(@.userId=='" + userId + "')].submittedAt")
                        .isNotEmpty());
    }

    @Test
    void approving_pending_profile_makes_it_public() throws Exception {
        UUID userId = submittedCandidate();

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/approve")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));

        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
    }

    @Test
    void rejecting_without_reason_returns_400_and_with_reason_stores_it() throws Exception {
        UUID userId = submittedCandidate();

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/reject")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/reject")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thiếu minh chứng kỹ năng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("REJECTED"));

        // Ứng viên thấy lý do bị trả lại trong lịch sử
        mvc.perform(get("/api/v1/profiles/me/review-history")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("REJECT"))
                .andExpect(jsonPath("$.data[0].reason").value("Thiếu minh chứng kỹ năng"))
                .andExpect(jsonPath("$.data[0].actorId").value(STAFF_ID.toString()))
                .andExpect(jsonPath("$.data[1].action").value("SUBMIT"));
    }

    @Test
    void approving_private_profile_returns_409() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "An", "an@rikkeijobs.test", "Java", "RA-1", "Java")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()));

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/approve")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF"))
                .andExpect(status().isConflict());
    }

    @Test
    void staff_endpoints_reject_missing_identity_and_non_staff_role() throws Exception {
        UUID userId = submittedCandidate();

        mvc.perform(get("/api/v1/staff/profile-reviews")
                        .header("X-User-Role", "ROLE_STAFF"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/staff/profile-reviews")
                        .header("X-User-Id", STAFF_ID.toString()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/approve")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_CANDIDATE"))
                .andExpect(status().isForbidden());

        // Hồ sơ vẫn chưa bị đổi trạng thái
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.data.visibility").value("PENDING"));
    }

    @Test
    void admin_can_also_review() throws Exception {
        UUID userId = submittedCandidate();

        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/approve")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }
}

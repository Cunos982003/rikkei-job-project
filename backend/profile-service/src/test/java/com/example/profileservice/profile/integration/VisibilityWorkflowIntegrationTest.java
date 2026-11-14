package com.example.profileservice.profile.integration;

import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.profile.service.PublicProfileQuery;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Nghiệm thu HS-05 đầu-cuối: ứng viên gửi duyệt, cán bộ duyệt/trả lại, doanh nghiệp chỉ
 * thấy hồ sơ PUBLIC, và rút công khai có hiệu lực ngay.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class VisibilityWorkflowIntegrationTest {

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

    @Autowired
    PublicProfileQuery publicProfileQuery;

    MockMvc mvc;

    private static final UUID STAFF_ID = UUID.randomUUID();
    private static final String STAFF_ROLE = "ROLE_STAFF";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private UUID newCandidate() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "Nguyễn Văn An", "an@rikkeijobs.test", "Java Fullstack", "RA-JAVA-2026", "Java")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        return userId;
    }

    private void submit(UUID userId, boolean consent, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/profiles/me/publish")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consent\":" + consent + "}"))
                .andExpect(status().is(expectedStatus));
    }

    private void staffApprove(UUID userId, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/approve")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", STAFF_ROLE))
                .andExpect(status().is(expectedStatus));
    }

    private void staffReject(UUID userId, String reason, int expectedStatus) throws Exception {
        String body = reason == null ? "{}" : "{\"reason\":\"" + reason + "\"}";
        mvc.perform(post("/api/v1/staff/profiles/" + userId + "/reject")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", STAFF_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private void withdraw(UUID userId, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/profiles/me/withdraw").header("X-User-Id", userId.toString()))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void full_lifecycle_from_private_to_public_and_back() throws Exception {
        UUID userId = newCandidate();

        // PRIVATE: doanh nghiệp không thấy
        assertThat(publicProfileQuery.isVisibleToRecruiters(userId)).isFalse();

        // Gửi duyệt (có đồng ý) → PENDING
        submit(userId, true, 200);
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.visibility").value("PENDING"));
        assertThat(publicProfileQuery.isVisibleToRecruiters(userId)).isFalse();

        // Cán bộ trả lại kèm lý do → REJECTED, ứng viên thấy lý do
        staffReject(userId, "Bổ sung minh chứng kỹ năng", 200);
        mvc.perform(get("/api/v1/profiles/me/review-history").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$[0].action").value("REJECT"))
                .andExpect(jsonPath("$[0].reason").value("Bổ sung minh chứng kỹ năng"));
        assertThat(publicProfileQuery.isVisibleToRecruiters(userId)).isFalse();

        // Sửa xong gửi lại → PENDING
        submit(userId, true, 200);

        // Cán bộ duyệt → PUBLIC, doanh nghiệp tìm thấy
        staffApprove(userId, 200);
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
        assertThat(publicProfileQuery.isVisibleToRecruiters(userId)).isTrue();

        // Ứng viên rút → PRIVATE, doanh nghiệp không thấy ngay
        withdraw(userId, 200);
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        assertThat(publicProfileQuery.isVisibleToRecruiters(userId)).isFalse();
    }

    @Test
    void history_records_every_transition_with_actor_and_time() throws Exception {
        UUID userId = newCandidate();

        submit(userId, true, 200);
        staffReject(userId, "Thiếu chứng chỉ", 200);
        submit(userId, true, 200);
        staffApprove(userId, 200);
        withdraw(userId, 200);

        mvc.perform(get("/api/v1/profiles/me/review-history").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].action").value("WITHDRAW"))
                .andExpect(jsonPath("$[1].action").value("APPROVE"))
                .andExpect(jsonPath("$[2].action").value("SUBMIT"))
                .andExpect(jsonPath("$[3].action").value("REJECT"))
                .andExpect(jsonPath("$[4].action").value("SUBMIT"))
                // Người thực hiện và thời điểm đều được ghi lại
                .andExpect(jsonPath("$[0].actorId").value(userId.toString()))
                .andExpect(jsonPath("$[1].actorId").value(STAFF_ID.toString()))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[1].createdAt").isNotEmpty());
    }

    @Test
    void submitting_without_consent_is_refused_and_keeps_profile_private() throws Exception {
        UUID userId = newCandidate();

        submit(userId, false, 400);

        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        mvc.perform(get("/api/v1/profiles/me/review-history").header("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void illegal_transitions_are_refused_with_409() throws Exception {
        UUID userId = newCandidate();

        // Duyệt hồ sơ còn PRIVATE
        staffApprove(userId, 409);
        // Rút hồ sơ còn PRIVATE
        withdraw(userId, 409);

        // Đưa lên PUBLIC rồi thử các chuyển sai luật khác
        submit(userId, true, 200);
        staffApprove(userId, 200);

        submit(userId, true, 409);          // gửi duyệt khi đang PUBLIC
        staffApprove(userId, 409);          // duyệt lại khi đã PUBLIC
        staffReject(userId, "lý do", 409);  // trả lại khi đã PUBLIC

        // Rút về PRIVATE rồi thử trả lại hồ sơ
        withdraw(userId, 200);
        staffReject(userId, "lý do", 409);
    }

    @Test
    void staff_listing_reflects_only_pending_profiles() throws Exception {
        UUID pendingCandidate = newCandidate();
        UUID privateCandidate = newCandidate();
        submit(pendingCandidate, true, 200);

        mvc.perform(get("/api/v1/staff/profile-reviews")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", STAFF_ROLE)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.userId=='" + pendingCandidate + "')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.userId=='" + privateCandidate + "')]").isEmpty());
    }

    @Test
    void unknown_status_filter_is_a_bad_request() throws Exception {
        mvc.perform(get("/api/v1/staff/profile-reviews")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", STAFF_ROLE)
                        .param("status", "KHONG_CO"))
                .andExpect(status().isBadRequest());
    }
}

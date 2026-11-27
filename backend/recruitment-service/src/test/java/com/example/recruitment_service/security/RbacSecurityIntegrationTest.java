package com.example.recruitment_service.security;

import com.example.recruitment_service.dto.RecruitmentReviewDetails;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.service.RecruitmentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NT-08: Bộ test kiểm quyền tự động RBAC
 * Yêu cầu:
 * 1. Gọi endpoint bằng token sai vai trò -> 403 Forbidden
 * 2. Truy cập dữ liệu của công ty khác / người khác -> 404 Not Found
 * 3. Kiểm tra bảo mật cơ bản: JWT hết hạn -> 401/403, JWT chữ ký giả -> 401/403
 */
@SpringBootTest
@ActiveProfiles("test")
class RbacSecurityIntegrationTest {

    private static final String JWT_SECRET = "test-only-signing-key-not-for-production";
    private static final String FORGED_SECRET = "wrong-forged-secret-key-that-does-not-match";

    private static final UUID CANDIDATE_USER_ID = UUID.randomUUID();
    private static final UUID HR_USER_ID = UUID.randomUUID();
    private static final UUID STAFF_USER_ID = UUID.randomUUID();

    private static final UUID COMPANY_A_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID COMPANY_B_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private static final UUID RECRUITMENT_ID = UUID.randomUUID();

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private RecruitmentService recruitmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    // =========================================================================
    // 1. Token sai vai trò -> 403 Forbidden
    // =========================================================================

    @Test
    @DisplayName("CANDIDATE gọi endpoint chuyển trạng thái ứng tuyển (chỉ dành cho HR) -> 403 Forbidden")
    void candidate_cannot_transition_recruitment_shouldReturn403() throws Exception {
        String candidateToken = createToken(CANDIDATE_USER_ID, "CANDIDATE", null, 3600);

        mockMvc.perform(post("/api/v1/recruitments/{id}/transition", RECRUITMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType("application/json")
                        .content("{\"toStatus\":\"INTERVIEW_SCHEDULED\",\"note\":\"Candidate trying to promote own status\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CANDIDATE gọi endpoint danh sách ứng tuyển nội bộ cán bộ STAFF -> 403 Forbidden")
    void candidate_cannot_access_staff_recruitments_shouldReturn403() throws Exception {
        String candidateToken = createToken(CANDIDATE_USER_ID, "CANDIDATE", null, 3600);

        mockMvc.perform(get("/api/v1/staff/recruitments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HR gọi endpoint ứng tuyển việc làm (chỉ dành cho CANDIDATE) -> 403 Forbidden")
    void hr_cannot_apply_for_job_shouldReturn403() throws Exception {
        String hrToken = createToken(HR_USER_ID, "HR", COMPANY_A_ID, 3600);
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/jobs/{id}/apply", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HR gọi endpoint chọn lịch phỏng vấn của ứng viên -> 403 Forbidden")
    void hr_cannot_pick_candidate_interview_slot_shouldReturn403() throws Exception {
        String hrToken = createToken(HR_USER_ID, "HR", COMPANY_A_ID, 3600);
        UUID interviewId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/me/interviews/{id}/pick", interviewId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrToken)
                        .contentType("application/json")
                        .content("{\"slotId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. Truy cập dữ liệu của công ty khác -> 404 Not Found (Data Isolation)
    // =========================================================================

    @Test
    @DisplayName("HR công ty A truy cập đơn ứng tuyển của công ty B -> 404 Not Found (Dữ liệu bị cô lập)")
    void hr_companyA_accessing_companyB_application_shouldReturn404() throws Exception {
        String hrCompanyAToken = createToken(HR_USER_ID, "HR", COMPANY_A_ID, 3600);

        // Giả lập service kiểm tra quyền sở hữu đơn ứng tuyển: không tìm thấy hoặc không thuộc công ty
        when(recruitmentService.getReviewDetails(eq(RECRUITMENT_ID), any(UserPrincipal.class), anyString()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ ứng tuyển", "NOT_FOUND"));

        mockMvc.perform(get("/api/v1/recruitments/{id}", RECRUITMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrCompanyAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // =========================================================================
    // 3. Bảo mật JWT: Không có token, hết hạn, chữ ký giả -> 403 / 401
    // =========================================================================

    @Test
    @DisplayName("Gọi API yêu cầu quyền khi không truyền Authorization header -> 403/401")
    void unauthenticated_request_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/staff/recruitments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gọi API với JWT đã hết hạn (Expired Token) -> Bị từ chối xác thực")
    void expired_jwt_token_shouldBeRejected() throws Exception {
        // Token hết hạn 1 giờ trước
        String expiredToken = createToken(HR_USER_ID, "HR", COMPANY_A_ID, -3600);

        mockMvc.perform(get("/api/v1/staff/recruitments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gọi API với JWT chữ ký giả (Forged Signature) -> Bị từ chối xác thực")
    void forged_jwt_signature_shouldBeRejected() throws Exception {
        // Ký bằng secret khác
        String forgedToken = Jwts.builder()
                .claim("userId", HR_USER_ID.toString())
                .claim("role", "STAFF")
                .signWith(Keys.hmacShaKeyFor(FORGED_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/staff/recruitments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helper tạo token
    // =========================================================================

    private String createToken(UUID userId, String role, UUID companyId, long validSeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .claim("userId", userId.toString())
                .claim("role", role)
                .claim("email", role.toLowerCase() + "@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validSeconds)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));

        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }
        return builder.compact();
    }
}

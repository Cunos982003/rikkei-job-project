package com.example.authservice;

import com.example.authservice.client.lms.LmsClient;
import com.example.authservice.client.lms.LmsUserResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.SsoRequest;
import com.example.authservice.entity.UsedSsoTicket;
import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import com.example.authservice.metrics.AuthEndpoint;
import com.example.authservice.metrics.AuthMetricsService;
import com.example.authservice.metrics.AuthReason;
import com.example.authservice.metrics.AuthResult;
import com.example.authservice.repository.UsedSsoTicketRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthMetricsIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsedSsoTicketRepository usedSsoTicketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthMetricsService authMetricsService;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private LmsClient lmsClient;

    private static final String TEST_EMAIL = "metrics-test@rikkei.vn";
    private static final String TEST_PASSWORD = "Password123@";

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        usedSsoTicketRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createActiveUser(String email, String role) {
        User user = new User(
                null,
                email,
                "Metrics User",
                role,
                passwordEncoder.encode(TEST_PASSWORD),
                Instant.now(),
                Instant.now()
        );
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("AUTH-01: Login thành công qua /api/v1/auth/login tăng counter endpoint=login, result=success, reason=none")
    void testLoginSuccessMetric() throws Exception {
        createActiveUser(TEST_EMAIL, "ROLE_CANDIDATE");
        double initialCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.SUCCESS, AuthReason.NONE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk());

        double newCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.SUCCESS, AuthReason.NONE);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: Login thành công qua /api/v1/auth/hr/login tăng counter endpoint=hr_login, result=success, reason=none")
    void testHrLoginSuccessMetric() throws Exception {
        createActiveUser("hr-metric@rikkei.vn", "ROLE_HR");
        double initialCount = authMetricsService.getCount(AuthEndpoint.HR_LOGIN, AuthResult.SUCCESS, AuthReason.NONE);

        mockMvc.perform(post("/api/v1/auth/hr/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("hr-metric@rikkei.vn", TEST_PASSWORD))))
                .andExpect(status().isOk());

        double newCount = authMetricsService.getCount(AuthEndpoint.HR_LOGIN, AuthResult.SUCCESS, AuthReason.NONE);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: Login thất bại do sai mật khẩu tăng counter result=failure, reason=invalid_credentials")
    void testLoginFailureInvalidCredentials() throws Exception {
        createActiveUser(TEST_EMAIL, "ROLE_CANDIDATE");
        double initialCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.INVALID_CREDENTIALS);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, "WrongPass123!"))))
                .andExpect(status().isUnauthorized());

        double newCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.INVALID_CREDENTIALS);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: Login thất bại do tài khoản bị khóa tăng counter result=failure, reason=account_locked")
    void testLoginFailureAccountLocked() throws Exception {
        User lockedUser = createActiveUser(TEST_EMAIL, "ROLE_CANDIDATE");
        lockedUser.setStatus(UserStatus.LOCKED);
        lockedUser.setLockedReason("Manual lock");
        userRepository.save(lockedUser);

        double initialCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.ACCOUNT_LOCKED);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isLocked());

        double newCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.ACCOUNT_LOCKED);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: Login thất bại do lỗi validate dữ liệu tăng counter result=failure, reason=validation_error")
    void testLoginFailureValidationError() throws Exception {
        double initialCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.VALIDATION_ERROR);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email-format\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());

        double newCount = authMetricsService.getCount(AuthEndpoint.LOGIN, AuthResult.FAILURE, AuthReason.VALIDATION_ERROR);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: SSO thành công tăng counter endpoint=sso, result=success, reason=none")
    void testSsoSuccessMetric() throws Exception {
        String ticket = "ticket-metric-success-01";
        when(lmsClient.verifyTicket(eq(ticket))).thenReturn(new LmsUserResponse(
                "lms-metric-1",
                "student-metric@rikkei.edu.vn",
                "Student Metric",
                "CANDIDATE",
                Instant.now().toString()
        ));

        double initialCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.SUCCESS, AuthReason.NONE);

        mockMvc.perform(post("/api/v1/auth/sso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andExpect(status().isOk());

        double newCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.SUCCESS, AuthReason.NONE);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: SSO thất bại do vé đã sử dụng tăng counter result=failure, reason=ticket_already_used")
    void testSsoFailureTicketAlreadyUsed() throws Exception {
        String ticket = "ticket-already-used-metric";
        usedSsoTicketRepository.save(new UsedSsoTicket(ticket, Instant.now()));

        double initialCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_ALREADY_USED);

        mockMvc.perform(post("/api/v1/auth/sso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andExpect(status().isUnauthorized());

        double newCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_ALREADY_USED);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: SSO thất bại do vé hết hạn tăng counter result=failure, reason=ticket_expired")
    void testSsoFailureTicketExpired() throws Exception {
        String ticket = "ticket-expired-metric";
        when(lmsClient.verifyTicket(eq(ticket))).thenReturn(new LmsUserResponse(
                "lms-metric-2",
                "student-metric2@rikkei.edu.vn",
                "Student Metric 2",
                "CANDIDATE",
                Instant.now().minus(10, ChronoUnit.MINUTES).toString()
        ));

        double initialCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_EXPIRED);

        mockMvc.perform(post("/api/v1/auth/sso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andExpect(status().isUnauthorized());

        double newCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_EXPIRED);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01: SSO thất bại do vé không hợp lệ tăng counter result=failure, reason=ticket_invalid")
    void testSsoFailureTicketInvalid() throws Exception {
        String ticket = "ticket-invalid-metric";
        when(lmsClient.verifyTicket(eq(ticket))).thenThrow(new SsoException(ErrorCode.TICKET_INVALID, "Invalid ticket"));

        double initialCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_INVALID);

        mockMvc.perform(post("/api/v1/auth/sso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andExpect(status().isBadRequest());

        double newCount = authMetricsService.getCount(AuthEndpoint.SSO, AuthResult.FAILURE, AuthReason.TICKET_INVALID);
        assertThat(newCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("AUTH-01 AC: /actuator/prometheus xuất metric auth_login_total và tuyệt đối KHÔNG chứa email hay username")
    void testPrometheusScrapeOutputAndNoPii() throws Exception {
        // Thực hiện 1 login thành công và 1 login thất bại
        createActiveUser(TEST_EMAIL, "ROLE_CANDIDATE");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(TEST_EMAIL, "WrongPass"))))
                .andExpect(status().isUnauthorized());

        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();

        // 1. Kiểm tra tồn tại metric auth_login_total với các label chuẩn
        assertThat(body).contains("auth_login_total");
        assertThat(body).contains("endpoint=\"login\"");
        assertThat(body).contains("result=\"success\"");
        assertThat(body).contains("result=\"failure\"");
        assertThat(body).contains("reason=\"none\"");
        assertThat(body).contains("reason=\"invalid_credentials\"");

        // 2. Kiểm tra PII: Tuyệt đối không chứa email hoặc username
        assertThat(body).doesNotContain(TEST_EMAIL);
        assertThat(body).doesNotContain("haikhac");
        assertThat(body).doesNotContain("Nguyen Van");
        assertThat(body).doesNotContain("Metrics User");
    }
}

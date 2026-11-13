package com.example.authservice;

import com.example.authservice.dto.SsoRequest;
import com.example.authservice.entity.UsedSsoTicket;
import com.example.authservice.entity.User;
import com.example.authservice.repository.UsedSsoTicketRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SsoAuthenticationIntegrationTest {

    private static MockWebServer mockLmsServer;

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
    private JwtTokenProvider jwtTokenProvider;

    @BeforeAll
    static void startMockLms() throws IOException {
        mockLmsServer = new MockWebServer();
        mockLmsServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                // Verify service key header
                String serviceKey = request.getHeader("X-Service-Key");
                if (!"test-service-key-xyz".equals(serviceKey)) {
                    return new MockResponse().setResponseCode(401).setBody("{\"error\":\"UNAUTHORIZED\"}");
                }

                String body = request.getBody().readUtf8();

                if (body.contains("ticket-new-user-01")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-student-1001",
                              "email": "student1001@rikkei.edu.vn",
                              "fullName": "Nguyen Van Student",
                              "role": "CANDIDATE",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().toString()));
                }

                if (body.contains("ticket-existing-user-02")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-mentor-2001",
                              "email": "mentor2001@rikkei.edu.vn",
                              "fullName": "Updated Mentor Name",
                              "role": "MENTOR",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().toString()));
                }

                if (body.contains("ticket-invalid-03")) {
                    return new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"TICKET_NOT_FOUND\",\"message\":\"Ticket not found\"}");
                }

                if (body.contains("ticket-expired-04")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-student-3001",
                              "email": "student3001@rikkei.edu.vn",
                              "fullName": "Expired Student",
                              "role": "CANDIDATE",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().minusSeconds(75).toString()));
                }

                if (body.contains("ticket-concurrent-06")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-concurrent-6001",
                              "email": "concurrent6001@rikkei.edu.vn",
                              "fullName": "Concurrent User",
                              "role": "STAFF",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().toString()));
                }

                if (body.contains("ticket-lms-down-07")) {
                    return new MockResponse()
                        .setResponseCode(503)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"LMS_UNAVAILABLE\",\"message\":\"Mock LMS temporarily unavailable\"}");
                }

                if (body.contains("ticket-invalid-role-08")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-hr-8001",
                              "email": "hr8001@rikkei.edu.vn",
                              "fullName": "HR User",
                              "role": "ROOT_ADMIN",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().toString()));
                }

                if (body.contains("ticket-staff-12")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "lmsUserId": "lms-staff-1201",
                              "email": "staff1201@rikkei.edu.vn",
                              "fullName": "Staff Member",
                              "role": "STAFF",
                              "issuedAt": "%s"
                            }
                            """.formatted(Instant.now().toString()));
                }

                if (body.contains("ticket-timeout-13")) {
                    return new MockResponse()
                        .setResponseCode(504)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"GATEWAY_TIMEOUT\",\"message\":\"Gateway timeout from Mock LMS\"}");
                }

                return new MockResponse().setResponseCode(404);
            }
        });
        mockLmsServer.start();
    }

    @AfterAll
    static void stopMockLms() throws IOException {
        mockLmsServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.lms.base-url", () -> "http://localhost:" + mockLmsServer.getPort());
        registry.add("app.lms.service-key", () -> "test-service-key-xyz");
        registry.add("app.lms.verify-endpoint", () -> "/lms/sso/verify");
        registry.add("app.lms.timeout-seconds", () -> 2);
        registry.add("app.lms.max-retries", () -> 1);
    }

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        usedSsoTicketRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. SSO thành công với user chưa tồn tại - tạo user mới và trả JWT")
    void ssoSuccessWithNewUser() throws Exception {
        String ticket = "ticket-new-user-01";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()))
            .andExpect(jsonPath("$.tokenType", is("Bearer")))
            .andExpect(jsonPath("$.user.lmsUserId", is("lms-student-1001")))
            .andExpect(jsonPath("$.user.email", is("student1001@rikkei.edu.vn")))
            .andExpect(jsonPath("$.user.fullName", is("Nguyen Van Student")))
            .andExpect(jsonPath("$.user.role", is("ROLE_CANDIDATE")));

        // Verify user exists in database
        assertThat(userRepository.findByLmsUserId("lms-student-1001")).isPresent();
        assertThat(usedSsoTicketRepository.existsByTicket(ticket)).isTrue();
    }

    @Test
    @DisplayName("2. SSO thành công với user đã tồn tại - cập nhật thông tin và trả JWT")
    void ssoSuccessWithExistingUser() throws Exception {
        User existing = new User(
            "lms-mentor-2001",
            "mentor2001@rikkei.edu.vn",
            "Old Mentor Name",
            "ROLE_MENTOR",
            Instant.now().minusSeconds(1000),
            Instant.now().minusSeconds(1000)
        );
        userRepository.save(existing);

        String ticket = "ticket-existing-user-02";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.fullName", is("Updated Mentor Name")))
            .andExpect(jsonPath("$.user.role", is("ROLE_MENTOR")));

        User updated = userRepository.findByLmsUserId("lms-mentor-2001").orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Updated Mentor Name");
    }

    @Test
    @DisplayName("3. Ticket không hợp lệ - Mock LMS trả 404")
    void ssoInvalidTicketFromLms() throws Exception {
        String ticket = "ticket-invalid-03";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("TICKET_INVALID")));

        assertThat(usedSsoTicketRepository.existsByTicket(ticket)).isFalse();
    }

    @Test
    @DisplayName("4. Ticket hết hạn sau 60 giây - bị từ chối")
    void ssoExpiredTicket() throws Exception {
        String ticket = "ticket-expired-04";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("TICKET_EXPIRED")));

        // Expired ticket must NOT create user
        assertThat(userRepository.findByLmsUserId("lms-student-3001")).isEmpty();
    }

    @Test
    @DisplayName("5. Ticket đã được sử dụng - bị từ chối với TICKET_ALREADY_USED")
    void ssoAlreadyUsedTicket() throws Exception {
        String ticket = "ticket-already-used-05";
        usedSsoTicketRepository.save(new UsedSsoTicket(ticket, Instant.now()));

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("TICKET_ALREADY_USED")));
    }

    @Test
    @DisplayName("6. Xử lý race condition - hai request đồng thời cùng sử dụng một ticket")
    void ssoConcurrentRequestsSameTicket() throws InterruptedException, ExecutionException {
        String ticket = "ticket-concurrent-06";

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> call1 = () -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/sso")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andReturn().getResponse().getStatus();
        };
        Callable<Integer> call2 = () -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/sso")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
                .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = executor.submit(call1);
        Future<Integer> f2 = executor.submit(call2);
        startLatch.countDown();

        int s1 = f1.get();
        int s2 = f2.get();
        executor.shutdown();

        // Exactly one request must succeed (200 OK), the other must be rejected (401 Unauthorized)
        assertThat(List.of(s1, s2)).containsExactlyInAnyOrder(200, 401);
    }

    @Test
    @DisplayName("7. Mock LMS không hoạt động / lỗi kết nối - trả lỗi 503 EXTERNAL_UNAVAILABLE")
    void ssoMockLmsUnavailable() throws Exception {
        String ticket = "ticket-lms-down-07";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code", is("EXTERNAL_UNAVAILABLE")));
    }

    @Test
    @DisplayName("8. Role không hợp lệ từ LMS (HR không thuộc LMS NT-01) - trả lỗi INVALID_ROLE")
    void ssoInvalidRoleFromLms() throws Exception {
        String ticket = "ticket-invalid-role-08";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("INVALID_ROLE")));
    }

    @Test
    @DisplayName("9. JWT hợp lệ - truy cập endpoint được bảo vệ /api/v1/auth/me thành công")
    void accessProtectedResourceWithValidJwt() throws Exception {
        User user = new User(
            "lms-admin-9001",
            "admin9001@rikkei.edu.vn",
            "System Admin",
            "ROLE_ADMIN",
            Instant.now(),
            Instant.now()
        );
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(user.getId().toString())))
            .andExpect(jsonPath("$.email", is("admin9001@rikkei.edu.vn")))
            .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("10. JWT không hợp lệ hoặc hết hạn - truy cập endpoint được bảo vệ trả 401")
    void accessProtectedResourceWithInvalidJwt() throws Exception {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("11. Không có token - truy cập endpoint được bảo vệ trả 401")
    void accessProtectedResourceWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("12. Role mapping chính xác cho tất cả role hợp lệ (STAFF -> ROLE_STAFF, ADMIN -> ROLE_ADMIN)")
    void ssoRoleMappingStaffAndAdmin() throws Exception {
        String ticket = "ticket-staff-12";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role", is("ROLE_STAFF")));
    }

    @Test
    @DisplayName("13. Mock LMS phản hồi timeout (504 Gateway Timeout) - backend xử lý trả về 503 EXTERNAL_UNAVAILABLE")
    void ssoMockLmsTimeout() throws Exception {
        String ticket = "ticket-timeout-13";

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SsoRequest(ticket))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code", is("EXTERNAL_UNAVAILABLE")));
    }
}

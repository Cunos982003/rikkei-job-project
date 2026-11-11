package com.example.authservice;

import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SecurityIntegrationTest.TestEndpointConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @TestConfiguration
    static class TestEndpointConfig {
        @RestController
        @RequestMapping("/api/v1/test")
        static class TestSecuredController {
            @GetMapping("/admin-only")
            @PreAuthorize("hasRole('ADMIN')")
            public ResponseEntity<Map<String, String>> adminEndpoint() {
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "role", "ROLE_ADMIN"));
            }

            @GetMapping("/hr-only")
            @PreAuthorize("hasRole('HR')")
            public ResponseEntity<Map<String, String>> hrEndpoint() {
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "role", "ROLE_HR"));
            }
        }
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Không có JWT truy cập endpoint bảo vệ - trả 401 UNAUTHORIZED")
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
            .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("2. JWT có chữ ký hoặc định dạng không hợp lệ - trả 401 UNAUTHORIZED")
    void protectedEndpointWithInvalidSignatureReturns401() throws Exception {
        String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4ifQ.invalid_sig";

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("3. JWT đã hết hạn - trả 401 UNAUTHORIZED")
    void protectedEndpointWithExpiredTokenReturns401() throws Exception {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("role", "ROLE_CANDIDATE")
            .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
            .expiration(Date.from(Instant.now().minusSeconds(3600)))
            .signWith(key)
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("4. User có role không đủ quyền - trả 403 FORBIDDEN")
    void userWithInsufficientRoleReturns403() throws Exception {
        User candidate = new User(
            "lms-cand-01",
            "candidate@rikkei.vn",
            "Candidate User",
            "ROLE_CANDIDATE",
            Instant.now(),
            Instant.now()
        );
        candidate = userRepository.save(candidate);

        String token = jwtTokenProvider.generateToken(candidate);

        mockMvc.perform(get("/api/v1/test/admin-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("FORBIDDEN")))
            .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("5. User có role phù hợp - truy cập thành công 200 OK")
    void userWithMatchingRoleReturns200() throws Exception {
        User admin = new User(
            "lms-admin-01",
            "admin@rikkei.vn",
            "Administrator",
            "ROLE_ADMIN",
            Instant.now(),
            Instant.now()
        );
        admin = userRepository.save(admin);

        String token = jwtTokenProvider.generateToken(admin);

        mockMvc.perform(get("/api/v1/test/admin-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("SUCCESS")))
            .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("6. Endpoint public không yêu cầu JWT - không bị từ chối bởi Spring Security")
    void publicEndpointsAccessibleWithoutAuth() throws Exception {
        // Validation error on missing body (400), but NOT 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

        mockMvc.perform(post("/api/v1/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("7. Preflight CORS OPTIONS request được cho phép mà không cần xác thực")
    void corsOptionsPreflightAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("8. JWT claims không chứa thông tin nhạy cảm và bao gồm cấu trúc roles hợp lệ")
    void jwtClaimsDoNotContainSensitiveData() {
        User hrUser = new User(
            null,
            "sec_hr@rikkei.vn",
            "Security HR",
            "ROLE_HR",
            "$2a$10$hashedPasswordHere1234567890",
            Instant.now(),
            Instant.now()
        );
        hrUser.setId(UUID.randomUUID());

        String token = jwtTokenProvider.generateToken(hrUser);
        var claims = jwtTokenProvider.getClaims(token);

        org.assertj.core.api.Assertions.assertThat(claims.get("password")).isNull();
        org.assertj.core.api.Assertions.assertThat(claims.get("passwordHash")).isNull();
        org.assertj.core.api.Assertions.assertThat(claims.get("lockedUntil")).isNull();
        org.assertj.core.api.Assertions.assertThat(claims.get("failedLoginCount")).isNull();
        org.assertj.core.api.Assertions.assertThat(claims.get("role")).isEqualTo("ROLE_HR");
        org.assertj.core.api.Assertions.assertThat(claims.get("roles")).isNotNull();
    }
}


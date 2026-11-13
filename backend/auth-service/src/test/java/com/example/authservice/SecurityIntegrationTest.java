package com.example.authservice;

import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtAuthenticationFilter;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.security.RoleMapper;
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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    @Test
    @DisplayName("9. JWT role không có prefix được chuẩn hóa thành ROLE_... trước khi được dùng trong Spring Security")
    void rawRoleWithoutRolePrefixIsNormalized() {
        User candidate = new User(
            "lms-candidate-raw",
            "raw@rikkei.vn",
            "Raw Candidate",
            "CANDIDATE",
            Instant.now(),
            Instant.now()
        );
        candidate = userRepository.save(candidate);

        String token = jwtTokenProvider.generateToken(candidate);
        var claims = jwtTokenProvider.getClaims(token);

        org.assertj.core.api.Assertions.assertThat(claims.get("role")).isEqualTo("ROLE_CANDIDATE");
        org.assertj.core.api.Assertions.assertThat(claims.get("roles")).isEqualTo(List.of("ROLE_CANDIDATE"));
    }

    @Test
    @DisplayName("10. JWT thiếu role claim bị từ chối ở tầng authentication")
    void jwtWithoutRoleClaimFailsAuthentication() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", UUID.randomUUID().toString())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. JWT với role invalid bị từ chối")
    void jwtWithInvalidRoleFailsAuthentication() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", UUID.randomUUID().toString())
            .claim("role", "ROLE_INVALID")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. JWT với roles list chứa role invalid bị từ chối")
    void jwtWithInvalidRoleInRolesArrayFailsAuthentication() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", UUID.randomUUID().toString())
            .claim("roles", List.of("ROLE_ADMIN", "ROLE_INVALID"))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("13. JWT với subject không phải UUID bị từ chối")
    void jwtWithInvalidSubjectUuidFailsAuthentication() throws Exception {
        String token = Jwts.builder()
            .subject("not-a-uuid")
            .claim("userId", UUID.randomUUID().toString())
            .claim("role", "ROLE_ADMIN")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14. JWT với userId claim không phải UUID bị từ chối")
    void jwtWithInvalidUserIdClaimFailsAuthentication() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", "not-a-uuid")
            .claim("role", "ROLE_ADMIN")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("15. SecurityContext đã có Authentication không bị ghi đè bởi JWT")
    void existingAuthenticationIsNotOverwritten() throws Exception {
        User user = new User(
            "existing-auth-user",
            "existing@rikkei.vn",
            "Existing User",
            "ROLE_ADMIN",
            Instant.now(),
            Instant.now()
        );
        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user);

        String existingPrincipal = "existing-principal";
        var existingAuthentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            existingPrincipal,
            "context-password",
            java.util.List.of()
        );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
        filter.doFilter(request, response, new MockFilterChain());

        org.assertj.core.api.Assertions.assertThat(
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
        ).isNotNull();
        org.assertj.core.api.Assertions.assertThat(
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
        ).isEqualTo(existingPrincipal);

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("15b. Role được hỗ trợ và normalize đúng cho mọi role chính thức")
    void supportedRolesAreNormalizedToSecurityAuthorities() {
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("HR")).isEqualTo("ROLE_HR");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("ROLE_HR")).isEqualTo("ROLE_HR");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("CANDIDATE")).isEqualTo("ROLE_CANDIDATE");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("ROLE_CANDIDATE")).isEqualTo("ROLE_CANDIDATE");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("MENTOR")).isEqualTo("ROLE_MENTOR");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("STAFF")).isEqualTo("ROLE_STAFF");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("ADMIN")).isEqualTo("ROLE_ADMIN");

        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("HR")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("ROLE_HR")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("CANDIDATE")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("ROLE_CANDIDATE")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("MENTOR")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("STAFF")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("15c. Role invalid bị từ chối và không fallback sang authority khác")
    void invalidRoleClaimIsRejectedWithoutFallback() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", UUID.randomUUID().toString())
            .claim("role", "ROLE_INVALID")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> RoleMapper.normalizeToSecurityRole("INVALID_ROLE"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("15d. roles[] có item invalid bị từ chối")
    void invalidRoleInRolesArrayIsRejectedWithoutFallback() throws Exception {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("userId", UUID.randomUUID().toString())
            .claim("roles", List.of("ROLE_ADMIN", "ROLE_INVALID"))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("16. mustChangePassword=true cho phép chỉ POST /auth/change-password")
    void mustChangePasswordAllowsOnlyPasswordChangeRequest() throws Exception {
        User user = new User(
            "must-change",
            "mustchange@rikkei.vn",
            "Must Change",
            "ROLE_ADMIN",
            Instant.now(),
            Instant.now()
        );
        user.setPasswordHash(new BCryptPasswordEncoder().encode("oldpass"));
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("MUST_CHANGE_PASSWORD")));

        mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"oldpass\",\"newPassword\":\"Newpass1\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("17. mustChangePassword=false + đúng role được phép truy cập endpoint business")
    void allowedRoleWithMustChangePasswordFalseCanAccessBusinessEndpoint() throws Exception {
        User admin = new User(
            "must-not-change",
            "admin2@rikkei.vn",
            "Administrator 2",
            "ROLE_ADMIN",
            Instant.now(),
            Instant.now()
        );
        admin = userRepository.save(admin);
        String token = jwtTokenProvider.generateToken(admin);

        mockMvc.perform(get("/api/v1/test/admin-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("18. Sai role khi mustChangePassword=true trả 403 FORBIDDEN")
    void wrongRoleStillReturnsForbiddenWhenMustChangePasswordIsTrue() throws Exception {
        User candidate = new User(
            "bad-role-must-change",
            "badrole@rikkei.vn",
            "Bad Role",
            "ROLE_CANDIDATE",
            Instant.now(),
            Instant.now()
        );
        candidate.setPasswordHash(new BCryptPasswordEncoder().encode("oldpass"));
        candidate.setMustChangePassword(true);
        candidate = userRepository.save(candidate);
        String token = jwtTokenProvider.generateToken(candidate);

        mockMvc.perform(get("/api/v1/test/admin-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("MUST_CHANGE_PASSWORD")));
    }

    @Test
    @DisplayName("19. Role HR và ROLE_HR được normalize đồng nhất")
    void hrAndRoleHrNormalizeToSameAuthority() {
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("HR")).isEqualTo("ROLE_HR");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("ROLE_HR")).isEqualTo("ROLE_HR");
    }

    @Test
    @DisplayName("20. Role CANDIDATE và ROLE_CANDIDATE được normalize đồng nhất")
    void candidateAndRoleCandidateNormalizeToSameAuthority() {
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("CANDIDATE")).isEqualTo("ROLE_CANDIDATE");
        org.assertj.core.api.Assertions.assertThat(RoleMapper.normalizeToSecurityRole("ROLE_CANDIDATE")).isEqualTo("ROLE_CANDIDATE");
    }

    @Test
    @DisplayName("21. Role MENTOR, STAFF, ADMIN được hỗ trợ")
    void supportedRolesAreAccepted() {
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("MENTOR")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("STAFF")).isTrue();
        org.assertj.core.api.Assertions.assertThat(RoleMapper.isSupportedRole("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("22. Role invalid không được cấp authority")
    void invalidRoleDoesNotProduceAuthority() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> RoleMapper.normalizeToSecurityRole("INVALID_ROLE"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}


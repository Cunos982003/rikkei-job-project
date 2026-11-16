package com.example.authservice;

import com.example.authservice.dto.CreateHrAccountAdminRequest;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LockHrAccountAdminRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.entity.PasswordResetToken;
import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.repository.PasswordResetTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HrAccountAdminIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    private String staffToken;
    private String adminToken;
    private String candidateToken;
    private String hrToken;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        User staffUser = createTestUser("staff@rikkei.vn", "Staff User", "ROLE_STAFF");
        staffToken = jwtTokenProvider.generateToken(staffUser);

        User adminUser = createTestUser("admin@rikkei.vn", "Admin User", "ROLE_ADMIN");
        adminToken = jwtTokenProvider.generateToken(adminUser);

        User candidateUser = createTestUser("candidate@rikkei.vn", "Candidate User", "ROLE_CANDIDATE");
        candidateToken = jwtTokenProvider.generateToken(candidateUser);

        User hrUser = createTestUser("existinghr@rikkei.vn", "Existing HR User", "ROLE_HR");
        hrToken = jwtTokenProvider.generateToken(hrUser);
    }

    private User createTestUser(String email, String fullName, String role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode("TestPass123@"));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Test
    @DisplayName("1. Tạo HR thành công - HTTP 201, nhận mật khẩu tạm, mustChangePassword = true")
    void createHrAccountSuccess() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("newhr@company.com", "Nguyen Van HR", companyId);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.statusCode", is(201)))
            .andExpect(jsonPath("$.data.email", is("newhr@company.com")))
            .andExpect(jsonPath("$.data.fullName", is("Nguyen Van HR")))
            .andExpect(jsonPath("$.data.companyId", is(companyId.toString())))
            .andExpect(jsonPath("$.data.status", is("ACTIVE")))
            .andExpect(jsonPath("$.data.mustChangePassword", is(true)))
            .andExpect(jsonPath("$.data.temporaryPassword", notNullValue()))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPass = root.path("data").path("temporaryPassword").asText();
        assertThat(tempPass).hasSize(12);
        assertThat(tempPass).containsPattern("[A-Z]");
        assertThat(tempPass).containsPattern("[a-z]");
        assertThat(tempPass).containsPattern("[0-9]");
        assertThat(tempPass).containsPattern("[@#$!%&*]");

        User savedUser = userRepository.findByEmail("newhr@company.com").orElseThrow();
        assertThat(savedUser.getCompanyId()).isEqualTo(companyId);
        assertThat(savedUser.isMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches(tempPass, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("2. Tạo HR trùng email - trả HTTP 409 Conflict")
    void createHrAccountDuplicateEmailReturns409() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("staff@rikkei.vn", "Trùng Email", companyId);

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("3. Tạo HR thiếu trường hoặc email không hợp lệ - trả HTTP 400 Bad Request")
    void createHrAccountValidationErrors() throws Exception {
        CreateHrAccountAdminRequest invalidEmail = new CreateHrAccountAdminRequest("not-an-email", "Name", UUID.randomUUID());
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)))
            .andExpect(status().isBadRequest());

        CreateHrAccountAdminRequest blankName = new CreateHrAccountAdminRequest("valid@email.com", "", UUID.randomUUID());
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankName)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Đăng nhập bằng mật khẩu tạm vừa tạo qua POST /api/v1/auth/hr/login thành công")
    void loginWithTemporaryPasswordSuccess() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("loginhr@company.com", "HR Dang Nhap", companyId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String tempPass = root.path("data").path("temporaryPassword").asText();

        LoginRequest loginReq = new LoginRequest("loginhr@company.com", tempPass);
        mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()))
            .andExpect(jsonPath("$.user.email", is("loginhr@company.com")))
            .andExpect(jsonPath("$.user.mustChangePassword", is(true)));
    }

    @Test
    @DisplayName("5. Khóa HR: POST /{id}/lock -> Đăng nhập bị từ chối 423 ACCOUNT_LOCKED và không tự mở sau 1 ngày")
    void lockHrAccountBlocksLoginPermanently() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("lockhr@company.com", "HR Khoa", companyId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();
        String tempPass = root.path("data").path("temporaryPassword").asText();

        LockHrAccountAdminRequest lockReq = new LockHrAccountAdminRequest("Tạm đình chỉ công tác");
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/lock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("LOCKED")))
            .andExpect(jsonPath("$.data.lockedReason", is("Tạm đình chỉ công tác")))
            .andExpect(jsonPath("$.data.lockedAt", notNullValue()));

        User userInDb = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        assertThat(userInDb.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(userInDb.getLockedReason()).isEqualTo("Tạm đình chỉ công tác");
        assertThat(userInDb.getLockedUntil()).isEqualTo(Instant.parse("9999-12-31T23:59:59Z"));

        LoginRequest loginReq = new LoginRequest("lockhr@company.com", tempPass);
        mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));

        userInDb.setLockedAt(Instant.now().minusSeconds(86400 * 2));
        userRepository.save(userInDb);

        mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));
    }

    @Test
    @DisplayName("6. Mở khóa HR: POST /{id}/unlock -> Đăng nhập lại thành công và idempotency")
    void unlockHrAccountEnablesLoginAgain() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("unlockhr@company.com", "HR Mo Khoa", companyId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();
        String tempPass = root.path("data").path("temporaryPassword").asText();

        LockHrAccountAdminRequest lockReq = new LockHrAccountAdminRequest("Khóa kiểm tra");
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/lock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockReq)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/unlock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("ACTIVE")))
            .andExpect(jsonPath("$.data.lockedReason").doesNotExist())
            .andExpect(jsonPath("$.data.lockedAt").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/unlock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        LoginRequest loginReq = new LoginRequest("unlockhr@company.com", tempPass);
        mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("7. Liệt kê tài khoản HR theo companyId - sắp xếp createdAt giảm dần")
    void listHrAccountsByCompanyId() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest hr1 = new CreateHrAccountAdminRequest("list1@company.com", "HR 1", companyId);
        CreateHrAccountAdminRequest hr2 = new CreateHrAccountAdminRequest("list2@company.com", "HR 2", companyId);

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hr1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hr2)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .param("companyId", companyId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].email", is("list2@company.com")))
            .andExpect(jsonPath("$.data[1].email", is("list1@company.com")))
            .andExpect(jsonPath("$.data[0].temporaryPassword").doesNotExist());
    }

    @Test
    @DisplayName("8. Đếm số HR theo danh sách companyIds")
    void countHrAccountsByCompanyIds() throws Exception {
        UUID compA = UUID.randomUUID();
        UUID compB = UUID.randomUUID();
        UUID compC = UUID.randomUUID();

        CreateHrAccountAdminRequest hrA1 = new CreateHrAccountAdminRequest("hra1@compa.com", "HR A1", compA);
        CreateHrAccountAdminRequest hrA2 = new CreateHrAccountAdminRequest("hra2@compa.com", "HR A2", compA);
        CreateHrAccountAdminRequest hrB1 = new CreateHrAccountAdminRequest("hrb1@compb.com", "HR B1", compB);

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hrA1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hrA2)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hrB1)))
            .andExpect(status().isCreated());

        String idsParam = compA + "," + compB + "," + compC;
        mockMvc.perform(get("/api/v1/auth/internal/hr-accounts/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .param("companyIds", idsParam))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data['" + compA + "']", is(2)))
            .andExpect(jsonPath("$.data['" + compB + "']", is(1)))
            .andExpect(jsonPath("$.data['" + compC + "']", is(0)));

        List<String> tooManyIds = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            tooManyIds.add(UUID.randomUUID().toString());
        }
        mockMvc.perform(get("/api/v1/auth/internal/hr-accounts/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .param("companyIds", String.join(",", tooManyIds)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Kiểm tra phân quyền: Không có token -> 401; Role CANDIDATE, HR -> 403; Role ADMIN -> 200/201")
    void securityAuthorizationChecks() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("sechr@company.com", "HR Security", companyId);

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("10. HR bị khoá -> gọi forgot/reset-password thành công nhưng login vẫn 423 với message khoá thủ công, status vẫn LOCKED")
    void lockedHrResetPasswordSucceedsButLoginStillBlocked() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("resetlocked@company.com", "HR Reset Locked", companyId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();

        // Staff lock HR
        LockHrAccountAdminRequest lockReq = new LockHrAccountAdminRequest("Khóa vì vi phạm kỷ luật");
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/lock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockReq)))
            .andExpect(status().isOk());

        // Call forgot-password
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("resetlocked@company.com"))))
            .andExpect(status().isOk());

        // Create reset token directly in DB to simulate email click
        String rawToken = "raw-test-reset-token-locked-hr";
        String tokenHash = sha256(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
            UUID.fromString(userId),
            tokenHash,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            Instant.now()
        );
        passwordResetTokenRepository.save(resetToken);

        // Call reset-password
        String newPassword = "BrandNewPassword123@";
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, newPassword))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", notNullValue()));

        // Check DB: user is STILL LOCKED, lockedReason is STILL "Khóa vì vi phạm kỷ luật"
        User userInDb = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        assertThat(userInDb.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(userInDb.getLockedReason()).isEqualTo("Khóa vì vi phạm kỷ luật");
        assertThat(userInDb.getLockedUntil()).isEqualTo(Instant.parse("9999-12-31T23:59:59Z"));
        assertThat(passwordEncoder.matches(newPassword, userInDb.getPasswordHash())).isTrue();

        // Login with new password -> returns 423 with manual lock message
        LoginRequest loginReq = new LoginRequest("resetlocked@company.com", newPassword);
        mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")))
            .andExpect(jsonPath("$.message", is("Tài khoản đã bị khoá. Vui lòng liên hệ cán bộ để được mở khoá.")));
    }

    @Test
    @DisplayName("11. HR có token hợp lệ, staff khoá HR -> HR gọi GET /api/v1/auth/me bằng token cũ nhận 403; Unlock xong -> login lại và /me dùng được")
    void hrTokenRevokedOnLockAndRestoredOnUnlock() throws Exception {
        UUID companyId = UUID.randomUUID();
        CreateHrAccountAdminRequest req = new CreateHrAccountAdminRequest("tokenrevoked@company.com", "HR Token Check", companyId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/auth/internal/hr-accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();
        String tempPass = root.path("data").path("temporaryPassword").asText();

        // HR logs in to get active token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("tokenrevoked@company.com", tempPass))))
            .andExpect(status().isOk())
            .andReturn();

        String hrJwt = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("accessToken").asText();

        // HR accesses /api/v1/auth/me successfully
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("tokenrevoked@company.com")));

        // Staff locks HR
        LockHrAccountAdminRequest lockReq = new LockHrAccountAdminRequest("Kỷ luật đình chỉ");
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/lock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockReq)))
            .andExpect(status().isOk());

        // HR accesses /api/v1/auth/me with old token -> HTTP 403 {"success":false,"statusCode":403,"message":"Tài khoản đã bị khoá"}
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrJwt))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.statusCode", is(403)))
            .andExpect(jsonPath("$.message", is("Tài khoản đã bị khoá")));

        // Staff unlocks HR
        mockMvc.perform(post("/api/v1/auth/internal/hr-accounts/{id}/unlock", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isOk());

        // Login again -> succeeds
        MvcResult reloginResult = mockMvc.perform(post("/api/v1/auth/hr/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("tokenrevoked@company.com", tempPass))))
            .andExpect(status().isOk())
            .andReturn();

        String newHrJwt = objectMapper.readTree(reloginResult.getResponse().getContentAsString()).path("accessToken").asText();

        // /api/v1/auth/me succeeds with new token
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newHrJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("tokenrevoked@company.com")));
    }

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}


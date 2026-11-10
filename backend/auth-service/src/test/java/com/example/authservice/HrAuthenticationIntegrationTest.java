package com.example.authservice;

import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.entity.PasswordResetToken;
import com.example.authservice.entity.User;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import com.example.authservice.repository.PasswordResetTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HrAuthenticationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. HR đăng nhập thành công bằng email và mật khẩu - trả về JWT và thông tin user")
    void hrLoginSuccess() throws Exception {
        User hrUser = new User(
            null,
            "hr1@rikkei.vn",
            "HR Manager",
            "ROLE_HR",
            passwordEncoder.encode("Password123@"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr1@rikkei.vn", "Password123@"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()))
            .andExpect(jsonPath("$.tokenType", is("Bearer")))
            .andExpect(jsonPath("$.user.email", is("hr1@rikkei.vn")))
            .andExpect(jsonPath("$.user.fullName", is("HR Manager")))
            .andExpect(jsonPath("$.user.role", is("ROLE_HR")));
    }

    @Test
    @DisplayName("2. HR đăng nhập sai mật khẩu - trả 401 INVALID_CREDENTIALS và tăng bộ đếm")
    void hrLoginWrongPassword() throws Exception {
        User hrUser = new User(
            null,
            "hr2@rikkei.vn",
            "HR Specialist",
            "ROLE_HR",
            passwordEncoder.encode("CorrectPassword123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr2@rikkei.vn", "WrongPassword!"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));

        User updated = userRepository.findByEmail("hr2@rikkei.vn").orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(updated.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("2b. HR đăng nhập bằng email không tồn tại - trả 401 INVALID_CREDENTIALS và không cấp JWT")
    void hrLoginEmailNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("nonexistent@rikkei.vn", "Password123@"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
            .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("3. Đăng nhập sai 5 lần liên tiếp - tài khoản bị khóa 15 phút")
    void hrAccountLockoutAfter5FailedAttempts() throws Exception {
        User hrUser = new User(
            null,
            "hr3@rikkei.vn",
            "HR Officer",
            "ROLE_HR",
            passwordEncoder.encode("RealPassword"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        // First 4 failed attempts -> 401
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("hr3@rikkei.vn", "Wrong" + i))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
        }

        // 5th failed attempt -> 423 LOCKED
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr3@rikkei.vn", "Wrong5"))))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));

        User lockedUser = userRepository.findByEmail("hr3@rikkei.vn").orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now().plus(14, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("4. Tài khoản đang bị khóa không thể đăng nhập kể cả khi nhập đúng mật khẩu")
    void hrAccountLockedCannotLoginEvenWithCorrectPassword() throws Exception {
        User hrUser = new User(
            null,
            "hr4@rikkei.vn",
            "HR Locked",
            "ROLE_HR",
            passwordEncoder.encode("RealPassword"),
            Instant.now(),
            Instant.now()
        );
        hrUser.setFailedLoginAttempts(5);
        hrUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(hrUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr4@rikkei.vn", "RealPassword"))))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));
    }

    @Test
    @DisplayName("5. Hết 15 phút khóa - tài khoản tự động mở và đăng nhập bình thường")
    void hrAccountAutoUnlockAfter15Minutes() throws Exception {
        User hrUser = new User(
            null,
            "hr5@rikkei.vn",
            "HR Unlocked",
            "ROLE_HR",
            passwordEncoder.encode("RealPassword"),
            Instant.now(),
            Instant.now()
        );
        hrUser.setFailedLoginAttempts(5);
        hrUser.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        userRepository.save(hrUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr5@rikkei.vn", "RealPassword"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()));

        User resetUser = userRepository.findByEmail("hr5@rikkei.vn").orElseThrow();
        assertThat(resetUser.getFailedLoginAttempts()).isZero();
        assertThat(resetUser.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("6. Quên mật khẩu - gửi email kèm token và lưu hash token trong database")
    void hrForgotPasswordSendsEmail() throws Exception {
        User hrUser = new User(
            null,
            "hr6@rikkei.vn",
            "HR Forgot",
            "ROLE_HR",
            passwordEncoder.encode("OldPassword"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("hr6@rikkei.vn"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", notNullValue()));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("hr6@rikkei.vn"), tokenCaptor.capture());
        String rawToken = tokenCaptor.getValue();
        assertThat(rawToken).isNotBlank();

        // Verify database stores token_hash instead of plain rawToken
        String expectedHash = sha256(rawToken);
        PasswordResetToken storedToken = passwordResetTokenRepository.findByTokenHash(expectedHash).orElseThrow();
        assertThat(storedToken.getUserId()).isEqualTo(hrUser.getId());
        assertThat(storedToken.getExpiresAt()).isAfter(Instant.now().plus(28, ChronoUnit.MINUTES));
        assertThat(storedToken.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("6b. Quên mật khẩu với email không tồn tại - vẫn trả 200 thông báo chung và không gửi email")
    void hrForgotPasswordNonExistentEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("unknown@rikkei.vn"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", notNullValue()));

        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("7. Đặt lại mật khẩu thành công bằng token hợp lệ - mật khẩu mới đăng nhập được")
    void hrResetPasswordSuccess() throws Exception {
        User hrUser = new User(
            null,
            "hr7@rikkei.vn",
            "HR Reset",
            "ROLE_HR",
            passwordEncoder.encode("OldPassword123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String rawToken = "valid-reset-token-777";
        String tokenHash = sha256(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
            hrUser.getId(),
            tokenHash,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            Instant.now()
        );
        passwordResetTokenRepository.save(resetToken);

        // Reset password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "NewBrandPassword2026@"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", notNullValue()));

        // Token marked used
        PasswordResetToken usedToken = passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(usedToken.getUsedAt()).isNotNull();

        // Old password fails
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr7@rikkei.vn", "OldPassword123"))))
            .andExpect(status().isUnauthorized());

        // New password succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr7@rikkei.vn", "NewBrandPassword2026@"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()));

        // Second reset attempt with same token fails immediately
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "AnotherPassword2026@"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("RESET_TOKEN_ALREADY_USED")));
    }

    @Test
    @DisplayName("8. Token khôi phục mật khẩu đã hết hạn sau 30 phút - bị từ chối")
    void hrResetPasswordExpiredToken() throws Exception {
        User hrUser = new User(
            null,
            "hr8@rikkei.vn",
            "HR Expired",
            "ROLE_HR",
            passwordEncoder.encode("Pass123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String rawToken = "expired-token-888";
        String tokenHash = sha256(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
            hrUser.getId(),
            tokenHash,
            Instant.now().minus(5, ChronoUnit.MINUTES),
            Instant.now().minus(35, ChronoUnit.MINUTES)
        );
        passwordResetTokenRepository.save(resetToken);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "NewPass456"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("RESET_TOKEN_EXPIRED")));
    }

    @Test
    @DisplayName("9. Token khôi phục mật khẩu đã dùng - không thể sử dụng lại")
    void hrResetPasswordAlreadyUsedToken() throws Exception {
        User hrUser = new User(
            null,
            "hr9@rikkei.vn",
            "HR Reused",
            "ROLE_HR",
            passwordEncoder.encode("Pass123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String rawToken = "already-used-token-999";
        String tokenHash = sha256(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
            hrUser.getId(),
            tokenHash,
            Instant.now().plus(20, ChronoUnit.MINUTES),
            Instant.now()
        );
        resetToken.setUsedAt(Instant.now().minusSeconds(60));
        passwordResetTokenRepository.save(resetToken);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "NewPass789"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("RESET_TOKEN_ALREADY_USED")));
    }

    @Test
    @DisplayName("10. JWT của HR hoạt động chính xác với Spring Security trên endpoint được bảo vệ")
    void hrJwtProtectedEndpointAccess() throws Exception {
        User hrUser = new User(
            null,
            "hr10@rikkei.vn",
            "HR Security Test",
            "ROLE_HR",
            passwordEncoder.encode("SecurePass123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr10@rikkei.vn", "SecurePass123"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseStr).get("accessToken").asText();

        // Access /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("hr10@rikkei.vn")))
            .andExpect(jsonPath("$.role", is("ROLE_HR")));
    }

    @Test
    @DisplayName("11. Tài khoản LMS SSO không thể đăng nhập bằng mật khẩu khi không có mật khẩu")
    void ssoUserCannotLoginWithPassword() throws Exception {
        User ssoUser = new User(
            "lms-student-99",
            "student99@rikkei.edu.vn",
            "SSO Student",
            "ROLE_CANDIDATE",
            null,
            Instant.now(),
            Instant.now()
        );
        userRepository.save(ssoUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("student99@rikkei.edu.vn", "AnyPass"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("12. Xử lý đồng thời hai request sử dụng cùng reset token - chỉ một request thành công")
    void concurrentResetPasswordRequestsSameToken() throws Exception {
        User hrUser = new User(
            null,
            "hr12@rikkei.vn",
            "HR Concurrency Test",
            "ROLE_HR",
            passwordEncoder.encode("OldPass123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String rawToken = "concurrent-reset-token-12";
        String tokenHash = sha256(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
            hrUser.getId(),
            tokenHash,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            Instant.now()
        );
        passwordResetTokenRepository.save(resetToken);

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> call1 = () -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "NewPassThread1@"))))
                .andReturn().getResponse().getStatus();
        };
        Callable<Integer> call2 = () -> {
            startLatch.await();
            return mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "NewPassThread2@"))))
                .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = executor.submit(call1);
        Future<Integer> f2 = executor.submit(call2);
        startLatch.countDown();

        int s1 = f1.get();
        int s2 = f2.get();
        executor.shutdown();

        // One request must succeed (200 OK) and the other must be rejected (400 Bad Request)
        assertThat(List.of(s1, s2)).containsExactlyInAnyOrder(200, 400);
    }

    @Test
    @DisplayName("13. Đặt lại mật khẩu thành công sẽ vô hiệu hóa tất cả các reset token còn lại của tài khoản")
    void resetPasswordInvalidatesOtherPendingTokensForSameUser() throws Exception {
        User hrUser = new User(
            null,
            "hr13@rikkei.vn",
            "HR Invalidation Test",
            "ROLE_HR",
            passwordEncoder.encode("InitialPassword123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String token1 = "token-first-13";
        String token2 = "token-second-13";

        passwordResetTokenRepository.save(new PasswordResetToken(
            hrUser.getId(),
            sha256(token1),
            Instant.now().plus(30, ChronoUnit.MINUTES),
            Instant.now()
        ));
        passwordResetTokenRepository.save(new PasswordResetToken(
            hrUser.getId(),
            sha256(token2),
            Instant.now().plus(30, ChronoUnit.MINUTES),
            Instant.now()
        ));

        // Use token2 to reset password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token2, "BrandNewPassword123@"))))
            .andExpect(status().isOk());

        // Token1 must now be marked used and rejected
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token1, "AnotherPassword123@"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("RESET_TOKEN_ALREADY_USED")));
    }

    @Test
    @DisplayName("14. Khi dịch vụ gửi mail gặp sự cố - trả lỗi 503 EXTERNAL_UNAVAILABLE và không lưu token")
    void forgotPasswordWhenEmailServiceFailsReturns503() throws Exception {
        User hrUser = new User(
            null,
            "hr14@rikkei.vn",
            "HR Smtp Fail Test",
            "ROLE_HR",
            passwordEncoder.encode("ValidPass123"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        doThrow(new SsoException(ErrorCode.EXTERNAL_UNAVAILABLE, "SMTP server unreachable"))
            .when(emailService).sendPasswordResetEmail(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("hr14@rikkei.vn"))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code", is("EXTERNAL_UNAVAILABLE")));

        // Transaction rollback: no reset token should be saved in DB
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("15. Không làm lộ mật khẩu, mã băm hoặc thông tin nội bộ trong API response")
    void noSensitiveInformationExposedInApiResponses() throws Exception {
        User hrUser = new User(
            null,
            "hr15@rikkei.vn",
            "HR Privacy Test",
            "ROLE_HR",
            passwordEncoder.encode("PrivatePassword123@"),
            Instant.now(),
            Instant.now()
        );
        userRepository.save(hrUser);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("hr15@rikkei.vn", "PrivatePassword123@"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(loginResponse).doesNotContain("password");
        assertThat(loginResponse).doesNotContain("passwordHash");
        assertThat(loginResponse).doesNotContain("PrivatePassword123@");
        assertThat(loginResponse).doesNotContain("lockedUntil");
        assertThat(loginResponse).doesNotContain("failedLoginAttempts");
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

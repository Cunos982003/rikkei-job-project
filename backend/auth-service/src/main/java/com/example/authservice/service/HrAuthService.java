package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.MessageResponse;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.dto.UserDto;
import com.example.authservice.entity.PasswordResetToken;
import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import com.example.authservice.repository.PasswordResetTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrAuthService {

    private static final Logger log = LoggerFactory.getLogger(HrAuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15L;
    private static final long RESET_TOKEN_EXPIRATION_MINUTES = 30L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public HrAuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    @Transactional(noRollbackFor = SsoException.class)
    public AuthResponse login(LoginRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";
        User user = userRepository.findByEmailForUpdate(email)
            .orElseThrow(() -> new SsoException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        Instant now = Instant.now();

        if (user.getStatus() == UserStatus.LOCKED || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                long remainingMinutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
                log.warn("Login attempt for locked account: {}, locked until {}", email, user.getLockedUntil());
                throw new SsoException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked due to 5 consecutive failed login attempts. Please try again in " + remainingMinutes + " minute(s)."
                );
            }
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failedAttempts);

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(now.plus(Duration.ofMinutes(LOCKOUT_DURATION_MINUTES)));
                user.setFailedLoginCount(MAX_FAILED_ATTEMPTS);
                userRepository.saveAndFlush(user);
                log.warn("Account {} exceeded max failed attempts, locked for 15 minutes", email);
                throw new SsoException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account has been locked for 15 minutes due to 5 consecutive failed login attempts."
                );
            }

            userRepository.saveAndFlush(user);
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(now);
        userRepository.saveAndFlush(user);

        String accessToken = jwtTokenProvider.generateToken(user);
        return new AuthResponse(
            accessToken,
            "Bearer",
            jwtTokenProvider.getExpirationSeconds(),
            UserDto.from(user)
        );
    }

    @Transactional
    public MessageResponse changePassword(String email, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Password must be at least 8 characters long and contain both letters and numbers");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new SsoException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return new MessageResponse("Password changed successfully.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";

        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
                String rawToken = generateSecureToken();
                String tokenHash = hashToken(rawToken);
                Instant now = Instant.now();
                Instant expiresAt = now.plus(Duration.ofMinutes(RESET_TOKEN_EXPIRATION_MINUTES));

                PasswordResetToken resetToken = new PasswordResetToken(user.getId(), tokenHash, expiresAt, now);
                passwordResetTokenRepository.save(resetToken);

                emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
            }
        });

        return new MessageResponse("If the email is registered, password reset instructions have been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String rawToken = request.token() != null ? request.token().trim() : "";
        if (rawToken.isBlank()) {
            throw new SsoException(ErrorCode.RESET_TOKEN_INVALID, "Token is required");
        }

        String tokenHash = hashToken(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new SsoException(ErrorCode.RESET_TOKEN_INVALID, "Invalid or unrecognized password reset token"));

        if (resetToken.isUsed()) {
            throw new SsoException(ErrorCode.RESET_TOKEN_ALREADY_USED, "Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new SsoException(ErrorCode.RESET_TOKEN_EXPIRED, "Password reset token has expired (valid for 30 minutes)");
        }

        if (request.newPassword() == null || request.newPassword().length() < 8 || !request.newPassword().matches(".*[A-Za-z].*") || !request.newPassword().matches(".*[0-9].*")) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Password must be at least 8 characters long and contain both letters and numbers");
        }

        User user = userRepository.findById(resetToken.getUserId())
            .orElseThrow(() -> new SsoException(ErrorCode.RESET_TOKEN_INVALID, "User associated with reset token was not found"));

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setMustChangePassword(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.invalidateAllUnusedTokensForUser(user.getId(), now);

        return new MessageResponse("Password has been successfully reset. You can now log in with your new password.");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

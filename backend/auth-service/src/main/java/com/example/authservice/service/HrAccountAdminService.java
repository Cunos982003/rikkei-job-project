package com.example.authservice.service;

import com.example.authservice.dto.CreateHrAccountAdminRequest;
import com.example.authservice.dto.HrAccountAdminResponse;
import com.example.authservice.entity.User;
import com.example.authservice.entity.UserRole;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class HrAccountAdminService {

    private static final Logger log = LoggerFactory.getLogger(HrAccountAdminService.class);

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$!%&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromEmail;

    public HrAccountAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:no-reply@rikkei-jobs.vn}") String fromEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.fromEmail = fromEmail;
    }

    public static String generateTemporaryPassword() {
        List<Character> chars = new ArrayList<>();
        chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        chars.add(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        for (int i = 4; i < 12; i++) {
            chars.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Transactional
    public HrAccountAdminResponse createHrAccount(CreateHrAccountAdminRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        String temporaryPassword = generateTemporaryPassword();

        User user = new User();
        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setCompanyId(req.companyId());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole(UserRole.HR);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);

        log.info("==================================================");
        log.info("🔑 CẤP TÀI KHOẢN HR THÀNH CÔNG CHO: {}", email);
        log.info("👉 Mật khẩu ngẫu nhiên tạm thời: {}", temporaryPassword);
        log.info("==================================================");

        boolean emailConfigured = mailSenderProvider.getIfAvailable() != null;
        if (emailConfigured) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                sendTemporaryPasswordEmail(email, req.fullName().trim(), temporaryPassword);
            });
        }

        return toResponse(saved, temporaryPassword, emailConfigured);
    }

    @Transactional
    public HrAccountAdminResponse lockHrAccount(UUID id, String reason) {
        User user = userRepository.findById(id)
                .filter(this::isHrUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản HR"));

        user.setStatus(UserStatus.LOCKED);
        user.setLockedReason(reason);
        user.setLockedAt(Instant.now());
        user.setLockedUntil(Instant.parse("9999-12-31T23:59:59Z"));
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);
        return toResponse(saved, null, false);
    }

    @Transactional
    public HrAccountAdminResponse unlockHrAccount(UUID id) {
        User user = userRepository.findById(id)
                .filter(this::isHrUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản HR"));

        user.setStatus(UserStatus.ACTIVE);
        user.setLockedReason(null);
        user.setLockedAt(null);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);
        return toResponse(saved, null, false);
    }

    @Transactional(readOnly = true)
    public List<HrAccountAdminResponse> listHrAccounts(UUID companyId) {
        List<User> hrUsers = userRepository.findHrAccountsByCompanyId(companyId);
        return hrUsers.stream()
                .map(u -> toResponse(u, null, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countHrAccounts(List<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (companyIds.size() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng companyIds không được vượt quá 200");
        }

        Map<String, Long> result = new HashMap<>();
        for (UUID cid : companyIds) {
            result.put(cid.toString(), 0L);
        }

        List<Object[]> rows = userRepository.countHrAccountsByCompanyIds(companyIds);
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                UUID cid = (UUID) row[0];
                Long count = ((Number) row[1]).longValue();
                result.put(cid.toString(), count);
            }
        }
        return result;
    }

    private boolean isHrUser(User user) {
        return user != null && user.getRole() == UserRole.HR;
    }

    private boolean sendTemporaryPasswordEmail(String toEmail, String fullName, String temporaryPassword) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("⚠️ JavaMailSender chưa được cấu hình hoặc chưa khởi tạo bean. Bỏ qua gửi email tới {}", toEmail);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Thông tin tài khoản HR - Rikkei Jobs");
            message.setText("Xin chào " + fullName + ",\n\n"
                    + "Tài khoản HR của bạn tại Rikkei Jobs đã được tạo thành công.\n"
                    + "Thông tin đăng nhập:\n"
                    + "- Email: " + toEmail + "\n"
                    + "- Mật khẩu tạm thời: " + temporaryPassword + "\n\n"
                    + "Vui lòng đổi mật khẩu trong lần đăng nhập đầu tiên.\n\n"
                    + "Trân trọng,\nĐội ngũ Rikkei Jobs");
            mailSender.send(message);
            log.info("✅ Đã gửi email thông tin tài khoản HR thành công tới {}", toEmail);
            return true;
        } catch (Exception ex) {
            log.error("❌ Gửi email tới {} thất bại: [{}] {}", toEmail, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return false;
        }
    }

    private HrAccountAdminResponse toResponse(User user, String temporaryPassword, boolean emailSent) {
        return new HrAccountAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCompanyId(),
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                user.getLockedReason(),
                user.getLockedAt(),
                user.isMustChangePassword(),
                user.getCreatedAt(),
                temporaryPassword,
                emailSent);
    }
}

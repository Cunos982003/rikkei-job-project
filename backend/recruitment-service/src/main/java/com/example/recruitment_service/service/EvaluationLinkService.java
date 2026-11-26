package com.example.recruitment_service.service;

import com.example.recruitment_service.dto.EvaluationFormResponse;
import com.example.recruitment_service.entity.EvaluationLink;
import com.example.recruitment_service.exception.InvalidEvaluationLinkException;
import com.example.recruitment_service.repository.EvaluationLinkRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class EvaluationLinkService {

    private final EvaluationLinkRepository linkRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public EvaluationLinkService(EvaluationLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Transactional
    public String createLink(String mentorEmail, UUID lmsUserId, UUID batchId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = sha256Hex(rawToken);

        EvaluationLink link = new EvaluationLink();
        link.setTokenHash(tokenHash);
        link.setMentorEmail(mentorEmail);
        link.setUserId(lmsUserId);
        link.setBatchId(batchId);
        link.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        linkRepository.save(link);

        return frontendBaseUrl + "/evaluate/" + rawToken;
    }

    @Transactional(readOnly = true)
    public EvaluationLink resolveToken(String rawToken) {
        String hash = sha256Hex(rawToken);
        EvaluationLink link = linkRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidEvaluationLinkException("Link không hợp lệ hoặc đã bị thay đổi"));

        if (link.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidEvaluationLinkException("Link đã hết hạn. Vui lòng liên hệ cán bộ quản lý thực tập.");
        }
        return link;
    }

    @Transactional
    public EvaluationFormResponse resolveAndBuildForm(String rawToken) {
        EvaluationLink link = resolveToken(rawToken);

        // TODO: 1. Tìm hoặc tạo tài khoản MENTOR (gọi service auth/user hiện có)
        // TODO: 2. Cấp phiên MENTOR (JWT) nếu dự án yêu cầu
        // TODO: 3. Tìm phiếu DG-01 theo (userId, batchId). Nếu chưa có thì tạo nháp DRAFT.

        // Tạm thời trả về mock data (Hãy thay thế bằng logic thật của bạn)
        UUID evaluationId = link.getEvaluationId() != null ? link.getEvaluationId() : UUID.randomUUID();
        String status = "DRAFT";
        boolean readOnly = "SUBMITTED".equals(status);

        return new EvaluationFormResponse(
                evaluationId,
                status,
                readOnly,
                "Thực tập sinh",
                "Đợt thực tập"
        );
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể băm token", e);
        }
    }
}
package com.example.learningservice.security;

import com.example.learningservice.exception.ForbiddenException;
import com.example.learningservice.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Component
public class UserContextHelper {

    private static final String DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final String jwtSecret;

    public UserContextHelper() {
        this.jwtSecret = DEFAULT_SECRET;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserContextHelper(@Value("${app.jwt.secret:" + DEFAULT_SECRET + "}") String jwtSecret) {
        this.jwtSecret = (jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : DEFAULT_SECRET;
    }

    public UUID resolveCandidateUserId(String headerUserId, String headerUserRole, String authHeader) {
        validateCandidateRole(headerUserRole);

        if (headerUserId != null && !headerUserId.isBlank()) {
            return parseOrDeterministicUuid(headerUserId.trim());
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = parseJwtClaims(token);
                Object roleClaim = claims.get("role");
                if (roleClaim != null) {
                    validateCandidateRole(roleClaim.toString());
                }
                String subject = claims.get("userId") != null
                        ? claims.get("userId").toString()
                        : claims.getSubject();
                if (subject == null || subject.isBlank()) {
                    throw new UnauthorizedException("Token không chứa thông tin định danh người dùng.");
                }
                return parseOrDeterministicUuid(subject.trim());
            } catch (ForbiddenException | UnauthorizedException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Lỗi giải mã JWT: {}", e.getMessage());
                throw new UnauthorizedException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
            }
        }

        throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện thao tác này.");
    }

    private void validateCandidateRole(String role) {
        if (role != null && !role.isBlank()) {
            String normalized = role.trim().toUpperCase().replace("ROLE_", "");
            if (!"CANDIDATE".equals(normalized)) {
                throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này. Chức năng chỉ dành cho Ứng viên (CANDIDATE).");
            }
        }
    }

    private UUID parseOrDeterministicUuid(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Claims parseJwtClaims(String token) {
        // Ưu tiên cách mã hóa UTF-8 bytes đồng bộ với auth-service
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // Dự phòng giải mã qua HexFormat nếu token được ký bằng key Hex
            try {
                SecretKey hexKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtSecret));
                return Jwts.parser()
                        .verifyWith(hexKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
                throw e;
            }
        }
    }
}

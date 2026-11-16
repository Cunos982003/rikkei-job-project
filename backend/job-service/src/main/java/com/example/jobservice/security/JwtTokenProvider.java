
package com.example.jobservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("userId", principal.getId().toString())
                .claim("fullName", principal.getFullName())
                .claim("role", principal.getRole())
                .claim("mustChangePassword", principal.isMustChangePassword())
                .claim(
                        "companyId",
                        principal.getCompanyId() != null
                                ? principal.getCompanyId().toString()
                                : null)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userIdClaim = claims.get("userId", String.class);
        UUID userId = null;
        if (userIdClaim != null && !userIdClaim.isBlank()) {
            userId = UUID.fromString(userIdClaim);
        } else {
            try {
                userId = UUID.fromString(claims.getSubject());
            } catch (Exception ignored) {
            }
        }

        String companyIdClaim = claims.get("companyId", String.class);
        UUID companyId = (companyIdClaim != null && !companyIdClaim.isBlank())
                ? UUID.fromString(companyIdClaim)
                : null;

        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            email = claims.getSubject();
        }

        String fullName = claims.get("fullName", String.class);
        String role = claims.get("role", String.class);
        if (role == null) {
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof java.util.List<?> rolesList && !rolesList.isEmpty()) {
                role = String.valueOf(rolesList.get(0));
            }
        }
        Boolean mustChangePassword = claims.get("mustChangePassword", Boolean.class);

        return UserPrincipal.of(
                userId,
                email,
                fullName,
                role,
                companyId,
                Boolean.TRUE.equals(mustChangePassword));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

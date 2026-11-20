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
            @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
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
                .claim("userId", principal.getId() != null ? principal.getId().toString() : null)
                .claim("fullName", principal.getFullName())
                .claim("role", principal.getRole())
                .claim(
                        "companyId",
                        principal.getCompanyId() != null
                                ? principal.getCompanyId().toString()
                                : null)
                .claim("mustChangePassword", principal.isMustChangePassword())
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
        String companyIdClaim = claims.get("companyId", String.class);

        UUID userId = userIdClaim != null
                ? UUID.fromString(userIdClaim)
                : null;

        UUID companyId = companyIdClaim != null
                ? UUID.fromString(companyIdClaim)
                : null;

        String emailClaim = claims.get("email", String.class);
        String email = emailClaim != null && !emailClaim.isBlank() ? emailClaim : claims.getSubject();
        String fullName = claims.get("fullName", String.class);
        String role = claims.get("role", String.class);
        Boolean mustChangePassword = claims.get("mustChangePassword", Boolean.class);

        return UserPrincipal.of(
                userId,
                email,
                fullName,
                role,
                companyId,
                Boolean.TRUE.equals(mustChangePassword));
    }
}

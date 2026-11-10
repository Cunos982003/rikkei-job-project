package com.example.authservice.security;

import com.example.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(
        @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
        @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        String role = user.getRole() == null ? "CANDIDATE" : user.getRole();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("userId", user.getId().toString())
            .claim("companyId", user.getCompanyId() != null ? user.getCompanyId().toString() : null)
            .claim("lmsUserId", user.getLmsUserId())
            .claim("email", user.getEmail())
            .claim("fullName", user.getFullName())
            .claim("role", role)
            .claim("roles", List.of(role))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        List<SimpleGrantedAuthority> authorities;
        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get("roles", List.class);
        if (rolesList != null && !rolesList.isEmpty()) {
            authorities = rolesList.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList();
        } else {
            String role = claims.get("role", String.class);
            if (role == null || role.isBlank()) {
                role = "ROLE_CANDIDATE";
            }
            authorities = List.of(new SimpleGrantedAuthority(role));
        }
        String principal = claims.getSubject();

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}


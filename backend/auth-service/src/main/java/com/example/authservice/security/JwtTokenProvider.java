package com.example.authservice.security;

import com.example.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
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
        Environment environment,
        @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        String secret = ProductionJwtSecretValidator.requireValidSecret(environment);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required. Set app.jwt.secret via environment configuration.");
        }

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
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID is required to generate a JWT");
        }

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        String role = RoleMapper.normalizeToSecurityRole(user.getRole() == null ? "CANDIDATE" : user.getRole());

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
        validateUserIdClaim(claims);
        List<SimpleGrantedAuthority> authorities = resolveAuthorities(claims);
        String principal = resolvePrincipal(claims);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private String resolvePrincipal(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("JWT is missing the required subject claim");
        }
        try {
            UUID.fromString(subject);
            return subject;
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT subject is not a valid UUID");
        }
    }

    private List<SimpleGrantedAuthority> resolveAuthorities(Claims claims) {
        Object rawRoles = claims.get("roles");
        if (rawRoles != null) {
            List<String> normalizedRoles = normalizeRoleList(rawRoles);
            if (!normalizedRoles.isEmpty()) {
                return normalizedRoles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            }
            throw new BadCredentialsException("JWT roles claim is invalid");
        }

        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new BadCredentialsException("JWT is missing the required role claim");
        }

        String normalizedRole = normalizeRoleOrThrow(role, "role");
        return List.of(new SimpleGrantedAuthority(normalizedRole));
    }

    private List<String> normalizeRoleList(Object rawRoles) {
        if (rawRoles instanceof List<?> rolesList) {
            List<String> normalizedRoles = new ArrayList<>();
            for (Object element : rolesList) {
                if (element == null || String.valueOf(element).isBlank()) {
                    throw new BadCredentialsException("JWT roles claim contains a blank role");
                }
                normalizedRoles.add(normalizeRoleOrThrow(String.valueOf(element), "roles"));
            }
            return normalizedRoles;
        }

        if (rawRoles instanceof String[] roleArray) {
            List<String> normalizedRoles = new ArrayList<>();
            for (String role : roleArray) {
                if (role == null || role.isBlank()) {
                    throw new BadCredentialsException("JWT roles claim contains a blank role");
                }
                normalizedRoles.add(normalizeRoleOrThrow(role, "roles"));
            }
            return normalizedRoles;
        }

        throw new BadCredentialsException("JWT roles claim is not a valid array of role values");
    }

    private void validateUserIdClaim(Claims claims) {
        Object userIdClaim = claims.get("userId");
        if (userIdClaim == null) {
            throw new BadCredentialsException("JWT is missing the required userId claim");
        }
        try {
            UUID.fromString(String.valueOf(userIdClaim));
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT userId claim is not a valid UUID");
        }
    }

    private String normalizeRoleOrThrow(String rawRole, String claimName) {
        try {
            return RoleMapper.normalizeToSecurityRole(rawRole);
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT " + claimName + " claim is invalid: " + rawRole, ex);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}


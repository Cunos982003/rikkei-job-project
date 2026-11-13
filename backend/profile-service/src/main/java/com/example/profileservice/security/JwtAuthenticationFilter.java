package com.example.profileservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> VALID_ROLES = Set.of("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN");

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret:${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required. Set app.jwt.secret via environment configuration.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] pad = new byte[32];
            System.arraycopy(keyBytes, 0, pad, 0, keyBytes.length);
            keyBytes = pad;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication != null && existingAuthentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String subject = claims.getSubject();
                if (!StringUtils.hasText(subject)) {
                    throw new JwtException("JWT subject is missing");
                }
                UUID userId = UUID.fromString(subject);

                String role = claims.get("role", String.class);
                if (!StringUtils.hasText(role)) {
                    throw new JwtException("JWT role is missing");
                }
                String normalizedRole = normalizeRole(role);
                if (!VALID_ROLES.contains(normalizedRole)) {
                    throw new JwtException("JWT role is invalid: " + role);
                }

                UserPrincipal principal = UserPrincipal.of(userId, userId.toString(), normalizedRole);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, token, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) {
            return null;
        }
        String normalized = rawRole.trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}

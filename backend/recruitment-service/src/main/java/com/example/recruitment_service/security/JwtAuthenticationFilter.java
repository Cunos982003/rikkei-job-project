package com.example.recruitment_service.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(header.substring(BEARER_PREFIX.length()).trim())
                        .getPayload();

                String userId = claims.get("userId", String.class);
                if (userId == null) {
                    userId = claims.getSubject();
                }
                if (userId == null || userId.isBlank()) {
                    throw new IllegalArgumentException("JWT user ID is missing");
                }
                String role = claims.get("role", String.class);
                if (role == null || role.isBlank()) {
                    throw new IllegalArgumentException("JWT role is missing");
                }
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                String companyId = claims.get("companyId", String.class);
                UserPrincipal principal = new UserPrincipal(
                        UUID.fromString(userId),
                        authority.substring("ROLE_".length()),
                        companyId == null || companyId.isBlank() ? null : UUID.fromString(companyId));
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, java.util.List.of(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

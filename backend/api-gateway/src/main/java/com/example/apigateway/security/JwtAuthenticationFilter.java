package com.example.apigateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecretKey key;

    JwtAuthenticationFilter(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (!header.startsWith("Bearer ")) {
                GatewayError.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Yêu cầu xác thực");
                return;
            }
            try {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7).trim()).getPayload();
                if (claims.getExpiration() == null) {
                    throw new IllegalArgumentException("JWT expiration is missing");
                }
                String userId = claims.get("userId", String.class);
                if (userId == null || userId.isBlank()) {
                    userId = claims.getSubject();
                }
                if (userId == null || userId.isBlank()) {
                    throw new IllegalArgumentException("JWT user ID is missing");
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                GatewayError.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Token không hợp lệ");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

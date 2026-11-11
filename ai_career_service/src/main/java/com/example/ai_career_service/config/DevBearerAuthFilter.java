package com.example.ai_career_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Dev-only: trusts the "sub" claim of any Bearer JWT WITHOUT verifying its signature.
 * Active only with the "dev" profile.
 */
@Component
@Profile("dev")
public class DevBearerAuthFilter extends OncePerRequestFilter {

    private static final Pattern SUB = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String subject = extractSubject(request.getHeader("Authorization"));
        if (subject == null) {
            chain.doFilter(request, response);
            return;
        }
        Principal principal = () -> subject;
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }
        }, response);
    }

    private String extractSubject(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String[] parts = header.substring(7).trim().split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = SUB.matcher(payload);
            return matcher.find() ? matcher.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

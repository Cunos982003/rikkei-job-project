package com.example.profileservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        String resolvedUserId = null;
        String resolvedRole = null;

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            resolvedUserId = jwtTokenProvider.getUserId(token);
            List<String> roles = jwtTokenProvider.getRoles(token);
            resolvedRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "ROLE_CANDIDATE";
        } else {
            String headerUserId = request.getHeader("X-User-Id");
            if (StringUtils.hasText(headerUserId)) {
                resolvedUserId = headerUserId.trim();
                resolvedRole = request.getHeader("X-User-Role");
                if (resolvedRole == null || resolvedRole.isBlank()) {
                    resolvedRole = "ROLE_CANDIDATE";
                } else if (!resolvedRole.startsWith("ROLE_")) {
                    resolvedRole = "ROLE_" + resolvedRole;
                }
                var authorities = List.of(new SimpleGrantedAuthority(resolvedRole));
                var authentication = new UsernamePasswordAuthenticationToken(resolvedUserId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        HttpServletRequest effectiveRequest = request;
        if (resolvedUserId != null && !StringUtils.hasText(request.getHeader("X-User-Id"))) {
            final String finalUserId = resolvedUserId;
            final String finalRole = resolvedRole != null ? resolvedRole : "ROLE_CANDIDATE";
            effectiveRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return finalUserId;
                    }
                    if ("X-User-Role".equalsIgnoreCase(name)) {
                        return finalRole;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return Collections.enumeration(List.of(finalUserId));
                    }
                    if ("X-User-Role".equalsIgnoreCase(name)) {
                        return Collections.enumeration(List.of(finalRole));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                    if (!names.contains("X-User-Id")) names.add("X-User-Id");
                    if (!names.contains("X-User-Role")) names.add("X-User-Role");
                    return Collections.enumeration(names);
                }
            };
        }

        filterChain.doFilter(effectiveRequest, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
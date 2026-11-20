package com.example.apigateway.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class IdentityHeaderFilter extends OncePerRequestFilter {
    private static final String USER_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        String userRole = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = authentication.getName();
            if (authentication.getPrincipal() instanceof GatewayPrincipal gp) {
                userRole = gp.role();
            } else {
                userRole = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                        .findFirst().orElse(null);
            }
        }

        final String finalUserId = userId;
        final String finalUserRole = userRole;

        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (USER_HEADER.equalsIgnoreCase(name)) {
                    return finalUserId;
                }
                if (ROLE_HEADER.equalsIgnoreCase(name)) {
                    return finalUserRole;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (USER_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(finalUserId == null ? List.of() : List.of(finalUserId));
                }
                if (ROLE_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(finalUserRole == null ? List.of() : List.of(finalUserRole));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                names.removeIf(h -> USER_HEADER.equalsIgnoreCase(h) || ROLE_HEADER.equalsIgnoreCase(h));
                if (finalUserId != null) {
                    names.add(USER_HEADER);
                }
                if (finalUserRole != null) {
                    names.add(ROLE_HEADER);
                }
                return Collections.enumeration(names);
            }
        }, response);
    }
}

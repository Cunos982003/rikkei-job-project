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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class IdentityHeaderFilter extends OncePerRequestFilter {
    private static final String USER_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return USER_HEADER.equalsIgnoreCase(name) ? userId : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (USER_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(userId == null ? List.of() : List.of(userId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                names.removeIf(USER_HEADER::equalsIgnoreCase);
                if (userId != null) {
                    names.add(USER_HEADER);
                }
                return Collections.enumeration(names);
            }
        }, response);
    }
}

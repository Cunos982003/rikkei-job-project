package com.example.authservice.security;

import com.example.authservice.dto.ErrorResponse;
import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
                    if (shouldSetJwtAuthentication(existingAuthentication)) {
                        Authentication authentication = jwtTokenProvider.getAuthentication(token);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        var principal = authentication.getPrincipal();
                        if (principal instanceof String userId && !userId.isBlank()) {
                            request.setAttribute("currentUser", userId);
                            try {
                                java.util.UUID parsedUserId = java.util.UUID.fromString(userId);
                                userRepository.findById(parsedUserId).ifPresent(user -> {
                                    if (user.isMustChangePassword()) {
                                        request.setAttribute("mustChangePassword", true);
                                    }
                                });
                            } catch (IllegalArgumentException ex) {
                                SecurityContextHolder.clearContext();
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    "Authentication required: Full authentication is required to access this resource"
                                ));
                                return;
                            }
                        }
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (BadCredentialsException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authentication required: Full authentication is required to access this resource"
                ));
                return;
            }
        }

        Boolean mustChangePassword = (Boolean) request.getAttribute("mustChangePassword");
        if (Boolean.TRUE.equals(mustChangePassword) && !isPasswordChangeEndpoint(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "MUST_CHANGE_PASSWORD",
                "Password change required before access is granted"
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSetJwtAuthentication(Authentication existingAuthentication) {
        return existingAuthentication == null
            || existingAuthentication.getName() == null
            || existingAuthentication.getName().isBlank()
            || !existingAuthentication.isAuthenticated();
    }

    private boolean isPasswordChangeEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;

        if (path == null || path.isBlank()) {
            return false;
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return ("/api/v1/auth/change-password".equals(path) || "/auth/change-password".equals(path))
            && "POST".equalsIgnoreCase(request.getMethod());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}


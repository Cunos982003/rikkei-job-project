package com.example.authservice.security;

import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null
            && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof String userId && !userId.isBlank()) {
                request.setAttribute("currentUser", userId);
                var userOpt = userRepository.findById(java.util.UUID.fromString(userId));
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (!isPublicAuthPath(request.getRequestURI())) {
                        Instant now = Instant.now();
                        if (user.getStatus() == UserStatus.LOCKED
                                && (user.getLockedUntil() == null || user.getLockedUntil().isAfter(now))) {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"success\":false,\"statusCode\":403,\"message\":\"Tài khoản đã bị khoá\"}");
                            return;
                        }
                    }
                    if (user.isMustChangePassword()) {
                        request.setAttribute("mustChangePassword", true);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicAuthPath(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.contains("/login")
            || uri.contains("/sso")
            || uri.contains("/forgot-password")
            || uri.contains("/reset-password");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

package com.example.jobservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                UserPrincipal principal = tokenProvider.parseToken(token);

                String requestUri = request.getRequestURI();
                if (principal.isMustChangePassword() && (requestUri == null || !requestUri.startsWith("/api/v1/auth/"))) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/problem+json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                            "{\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Bạn phải đổi mật khẩu trước khi sử dụng API này\"}");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        token,
                        principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ex) {
                // Token không hợp lệ: không set authentication, để Spring Security từ chối
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
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

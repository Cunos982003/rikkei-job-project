package com.example.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter OncePerRequestFilter đưa userId, role, path, status, durationMs vào MDC:
 * - Trích xuất từ Gateway headers (X-User-Id, X-User-Role, X-Correlation-Id).
 * - Fallback trích xuất từ HttpServletRequest.getUserPrincipal() hoặc JWT Bearer token đã xác thực.
 * - Luôn xóa sạch MDC trong khối finally để ngăn chặn triệt để rò rỉ dữ liệu giữa các request trên cùng thread pool.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceContextFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String traceId = resolveTraceId(request);
        String spanId = generateSpanId();
        String userId = resolveUserId(request);
        String role = resolveUserRole(request);
        String path = request.getRequestURI();

        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("service", serviceName != null ? serviceName : "unknown-service");
        MDC.put("path", path);

        if (userId != null && !userId.isBlank()) {
            MDC.put("userId", userId.trim());
        }
        if (role != null && !role.isBlank()) {
            MDC.put("role", role.trim());
        }

        response.setHeader(CORRELATION_ID_HEADER, traceId);
        response.setHeader(TRACEPARENT_HEADER, "00-" + traceId.replace("-", "") + "-" + spanId + "-01");

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long durationMs = System.currentTimeMillis() - startTime;
                MDC.put("status", String.valueOf(response.getStatus()));
                MDC.put("durationMs", String.valueOf(durationMs));

                if (MDC.get("userId") == null) {
                    Principal principal = request.getUserPrincipal();
                    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
                        MDC.put("userId", principal.getName().trim());
                    }
                }

                if (path != null && !path.startsWith("/actuator") && !path.equals("/error")) {
                    log.info("Handled {} {} with status {} in {} ms", request.getMethod(), path, response.getStatus(), durationMs);
                }
            } finally {
                MDC.clear();
            }
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        String altCorrelationId = request.getHeader("x-correlation-id");
        if (altCorrelationId != null && !altCorrelationId.isBlank()) {
            return altCorrelationId.trim();
        }
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        if (traceparent != null && traceparent.startsWith("00-") && traceparent.length() >= 35) {
            return traceparent.substring(3, 35);
        }
        return UUID.randomUUID().toString();
    }

    private String resolveUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName().trim();
        }
        String authHeader = request.getHeader("Authorization");
        String sub = extractJwtClaim(authHeader, "sub");
        if (sub != null && !sub.isBlank()) {
            return sub.trim();
        }
        String jwtUserId = extractJwtClaim(authHeader, "userId");
        if (jwtUserId != null && !jwtUserId.isBlank()) {
            return jwtUserId.trim();
        }
        return null;
    }

    private String resolveUserRole(HttpServletRequest request) {
        String role = request.getHeader(USER_ROLE_HEADER);
        if (role != null && !role.isBlank()) {
            return role.trim();
        }
        String authHeader = request.getHeader("Authorization");
        String jwtRole = extractJwtClaim(authHeader, "role");
        if (jwtRole != null && !jwtRole.isBlank()) {
            return jwtRole.trim();
        }
        return null;
    }

    private String extractJwtClaim(String authHeader, String claimName) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                String payload = new String(decoded, StandardCharsets.UTF_8);
                String search = "\"" + claimName + "\":\"";
                int idx = payload.indexOf(search);
                if (idx != -1) {
                    int start = idx + search.length();
                    int end = payload.indexOf("\"", start);
                    if (end != -1) {
                        return payload.substring(start, end);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

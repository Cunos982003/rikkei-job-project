package com.example.apigateway.security;

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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gateway Ingress Filter for OBS-PLAT-03 Distributed Tracing:
 * 1. Extracts or generates X-Correlation-Id and W3C traceparent.
 * 2. Sets X-Correlation-Id into HTTP response headers.
 * 3. Populates SLF4J MDC with traceId, spanId, service="api-gateway", path.
 * 4. Forwards X-Correlation-Id and traceparent to all downstream microservices.
 * 5. Logs request access metrics with status and durationMs upon completion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TraceCorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Resolve or generate Correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader("x-correlation-id");
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        // 2. Resolve or generate W3C traceparent (00-{traceId32Hex}-{spanId16Hex}-01)
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        if (traceparent == null || traceparent.isBlank()) {
            String cleanTraceId = correlationId.replace("-", "");
            if (cleanTraceId.length() < 32) {
                cleanTraceId = String.format("%-32s", cleanTraceId).replace(' ', '0');
            } else if (cleanTraceId.length() > 32) {
                cleanTraceId = cleanTraceId.substring(0, 32);
            }
            traceparent = "00-" + cleanTraceId + "-" + spanId + "-01";
        }

        // 3. Set Response Header for client traceability
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // 4. Populate MDC for structured logging
        long startTime = System.currentTimeMillis();
        MDC.put("traceId", correlationId);
        MDC.put("spanId", spanId);
        MDC.put("service", "api-gateway");
        MDC.put("path", request.getRequestURI());

        final String finalCorrelationId = correlationId;
        final String finalTraceparent = traceparent;

        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                    return finalCorrelationId;
                }
                if (TRACEPARENT_HEADER.equalsIgnoreCase(name)) {
                    return finalTraceparent;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(finalCorrelationId));
                }
                if (TRACEPARENT_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(finalTraceparent));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                names.removeIf(h -> CORRELATION_ID_HEADER.equalsIgnoreCase(h) || TRACEPARENT_HEADER.equalsIgnoreCase(h));
                names.add(CORRELATION_ID_HEADER);
                names.add(TRACEPARENT_HEADER);
                return Collections.enumeration(names);
            }
        };

        try {
            chain.doFilter(wrappedRequest, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(durationMs));

            // Structured completion log
            log.info("Gateway Ingress {} {} -> status={} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);

            MDC.clear();
        }
    }
}

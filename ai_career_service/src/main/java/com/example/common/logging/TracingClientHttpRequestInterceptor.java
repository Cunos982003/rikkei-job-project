package com.example.common.logging;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        request.getHeaders().set(CORRELATION_ID_HEADER, traceId);
        String cleanTraceId = traceId.replace("-", "");
        if (cleanTraceId.length() < 32) {
            cleanTraceId = String.format("%-32s", cleanTraceId).replace(' ', '0');
        } else if (cleanTraceId.length() > 32) {
            cleanTraceId = cleanTraceId.substring(0, 32);
        }
        request.getHeaders().set(TRACEPARENT_HEADER, "00-" + cleanTraceId + "-" + spanId + "-01");

        String userId = MDC.get("userId");
        if (userId != null && !userId.isBlank() && !request.getHeaders().containsHeader(USER_ID_HEADER)) {
            request.getHeaders().set(USER_ID_HEADER, userId);
        }

        String role = MDC.get("role");
        if (role != null && !role.isBlank() && !request.getHeaders().containsHeader(USER_ROLE_HEADER)) {
            request.getHeaders().set(USER_ROLE_HEADER, role);
        }

        return execution.execute(request, body);
    }
}

package com.example.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * COMMON-04 Acceptance Criteria Verification:
 * 1. RestClient Interceptor forwards X-Correlation-Id and W3C traceparent (00-{traceId32}-{spanId16}-01).
 * 2. Downstream service TraceContextFilter extracts traceparent and maintains traceId across services.
 * 3. User identity (userId, role) is forwarded across internal RestClient calls.
 * 4. traceId and spanId automatically populate MDC for structured JSON logs.
 */
class DistributedTracingTest {

    private TracingClientHttpRequestInterceptor interceptor;
    private TraceContextFilter traceContextFilter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        interceptor = new TracingClientHttpRequestInterceptor();
        traceContextFilter = new TraceContextFilter();
        traceContextFilter.setServiceName("recruitment-service");
    }

    @Test
    @DisplayName("COMMON-04: RestClient Interceptor generates/forwards W3C traceparent and X-Correlation-Id")
    void testInterceptorInjectsTracingHeaders() throws IOException {
        MDC.put("traceId", "test-corr-id-12345");
        MDC.put("userId", "user-cand-888");
        MDC.put("role", "CANDIDATE");

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://ai-career-service/api/v1/recommendations"));
        byte[] body = new byte[0];

        ClientHttpRequestExecution execution = (req, b) -> new MockClientHttpResponse(new byte[0], org.springframework.http.HttpStatus.OK);

        interceptor.intercept(request, body, execution);

        HttpHeaders headers = request.getHeaders();
        assertEquals("test-corr-id-12345", headers.getFirst("X-Correlation-Id"));
        assertNotNull(headers.getFirst("traceparent"), "Header traceparent must be present");

        String traceparent = headers.getFirst("traceparent");
        assertTrue(traceparent.startsWith("00-"), "traceparent must follow W3C tracecontext format starting with 00-");
        assertEquals(55, traceparent.length(), "W3C traceparent must be exactly 55 characters (00-32hex-16hex-01)");
        assertTrue(traceparent.endsWith("-01"), "traceparent sampled flag must end with -01");

        // Identity forwarding
        assertEquals("user-cand-888", headers.getFirst("X-User-Id"));
        assertEquals("CANDIDATE", headers.getFirst("X-User-Role"));
    }

    @Test
    @DisplayName("COMMON-04: End-to-end trace propagation between upstream and downstream service")
    void testEndToEndTracePropagationAcrossServices() throws ServletException, IOException {
        // Step 1: Upstream service (e.g., recruitment-service) has active trace in MDC
        String originalTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        MDC.put("traceId", originalTraceId);
        MDC.put("userId", "emp-hr-101");
        MDC.put("role", "RECRUITER");

        // Upstream calls downstream via RestClient with TracingClientHttpRequestInterceptor
        MockClientHttpRequest outboundRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://profile-service/api/v1/cvs/123/pdf"));
        interceptor.intercept(outboundRequest, new byte[0], (req, b) -> new MockClientHttpResponse(new byte[0], org.springframework.http.HttpStatus.OK));

        String forwardedCorrelationId = outboundRequest.getHeaders().getFirst("X-Correlation-Id");
        String forwardedTraceparent = outboundRequest.getHeaders().getFirst("traceparent");
        String forwardedUserId = outboundRequest.getHeaders().getFirst("X-User-Id");
        String forwardedRole = outboundRequest.getHeaders().getFirst("X-User-Role");

        // Upstream finishes request
        MDC.clear();

        // Step 2: Downstream service (e.g. profile-service) receives inbound request
        TraceContextFilter downstreamFilter = new TraceContextFilter();
        downstreamFilter.setServiceName("profile-service");

        MockHttpServletRequest inboundRequest = new MockHttpServletRequest("GET", "/api/v1/cvs/123/pdf");
        inboundRequest.addHeader("X-Correlation-Id", forwardedCorrelationId);
        inboundRequest.addHeader("traceparent", forwardedTraceparent);
        inboundRequest.addHeader("X-User-Id", forwardedUserId);
        inboundRequest.addHeader("X-User-Role", forwardedRole);

        MockHttpServletResponse downstreamResponse = new MockHttpServletResponse();
        AtomicBoolean downstreamVerified = new AtomicBoolean(false);

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // Downstream service's MDC must have EXACT same traceId as upstream service!
                assertEquals(originalTraceId, MDC.get("traceId"), "TraceId must propagate identically across services!");
                assertNotNull(MDC.get("spanId"), "Each hop must generate a unique spanId");
                assertEquals("profile-service", MDC.get("service"));
                assertEquals("/api/v1/cvs/123/pdf", MDC.get("path"));
                assertEquals("emp-hr-101", MDC.get("userId"));
                assertEquals("RECRUITER", MDC.get("role"));
                downstreamVerified.set(true);
                ((HttpServletResponse) res).setStatus(200);
            }
        };

        downstreamFilter.doFilter(inboundRequest, downstreamResponse, chain);

        assertTrue(downstreamVerified.get(), "Downstream filterChain must execute and verify trace propagation");
        assertEquals(originalTraceId, downstreamResponse.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("traceId"), "MDC must be cleared after request");
    }
}

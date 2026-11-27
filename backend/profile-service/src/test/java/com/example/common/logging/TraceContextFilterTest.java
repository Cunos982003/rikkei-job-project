package com.example.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit Test xác minh tiêu chí nghiệm thu của COMMON-02:
 * 1. Đưa đầy đủ userId, role, path, status, durationMs, traceId, spanId, service vào MDC.
 * 2. CHỨNG MINH MDC KHÔNG RÒ RỈ GIỮA HAI REQUEST LIÊN TIẾP TRÊN CÙNG THREAD.
 * 3. Đảm bảo MDC được xóa sạch 100% kể cả khi xảy ra Exception bên trong filter chain.
 * 4. Hỗ trợ trích xuất userId/role từ JWT Bearer token khi không có header gateway.
 */
class TraceContextFilterTest {

    private TraceContextFilter filter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        filter = new TraceContextFilter();
        filter.setServiceName("api-gateway");
    }

    @Test
    @DisplayName("AC: Chứng minh MDC không rò rỉ giữa hai request liên tiếp trên cùng một Thread")
    void testMdcIsolationBetweenConsecutiveRequestsOnSameThread() throws ServletException, IOException {
        // --- REQUEST 1 (Ứng viên có danh tính đăng nhập) trên Current Thread ---
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/candidate/jobs");
        request1.addHeader("X-Correlation-Id", "trace-req-001");
        request1.addHeader("X-User-Id", "cand-user-999");
        request1.addHeader("X-User-Role", "CANDIDATE");
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        AtomicBoolean req1Asserted = new AtomicBoolean(false);
        MockFilterChain chain1 = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                // Bên trong filterChain của Request 1: MDC phải chứa đầy đủ thông tin của Request 1
                assertEquals("trace-req-001", MDC.get("traceId"));
                assertNotNull(MDC.get("spanId"));
                assertEquals("api-gateway", MDC.get("service"));
                assertEquals("/api/v1/candidate/jobs", MDC.get("path"));
                assertEquals("cand-user-999", MDC.get("userId"));
                assertEquals("CANDIDATE", MDC.get("role"));
                req1Asserted.set(true);
                ((HttpServletResponse) res).setStatus(200);
            }
        };

        filter.doFilter(request1, response1, chain1);
        assertTrue(req1Asserted.get());

        // Ngay sau khi Request 1 kết thúc: MDC BẮT BUỘC ĐÃ ĐƯỢC XÓA SẠCH TRÊN THREAD NÀY!
        assertNull(MDC.get("traceId"), "MDC traceId phải được dọn sạch sau Request 1");
        assertNull(MDC.get("userId"), "MDC userId phải được dọn sạch sau Request 1");
        assertNull(MDC.get("role"), "MDC role phải được dọn sạch sau Request 1");
        assertNull(MDC.get("path"), "MDC path phải được dọn sạch sau Request 1");
        assertNull(MDC.get("status"), "MDC status phải được dọn sạch sau Request 1");
        assertNull(MDC.get("durationMs"), "MDC durationMs phải được dọn sạch sau Request 1");
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty(),
                "MDC Context Map phải trống hoàn toàn sau khi Request 1 kết thúc");

        // --- REQUEST 2 (Khách ẩn danh - Anonymous) trên CHÍNH THREAD ĐÓ ---
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/actuator/health");
        request2.addHeader("X-Correlation-Id", "trace-req-002");
        // Request 2 hoàn toàn không có header X-User-Id và X-User-Role
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        AtomicBoolean req2Asserted = new AtomicBoolean(false);
        MockFilterChain chain2 = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                // Bên trong filterChain của Request 2:
                // 1. Mang traceId mới của Request 2
                assertEquals("trace-req-002", MDC.get("traceId"));
                assertEquals("/actuator/health", MDC.get("path"));

                // 2. TUYỆT ĐỐI KHÔNG BỊ RÒ RỈ userId hoặc role của Request 1 sang!
                assertNull(MDC.get("userId"), "LỖI RÒ RỈ: userId của Request 1 đã bị rò rỉ sang Request 2!");
                assertNull(MDC.get("role"), "LỖI RÒ RỈ: role của Request 1 đã bị rò rỉ sang Request 2!");
                req2Asserted.set(true);
                ((HttpServletResponse) res).setStatus(200);
            }
        };

        filter.doFilter(request2, response2, chain2);
        assertTrue(req2Asserted.get());

        // Sau khi Request 2 kết thúc, MDC vẫn sạch sẽ
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }

    @Test
    @DisplayName("AC: Chứng minh MDC luôn được dọn dẹp sạch sẽ kể cả khi ném Exception trong Controller/Service")
    void testMdcCleanedUpEvenWhenExceptionOccurs() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments");
        request.addHeader("X-Correlation-Id", "trace-error-500");
        request.addHeader("X-User-Id", "user-error-test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // Kiểm tra MDC được set trước khi crash
                assertEquals("trace-error-500", MDC.get("traceId"));
                assertEquals("user-error-test", MDC.get("userId"));
                throw new RuntimeException("Mô phỏng lỗi nghiêm trọng trong tầng nghiệp vụ!");
            }
        };

        boolean caught = false;
        try {
            filter.doFilter(request, response, failingChain);
        } catch (Exception ex) {
            caught = true;
            assertEquals("Mô phỏng lỗi nghiêm trọng trong tầng nghiệp vụ!", ex.getMessage());
        }
        assertTrue(caught, "Phải bắt được ngoại lệ");

        // KHỐI FINALLY PHẢI ĐẢM BẢO CLEAR MDC DÙ CÓ CRASH
        assertNull(MDC.get("traceId"), "MDC traceId không được tồn tại sau khi crash");
        assertNull(MDC.get("userId"), "MDC userId không được tồn tại sau khi crash");
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty(),
                "MDC Context Map phải được clear hoàn toàn kể cả khi có ngoại lệ");
    }

    @Test
    @DisplayName("AC: Trích xuất userId và role từ JWT Bearer token khi không có header Gateway")
    void testExtractUserFromJwtTokenWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles/me");
        // Giả lập JWT token chứa {"sub":"jwt-user-777","role":"MENTOR"}
        String headerPart = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payloadPart = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"jwt-user-777\",\"role\":\"MENTOR\"}".getBytes(StandardCharsets.UTF_8));
        String fakeJwt = headerPart + "." + payloadPart + ".fakesignature";

        request.addHeader("Authorization", "Bearer " + fakeJwt);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean asserted = new AtomicBoolean(false);
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                assertEquals("jwt-user-777", MDC.get("userId"));
                assertEquals("MENTOR", MDC.get("role"));
                assertEquals("/api/v1/profiles/me", MDC.get("path"));
                assertNotNull(MDC.get("traceId"));
                assertNotNull(MDC.get("spanId"));
                asserted.set(true);
            }
        };

        filter.doFilter(request, response, chain);
        assertTrue(asserted.get());
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}

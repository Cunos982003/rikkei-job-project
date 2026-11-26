package com.example.common.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PiiMaskingConverterTest {

    @Test
    void testMaskPasswordInJson() {
        String log = "{\"username\": \"john_doe\", \"password\": \"mySecretPassword123!\"}";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("mySecretPassword123!"));
        assertTrue(masked.contains("\"password\": \"******\""));
    }

    @Test
    void testMaskPasswordInQueryParams() {
        String log = "POST /api/v1/auth/login?user=admin&password=SuperAdminPassword99&redirect=/dashboard";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("SuperAdminPassword99"));
        assertTrue(masked.contains("password=******"));
    }

    @Test
    void testMaskBearerJwtToken() {
        String log = "Handling request with Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
        assertTrue(masked.contains("Bearer [JWT_MASKED]"));
    }

    @Test
    void testMaskEmail() {
        String log = "User email is candidate.alex@rikkeisoft.com and contact is hr-manager@company.vn";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("candidate.alex@rikkeisoft.com"));
        assertFalse(masked.contains("hr-manager@company.vn"));
        assertTrue(masked.contains("c***@rikkeisoft.com"));
        assertTrue(masked.contains("h***@company.vn"));
    }

    @Test
    void testMaskPhoneNumber() {
        String log = "Candidate phone number is 0987654321 and secondary is +84912345678";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("0987654321"));
        assertFalse(masked.contains("+84912345678"));
        assertTrue(masked.contains("098****321"));
        assertTrue(masked.contains("+8491****678"));
    }

    @Test
    void testMaskCvContent() {
        String log = "Uploading application with {\"cvContent\": \"Nguyễn Văn A, 5 năm kinh nghiệm Java Backend, Spring Boot\"}";
        String masked = PiiMaskingConverter.mask(log);

        assertFalse(masked.contains("5 năm kinh nghiệm Java Backend"));
        assertTrue(masked.contains("\"cvContent\": \"[CV_CONTENT_MASKED]\""));
    }

    public static void main(String[] args) {
        PiiMaskingConverterTest test = new PiiMaskingConverterTest();
        test.testMaskPasswordInJson();
        test.testMaskPasswordInQueryParams();
        test.testMaskBearerJwtToken();
        test.testMaskEmail();
        test.testMaskPhoneNumber();
        test.testMaskCvContent();
        System.out.println("ALL PII MASKING TESTS PASSED SUCCESSFULLY!");
    }
}

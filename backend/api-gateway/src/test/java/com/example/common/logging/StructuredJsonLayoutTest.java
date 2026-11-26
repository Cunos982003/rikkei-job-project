package com.example.common.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredJsonLayoutTest {

    @Test
    void testStructuredJsonOutputFormat() {
        StructuredJsonLayout layout = new StructuredJsonLayout();
        layout.setServiceName("test-service");

        LoggingEvent event = new LoggingEvent();
        event.setTimeStamp(System.currentTimeMillis());
        event.setLevel(Level.INFO);
        event.setLoggerName("com.example.TestLogger");
        event.setMessage("User logged in with password=\"secret123\" and email=alex@example.com");

        Map<String, String> mdc = new HashMap<>();
        mdc.put("traceId", "test-trace-12345");
        mdc.put("spanId", "test-span-67890");
        mdc.put("userId", "user-001");
        mdc.put("role", "CANDIDATE");
        mdc.put("path", "/api/v1/login");
        mdc.put("status", "200");
        mdc.put("durationMs", "42");
        event.setMDCPropertyMap(mdc);

        String jsonLine = layout.doLayout(event);
        System.out.println("JSON_OUTPUT: " + jsonLine);
        assertNotNull(jsonLine);
        assertTrue(jsonLine.endsWith("\n"));

        // Verify JSON fields
        assertTrue(jsonLine.contains("\"level\":\"INFO\""));
        assertTrue(jsonLine.contains("\"service\":\"test-service\""));
        assertTrue(jsonLine.contains("\"traceId\":\"test-trace-12345\""));
        assertTrue(jsonLine.contains("\"spanId\":\"test-span-67890\""));
        assertTrue(jsonLine.contains("\"userId\":\"user-001\""));
        assertTrue(jsonLine.contains("\"role\":\"CANDIDATE\""));
        assertTrue(jsonLine.contains("\"path\":\"/api/v1/login\""));
        assertTrue(jsonLine.contains("\"status\":\"200\""));
        assertTrue(jsonLine.contains("\"durationMs\":\"42\""));

        // Verify message is masked
        assertFalse(jsonLine.contains("secret123"));
        assertTrue(jsonLine.contains("******"));
        assertFalse(jsonLine.contains("alex@example.com"));
        assertTrue(jsonLine.contains("a***@example.com"));
    }

    public static void main(String[] args) {
        StructuredJsonLayoutTest test = new StructuredJsonLayoutTest();
        test.testStructuredJsonOutputFormat();
        System.out.println("ALL STRUCTURED JSON LAYOUT TESTS PASSED SUCCESSFULLY!");
    }
}

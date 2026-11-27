package com.example.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Logback layout that serializes log events into strict Single-Line Structured JSON.
 * Produces fields: timestamp, level, service, traceId, spanId, userId, role, path, status, durationMs, message, stackTrace.
 * Pure Java — Zero external dependencies.
 */
public class StructuredJsonLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private String serviceName = "unknown-service";

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');

        // 1. Timestamp (ISO-8601 UTC)
        appendField(sb, "timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())), true);

        // 2. Log Level
        appendField(sb, "level", event.getLevel().toString(), true);

        // 3. Service Name
        appendField(sb, "service", serviceName, true);

        // 4. MDC Attributes
        Map<String, String> mdc = event.getMDCPropertyMap();
        appendField(sb, "traceId", mdc != null ? mdc.get("traceId") : null, true);
        appendField(sb, "spanId", mdc != null ? mdc.get("spanId") : null, true);
        appendField(sb, "userId", mdc != null ? mdc.get("userId") : null, true);
        appendField(sb, "role", mdc != null ? mdc.get("role") : null, true);
        appendField(sb, "path", mdc != null ? mdc.get("path") : null, true);
        appendField(sb, "status", mdc != null ? mdc.get("status") : null, true);
        appendField(sb, "durationMs", mdc != null ? mdc.get("durationMs") : null, true);

        // 5. Message (PII-masked)
        String formattedMessage = event.getFormattedMessage();
        String maskedMessage = PiiMaskingConverter.mask(formattedMessage);
        appendField(sb, "message", maskedMessage, true);

        // 6. Stack Trace (if present)
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            StringBuilder st = new StringBuilder();
            st.append(tp.getClassName()).append(": ").append(tp.getMessage()).append("\n");
            for (StackTraceElementProxy step : tp.getStackTraceElementProxyArray()) {
                st.append("\tat ").append(step.toString()).append("\n");
            }
            appendField(sb, "stackTrace", st.toString(), false);
        } else {
            appendField(sb, "stackTrace", null, false);
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String val, boolean hasNext) {
        sb.append('"').append(key).append("\":");
        if (val == null) {
            sb.append("null");
        } else {
            sb.append('"');
            escapeJson(val, sb);
            sb.append('"');
        }
        if (hasNext) {
            sb.append(',');
        }
    }

    private void escapeJson(String input, StringBuilder sb) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }
}

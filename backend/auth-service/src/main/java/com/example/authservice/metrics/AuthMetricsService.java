package com.example.authservice.metrics;

import com.example.authservice.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AuthMetricsService {

    public static final String METRIC_NAME = "auth_login_total";

    private final MeterRegistry meterRegistry;

    public AuthMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(AuthEndpoint endpoint) {
        incrementCounter(endpoint.getValue(), AuthResult.SUCCESS.getValue(), AuthReason.NONE.getValue());
    }

    public void recordFailure(AuthEndpoint endpoint, AuthReason reason) {
        incrementCounter(
                endpoint.getValue(),
                AuthResult.FAILURE.getValue(),
                reason != null ? reason.getValue() : AuthReason.INTERNAL_ERROR.getValue()
        );
    }

    public void recordFailure(AuthEndpoint endpoint, ErrorCode errorCode) {
        recordFailure(endpoint, AuthReason.fromErrorCode(errorCode));
    }

    public double getCount(AuthEndpoint endpoint, AuthResult result, AuthReason reason) {
        Counter counter = meterRegistry.find(METRIC_NAME)
                .tag("endpoint", endpoint.getValue())
                .tag("result", result.getValue())
                .tag("reason", reason.getValue())
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    private void incrementCounter(String endpoint, String result, String reason) {
        Counter.builder(METRIC_NAME)
                .description("Total authentication and login attempts")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}

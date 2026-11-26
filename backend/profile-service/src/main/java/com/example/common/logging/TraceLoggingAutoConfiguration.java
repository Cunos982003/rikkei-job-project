package com.example.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TraceLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceContextFilter traceContextFilter() {
        return new TraceContextFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public TracingClientHttpRequestInterceptor tracingClientHttpRequestInterceptor() {
        return new TracingClientHttpRequestInterceptor();
    }
}

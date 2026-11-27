package com.example.jobservice.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthEnvelope<T>(
        Boolean success,
        Integer statusCode,
        String message,
        T data,
        Object timestamp
) {}


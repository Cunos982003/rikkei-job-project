package com.example.jobservice.common.exception;

import org.springframework.http.HttpStatus;

public class BadGatewayException extends BusinessException {

    public BadGatewayException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}


package com.example.apigateway.security;

import java.security.Principal;

public record GatewayPrincipal(String userId, String role) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}


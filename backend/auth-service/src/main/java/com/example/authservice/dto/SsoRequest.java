package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record SsoRequest(
    @NotBlank(message = "Ticket is required and cannot be blank")
    String ticket
) {
}


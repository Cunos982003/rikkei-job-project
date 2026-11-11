package com.example.jobservice.user.dto;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID id,
        String email,
        String fullName,
        String role,
        UUID companyId,
        boolean mustChangePassword,
        String message
) {
}


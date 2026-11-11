package com.example.jobservice.user.dto;

public record LoginResponse(
        String token,
        Long id,
        String email,
        String fullName,
        String role,
        Long companyId,
        boolean mustChangePassword,
        String message
) {
}


package com.example.authservice.dto;

import java.time.Instant;
import java.util.UUID;

public record HrAccountAdminResponse(
    UUID id,
    String email,
    String fullName,
    UUID companyId,
    String status,
    String lockedReason,
    Instant lockedAt,
    boolean mustChangePassword,
    Instant createdAt,
    String temporaryPassword,
    boolean emailSent
) {}


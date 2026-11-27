package com.example.jobservice.auth.client;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthHrAccount(
        UUID id,
        String email,
        String fullName,
        UUID companyId,
        String status,
        String lockedReason,
        OffsetDateTime lockedAt,
        boolean mustChangePassword,
        OffsetDateTime createdAt,
        String temporaryPassword,
        boolean emailSent
) {}


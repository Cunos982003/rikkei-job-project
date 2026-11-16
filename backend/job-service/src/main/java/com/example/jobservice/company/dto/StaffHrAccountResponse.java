package com.example.jobservice.company.dto;

import com.example.jobservice.auth.client.AuthHrAccount;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffHrAccountResponse(
        UUID id,
        String fullName,
        String email,
        String status,
        String lockedReason,
        OffsetDateTime lockedAt,
        OffsetDateTime createdAt) {
    public static StaffHrAccountResponse fromAuthHrAccount(AuthHrAccount account) {
        return new StaffHrAccountResponse(
                account.id(),
                account.fullName(),
                account.email(),
                account.status() != null ? account.status() : "ACTIVE",
                account.lockedReason(),
                account.lockedAt(),
                account.createdAt());
    }
}
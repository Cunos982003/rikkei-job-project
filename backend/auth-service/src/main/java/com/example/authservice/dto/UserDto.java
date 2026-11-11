package com.example.authservice.dto;

import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import java.util.UUID;

public record UserDto(
    UUID id,
    String lmsUserId,
    String email,
    String fullName,
    String role,
    UUID companyId,
    boolean mustChangePassword,
    UserStatus status
) {
    public UserDto(UUID id, String lmsUserId, String email, String fullName, String role) {
        this(id, lmsUserId, email, fullName, role, null, false, UserStatus.ACTIVE);
    }

    public static UserDto from(User user) {
        String normalizedRole = user.getRole() == null ? null : user.getRole().trim();
        if (normalizedRole != null && !normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        return new UserDto(
            user.getId(),
            user.getLmsUserId(),
            user.getEmail(),
            user.getFullName(),
            normalizedRole,
            user.getCompanyId(),
            user.isMustChangePassword(),
            user.getStatus()
        );
    }
}

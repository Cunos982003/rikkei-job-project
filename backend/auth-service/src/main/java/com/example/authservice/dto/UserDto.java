package com.example.authservice.dto;

import com.example.authservice.entity.User;
import java.util.UUID;

public record UserDto(
    UUID id,
    String lmsUserId,
    String email,
    String fullName,
    String role
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId(),
            user.getLmsUserId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}


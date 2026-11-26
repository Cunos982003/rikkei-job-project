package com.example.notificationservice.dto;

import com.example.notificationservice.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateNotificationRequest(
        @NotBlank(message = "User ID không được để trống")
        String userId,

        String recipientRole,

        @NotNull(message = "Loại thông báo không được để trống")
        NotificationType type,

        @NotBlank(message = "Tiêu đề không được để trống")
        String title,

        @NotBlank(message = "Nội dung không được để trống")
        String body,

        String actionUrl,

        String metadata
) {}

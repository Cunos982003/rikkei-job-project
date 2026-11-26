package com.example.notificationservice.dto;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.enums.NotificationType;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record NotificationDto(
        UUID id,
        String userId,
        String recipientRole,
        NotificationType type,
        String title,
        String body,
        String actionUrl,
        String metadata,
        boolean isRead,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .recipientRole(notification.getRecipientRole())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .actionUrl(notification.getActionUrl())
                .metadata(notification.getMetadata())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

package com.example.notificationservice.entity;

import com.example.notificationservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "recipient_role", length = 50)
    private String recipientRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = OffsetDateTime.now();
        }
    }
}

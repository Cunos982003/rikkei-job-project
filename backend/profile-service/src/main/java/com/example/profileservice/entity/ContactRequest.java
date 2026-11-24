package com.example.profileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactRequest {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "job_id", columnDefinition = "uuid")
    private UUID jobId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactRequestStatus status;

    @Column(name = "reviewed_by", columnDefinition = "uuid")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum ContactRequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
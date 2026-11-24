package com.example.profileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_access_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactAccessLog {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    @Column(name = "hr_id", nullable = false, columnDefinition = "uuid")
    private UUID hrId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "via", nullable = false, length = 30)
    private ViaType via;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum ViaType {
        APPLICATION, APPROVED_REQUEST
    }
}
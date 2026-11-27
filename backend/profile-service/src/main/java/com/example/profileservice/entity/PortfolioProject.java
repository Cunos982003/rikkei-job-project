package com.example.profileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolio_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioProject {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source", length = 20, nullable = false)
    private String source;

    @Column(name = "source_ref", length = 100, nullable = false)
    private String sourceRef;

    @Column(name = "name", length = 300, nullable = false)
    private String name;

    @Column(name = "role", length = 300)
    private String role;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "demo_url", length = 500)
    private String demoUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "internship_id")
    private UUID internshipId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
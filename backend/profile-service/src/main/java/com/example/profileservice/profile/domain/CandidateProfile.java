package com.example.profileservice.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfile {

    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 100)
    private String program;

    @Column(name = "class_code", length = 50)
    private String classCode;

    @Column(name = "course_name", length = 200)
    private String courseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onPersist() {
        if (visibility == null) {
            visibility = Visibility.PRIVATE;
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    public enum Visibility {
        PRIVATE, PUBLIC, RECRUITER
    }
}
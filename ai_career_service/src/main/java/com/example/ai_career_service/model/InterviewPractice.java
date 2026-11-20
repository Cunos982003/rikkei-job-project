package com.example.ai_career_service.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_practices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "job_title", length = 500)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PracticeStatus status = PracticeStatus.IN_PROGRESS;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "practice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("qOrder ASC")
    private List<PracticeQuestion> questions = new ArrayList<>();
}

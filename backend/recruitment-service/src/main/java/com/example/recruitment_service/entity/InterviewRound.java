package com.example.recruitment_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.recruitment_service.enums.InterviewRoundResult;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "interview_rounds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interview_rounds_recruitment_round_no",
                columnNames = {"recruitment_id", "round_no"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InterviewRound {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    @ToString.Exclude
    private Recruitment recruitment;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "interviewer_email")
    private String interviewerEmail;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private InterviewRoundResult result = InterviewRoundResult.PENDING;

    @Column(name = "score", columnDefinition = "integer check (score IS NULL OR (score >= 0 AND score <= 100))")
    private Integer score;

    @Column(name = "note")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

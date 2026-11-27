package com.example.learningservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skill_course_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCourseMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    @Column(name = "lms_course_id", nullable = false, length = 64)
    private String lmsCourseId;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "lesson_hint", columnDefinition = "TEXT")
    private String lessonHint;

    @Column(name = "level_from", nullable = false)
    @Builder.Default
    private Integer levelFrom = 0;

    @Column(name = "level_to", nullable = false)
    @Builder.Default
    private Integer levelTo = 100;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}


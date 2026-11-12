package com.example.recruitment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter 
@ToString 
@Table(name = "recruitments")

public class Recruitment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID job_id;

    @Column(name = "user_id", nullable = false)
    private UUID user_id;

    @Column(name = "cv_id") 
    private UUID cv_id;

    @Column(name = "cv_snapshot_url")
    private String cv_snapshot_url;

    @Column(name = "cover_letter")
    private String cover_letter;

    @Column(name = "status", nullable = false)
    private RecruitmentStatus status;

    @Column(name = "created_at")
    @CreationTimestamp 
    private java.time.LocalDateTime created_at;

    @Column(name = "updated_at")
    @UpdateTimestamp 
    private java.time.LocalDateTime updated_at;
}

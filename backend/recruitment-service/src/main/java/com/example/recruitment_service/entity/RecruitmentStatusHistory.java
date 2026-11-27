package com.example.recruitment_service.entity;

import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity 
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
@ToString 
@Table(name = "recruitment_status_history")
public class RecruitmentStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recruitment_id", nullable = false)
    private UUID recruitment_id;

    @Column(name = "from_status", nullable = false)
    private RecruitmentStatus from_status;

    @Column(name = "to_status", nullable = false)
    private RecruitmentStatus to_status;

    @Column(name = "actor_id", nullable = false)
    private UUID actor_id;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private java.time.LocalDateTime created_at;
}

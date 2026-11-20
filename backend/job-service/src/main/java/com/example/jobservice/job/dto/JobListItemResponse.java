package com.example.jobservice.job.dto;

import com.example.jobservice.job.domain.JobLevel;
import com.example.jobservice.job.domain.JobStatus;
import com.example.jobservice.job.domain.JobType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobListItemResponse(
        UUID id,
        String title,
        String position,
        JobLevel level,
        JobType type,
        Integer quantity,
        String city,
        LocalDate deadline,
        JobStatus status,
        String rejectedReason,
        int requiredSkillCount,
        OffsetDateTime updatedAt
) {}


package com.example.jobservice.job.dto;

import com.example.jobservice.job.domain.JobLevel;
import com.example.jobservice.job.domain.JobStatus;
import com.example.jobservice.job.domain.JobType;
import com.example.jobservice.job.domain.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String title,
        String position,
        JobLevel level,
        JobType type,
        Integer quantity,
        String city,
        String address,
        WorkMode workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String description,
        String requirements,
        String benefits,
        LocalDate deadline,
        Instant expiresAt,
        JobStatus status,
        String rejectedReason,
        List<JobSkillResponse> skills,
        List<JobSkillResponse> jobSkills,
        List<String> requiredSkills,
        List<String> hrEmails,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String experienceLevel,
        String jobType,
        String location
) {}


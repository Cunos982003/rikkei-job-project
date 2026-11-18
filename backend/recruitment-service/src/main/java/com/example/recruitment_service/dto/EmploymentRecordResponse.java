package com.example.recruitment_service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.recruitment_service.entity.EmploymentRecord;

public record EmploymentRecordResponse(
        UUID id,
        UUID userId,
        UUID companyId,
        String companyName,
        UUID jobId,
        UUID applicationId,
        String position,
        LocalDate startDate,
        Instant confirmedAt,
        Instant createdAt
) {
    public static EmploymentRecordResponse from(EmploymentRecord record) {
        return new EmploymentRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getCompanyId(),
                record.getCompanyName(),
                record.getJobId(),
                record.getApplicationId(),
                record.getPosition(),
                record.getStartDate(),
                record.getConfirmedAt(),
                record.getCreatedAt()
        );
    }
}

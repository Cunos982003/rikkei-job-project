package com.example.jobservice.company.dto;

import com.example.jobservice.company.domain.Company;

import java.time.OffsetDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String taxCode,
        String industry,
        String size,
        String address,
        String city,
        String website,
        String logoUrl,
        String description,
        String status,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CompanyResponse fromEntity(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getTaxCode(),
                company.getIndustry(),
                company.getSize(),
                company.getAddress(),
                company.getCity(),
                company.getWebsite(),
                company.getLogoUrl(),
                company.getDescription(),
                company.getStatus() != null ? company.getStatus().name() : "ACTIVE",
                company.getCreatedBy(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}


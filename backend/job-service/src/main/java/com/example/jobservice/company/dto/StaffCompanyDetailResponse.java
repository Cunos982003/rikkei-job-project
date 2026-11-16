package com.example.jobservice.company.dto;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StaffCompanyDetailResponse(
        UUID id,
        String name,
        String taxCode,
        String industry,
        String size,
        String address,
        String city,
        String website,
        String logoUrl,
        String description,
        CompanyStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<StaffHrAccountResponse> hrAccounts
) {
    public List<StaffHrAccountResponse> getHrs() {
        return hrAccounts;
    }

    public static StaffCompanyDetailResponse fromEntity(Company company, List<StaffHrAccountResponse> hrAccounts) {
        return new StaffCompanyDetailResponse(
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
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt(),
                hrAccounts
        );
    }
}


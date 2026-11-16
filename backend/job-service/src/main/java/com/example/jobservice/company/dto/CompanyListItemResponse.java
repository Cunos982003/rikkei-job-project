package com.example.jobservice.company.dto;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyStatus;

import java.util.UUID;

public record CompanyListItemResponse(
        UUID id,
        String name,
        String taxCode,
        String industry,
        String size,
        String address,
        String city,
        String logoUrl,
        CompanyStatus status,
        long totalHr,
        long totalJd
) {
    public static CompanyListItemResponse fromEntity(Company company, long totalHr, long totalJd) {
        return new CompanyListItemResponse(
                company.getId(),
                company.getName(),
                company.getTaxCode(),
                company.getIndustry(),
                company.getSize(),
                company.getAddress(),
                company.getCity(),
                company.getLogoUrl(),
                company.getStatus(),
                totalHr,
                totalJd
        );
    }
}


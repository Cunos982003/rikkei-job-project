package com.example.profileservice.profile.api.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {
    private UUID id;
    private UUID userId;
    private String school;
    private String major;
    private LocalDate fromDate;
    private LocalDate toDate;
    private java.math.BigDecimal gpa;
    private String note;
}
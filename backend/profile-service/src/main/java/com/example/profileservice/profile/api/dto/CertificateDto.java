package com.example.profileservice.profile.api.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String issuer;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private String fileUrl;
    private String fileType;
}
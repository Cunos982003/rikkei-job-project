package com.example.profileservice.profile.service;

import com.example.profileservice.profile.api.dto.CertificateDto;
import com.example.profileservice.profile.domain.Certificate;
import com.example.profileservice.profile.infrastructure.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepo;

    @Transactional
    public CertificateDto createCertificate(UUID userId, CertificateDto dto) {
        Certificate certificate = Certificate.builder()
                .userId(userId)
                .name(dto.getName())
                .issuer(dto.getIssuer())
                .issuedAt(dto.getIssuedAt())
                .expiresAt(dto.getExpiresAt())
                .fileUrl(dto.getFileUrl())
                .fileType(dto.getFileType())
                .build();

        Certificate saved = certificateRepo.save(certificate);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> getCertificates(UUID userId) {
        return certificateRepo.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CertificateDto getCertificate(UUID userId, UUID certificateId) {
        Certificate certificate = certificateRepo.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (!certificate.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return toDto(certificate);
    }

    @Transactional
    public CertificateDto updateCertificate(UUID userId, UUID certificateId, CertificateDto dto) {
        Certificate certificate = certificateRepo.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (!certificate.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        certificate.setName(dto.getName());
        certificate.setIssuer(dto.getIssuer());
        certificate.setIssuedAt(dto.getIssuedAt());
        certificate.setExpiresAt(dto.getExpiresAt());
        certificate.setFileUrl(dto.getFileUrl());
        certificate.setFileType(dto.getFileType());

        Certificate saved = certificateRepo.save(certificate);
        return toDto(saved);
    }

    @Transactional
    public void deleteCertificate(UUID userId, UUID certificateId) {
        Certificate certificate = certificateRepo.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (!certificate.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        certificateRepo.delete(certificate);
    }

    private CertificateDto toDto(Certificate c) {
        return CertificateDto.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .name(c.getName())
                .issuer(c.getIssuer())
                .issuedAt(c.getIssuedAt())
                .expiresAt(c.getExpiresAt())
                .fileUrl(c.getFileUrl())
                .fileType(c.getFileType())
                .build();
    }
}
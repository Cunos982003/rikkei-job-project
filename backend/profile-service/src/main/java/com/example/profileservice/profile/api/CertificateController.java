package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.CertificateDto;
import com.example.profileservice.profile.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/me/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<CertificateDto> createCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody CertificateDto dto) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CertificateDto created = certificateService.createCertificate(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CertificateDto>> getCertificates(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<CertificateDto> certificates = certificateService.getCertificates(userId);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<CertificateDto> getCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CertificateDto certificate = certificateService.getCertificate(userId, certificateId);
        return ResponseEntity.ok(certificate);
    }

    @PutMapping("/{certificateId}")
    public ResponseEntity<CertificateDto> updateCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId,
            @RequestBody CertificateDto dto) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CertificateDto updated = certificateService.updateCertificate(userId, certificateId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{certificateId}")
    public ResponseEntity<Void> deleteCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        certificateService.deleteCertificate(userId, certificateId);
        return ResponseEntity.noContent().build();
    }
}
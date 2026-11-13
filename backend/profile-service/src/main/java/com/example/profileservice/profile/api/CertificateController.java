package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.CertificateDto;
import com.example.profileservice.profile.service.CertificateService;
import com.example.profileservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CertificateDto dto) {
        UUID userId = principal.getUserId();
        CertificateDto created = certificateService.createCertificate(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CertificateDto>> getCertificates(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUserId();
        List<CertificateDto> certificates = certificateService.getCertificates(userId);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<CertificateDto> getCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificateId) {
        UUID userId = principal.getUserId();
        CertificateDto certificate = certificateService.getCertificate(userId, certificateId);
        return ResponseEntity.ok(certificate);
    }

    @PutMapping("/{certificateId}")
    public ResponseEntity<CertificateDto> updateCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificateId,
            @RequestBody CertificateDto dto) {
        UUID userId = principal.getUserId();
        CertificateDto updated = certificateService.updateCertificate(userId, certificateId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{certificateId}")
    public ResponseEntity<Void> deleteCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificateId) {
        UUID userId = principal.getUserId();
        certificateService.deleteCertificate(userId, certificateId);
        return ResponseEntity.noContent().build();
    }
}
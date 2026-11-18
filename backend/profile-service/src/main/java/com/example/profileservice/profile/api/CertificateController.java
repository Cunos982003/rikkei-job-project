package com.example.profileservice.profile.api;

import com.example.profileservice.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<CertificateDto>> createCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody CertificateDto dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        CertificateDto created = certificateService.createCertificate(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Certificate created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificateDto>>> getCertificates(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        List<CertificateDto> certificates = certificateService.getCertificates(userId);
        return ResponseEntity.ok(ApiResponse.success("Certificates retrieved successfully", certificates));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateDto>> getCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        CertificateDto certificate = certificateService.getCertificate(userId, certificateId);
        return ResponseEntity.ok(ApiResponse.success("Certificate retrieved successfully", certificate));
    }

    @PutMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateDto>> updateCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId,
            @RequestBody CertificateDto dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        CertificateDto updated = certificateService.updateCertificate(userId, certificateId, dto);
        return ResponseEntity.ok(ApiResponse.success("Certificate updated successfully", updated));
    }

    @DeleteMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        certificateService.deleteCertificate(userId, certificateId);
        return ResponseEntity.ok(ApiResponse.success("Certificate deleted successfully", null));
    }
}
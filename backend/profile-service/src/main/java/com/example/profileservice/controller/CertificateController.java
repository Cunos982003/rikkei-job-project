package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.dto.CertificateDto;
import com.example.profileservice.service.CertificateService;
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
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CertificateDto created = certificateService.createCertificate(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Tạo chứng chỉ thành công", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificateDto>>> getCertificates(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<CertificateDto> certificates = certificateService.getCertificates(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chứng chỉ thành công", certificates));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateDto>> getCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CertificateDto certificate = certificateService.getCertificate(userId, certificateId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chứng chỉ thành công", certificate));
    }

    @PutMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateDto>> updateCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId,
            @RequestBody CertificateDto dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CertificateDto updated = certificateService.updateCertificate(userId, certificateId, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chứng chỉ thành công", updated));
    }

    @DeleteMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID certificateId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        certificateService.deleteCertificate(userId, certificateId);
        return ResponseEntity.ok(ApiResponse.success("Xóa chứng chỉ thành công", null));
    }
}
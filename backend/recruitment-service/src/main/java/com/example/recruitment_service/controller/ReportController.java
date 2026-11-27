package com.example.recruitment_service.controller;

import com.example.recruitment_service.dto.RecruitmentReportResponse;
import com.example.recruitment_service.service.RecruitmentReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
// SỬA DÒNG NÀY: Thêm /api/v1
@RequestMapping("/api/v1/reports/recruitment")
public class ReportController {

    private final RecruitmentReportService reportService;

    public ReportController(RecruitmentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'STAFF')")
    public ResponseEntity<RecruitmentReportResponse> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) String courseName,
            Authentication auth) {

        UUID effectiveCompanyId = companyId;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            effectiveCompanyId = getCurrentUserCompanyId(auth);
        }

        return ResponseEntity.ok(reportService.generateReport(from, to, effectiveCompanyId, jobId, courseName));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('HR', 'STAFF')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) String courseName,
            Authentication auth) throws Exception {

        UUID effectiveCompanyId = companyId;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            effectiveCompanyId = getCurrentUserCompanyId(auth);
        }

        RecruitmentReportResponse report = reportService.generateReport(from, to, effectiveCompanyId, jobId, courseName);
        byte[] excelData = reportService.exportExcel(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recruitment_report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    private UUID getCurrentUserCompanyId(Authentication auth) {
        // TODO: Lấy companyId thực tế của user đang đăng nhập
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock ID công ty A
    }
}
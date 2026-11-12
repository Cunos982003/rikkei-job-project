package com.example.recruitment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.recruitment_service.dto.RecruitmentDetails;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.dto.ApplyRequest;
import com.example.recruitment_service.dto.WithdrawRequest;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.RecruitmentService;

import jakarta.validation.Valid;

@RestController
@Validated
public class RecruitmentController {
    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @PostMapping("/jobs/{id}/apply")
    public ResponseEntity<RecruitmentSummary> apply(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyRequest request,
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.status(201).body(recruitmentService.apply(id, request, candidate, authorization));
    }

    @GetMapping("/me/recruitments")
    public List<RecruitmentSummary> list(
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestParam(required = false) RecruitmentStatus status) {
        return recruitmentService.list(candidate, status);
    }

    @GetMapping("/me/recruitments/{id}")
    public RecruitmentDetails get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal candidate) {
        return recruitmentService.get(id, candidate);
    }

    @PostMapping("/me/recruitments/{id}/withdraw")
    public RecruitmentSummary withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal candidate) {
        return recruitmentService.withdraw(id, request.reason(), candidate);
    }

    @GetMapping("/recruitments/{id}/cv-snapshot")
    public ResponseEntity<byte[]> getCvSnapshot(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        byte[] pdf = recruitmentService.getCvSnapshot(id, principal, authorization);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv-" + id + ".pdf\"")
                .body(pdf);
    }
}

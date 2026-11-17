package com.example.recruitment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.recruitment_service.dto.CreateInterviewRoundRequest;
import com.example.recruitment_service.dto.InterviewRoundSummary;
import com.example.recruitment_service.dto.RecruitmentDetails;
import com.example.recruitment_service.dto.RecruitmentReviewDetails;
import com.example.recruitment_service.dto.RecruitmentStaffSummary;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.dto.ApplyRequest;
import com.example.recruitment_service.dto.TransitionRequest;
import com.example.recruitment_service.dto.UpdateInterviewRoundRequest;
import com.example.recruitment_service.dto.WithdrawRequest;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.RecruitmentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1")
public class RecruitmentController {
    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @PostMapping("/jobs/{id}/recruit")
    public ResponseEntity<RecruitmentSummary> recruit(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyRequest request,
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.status(201).body(recruitmentService.recruit(id, request, candidate, authorization));
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

    @GetMapping("/recruitments/{id}")
    public RecruitmentReviewDetails getForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return recruitmentService.getReviewDetails(id, principal, authorization);
    }

    @PostMapping("/recruitments/{id}/transition")
    public RecruitmentSummary transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return recruitmentService.transition(id, request, actor, authorization);
    }

    @PostMapping("/recruitments/{id}/rounds")
    public InterviewRoundSummary createRound(
            @PathVariable UUID id,
            @Valid @RequestBody CreateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return InterviewRoundSummary.from(recruitmentService.createRound(id, request, actor, authorization));
    }

    @PutMapping("/recruitments/{id}/rounds/{roundId}")
    public InterviewRoundSummary updateRound(
            @PathVariable UUID id,
            @PathVariable UUID roundId,
            @Valid @RequestBody UpdateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return InterviewRoundSummary.from(
                recruitmentService.updateRound(id, roundId, request, actor, authorization));
    }

    @GetMapping("/staff/recruitments")
    public Page<RecruitmentStaffSummary> listForStaff(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) RecruitmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return recruitmentService.listForStaff(companyId, jobId, status, page);
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

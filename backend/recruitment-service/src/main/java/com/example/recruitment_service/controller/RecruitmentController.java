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

import com.example.recruitment_service.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<RecruitmentSummary>> recruit(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyRequest request,
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        RecruitmentSummary summary = recruitmentService.recruit(id, request, candidate, authorization);
        return ResponseEntity.status(201).body(ApiResponse.created("Nộp đơn ứng tuyển thành công", summary));
    }

    @GetMapping("/me/recruitments")
    public ResponseEntity<ApiResponse<List<RecruitmentSummary>>> list(
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestParam(required = false) RecruitmentStatus status) {
        List<RecruitmentSummary> result = recruitmentService.list(candidate, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn ứng tuyển thành công", result));
    }

    @GetMapping("/me/recruitments/{id}")
    public ResponseEntity<ApiResponse<RecruitmentDetails>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal candidate) {
        RecruitmentDetails details = recruitmentService.get(id, candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn ứng tuyển thành công", details));
    }

    @GetMapping({"/recruitments/{id}", "/applications/{id}"})
    public ResponseEntity<ApiResponse<RecruitmentReviewDetails>> getForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        RecruitmentReviewDetails details = recruitmentService.getReviewDetails(id, principal, authorization);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đánh giá ứng viên thành công", details));
    }

    @PostMapping({"/recruitments/{id}/transition", "/applications/{id}/transition"})
    public ResponseEntity<ApiResponse<RecruitmentSummary>> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        RecruitmentSummary summary = recruitmentService.transition(id, request, actor, authorization);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái tuyển dụng thành công", summary));
    }

    @PostMapping({"/recruitments/{id}/rounds", "/applications/{id}/rounds"})
    public ResponseEntity<ApiResponse<InterviewRoundSummary>> createRound(
            @PathVariable UUID id,
            @Valid @RequestBody CreateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        InterviewRoundSummary round = InterviewRoundSummary.from(
                recruitmentService.createRound(id, request, actor, authorization));
        return ResponseEntity.status(201).body(ApiResponse.created("Tạo vòng phỏng vấn thành công", round));
    }

    @PutMapping({"/recruitments/{id}/rounds/{roundId}", "/applications/{id}/rounds/{roundId}"})
    public ResponseEntity<ApiResponse<InterviewRoundSummary>> updateRound(
            @PathVariable UUID id,
            @PathVariable UUID roundId,
            @Valid @RequestBody UpdateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        InterviewRoundSummary round = InterviewRoundSummary.from(
                recruitmentService.updateRound(id, roundId, request, actor, authorization));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vòng phỏng vấn thành công", round));
    }

    @GetMapping({"/staff/recruitments", "/staff/applications"})
    public ResponseEntity<ApiResponse<Page<RecruitmentStaffSummary>>> listForStaff(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) RecruitmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        Page<RecruitmentStaffSummary> result = recruitmentService.listForStaff(companyId, jobId, status, page);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn ứng tuyển cho cán bộ thành công", result));
    }

    @PostMapping("/me/recruitments/{id}/withdraw")
    public ResponseEntity<ApiResponse<RecruitmentSummary>> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal candidate) {
        RecruitmentSummary summary = recruitmentService.withdraw(id, request.reason(), candidate);
        return ResponseEntity.ok(ApiResponse.success("Rút đơn ứng tuyển thành công", summary));
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

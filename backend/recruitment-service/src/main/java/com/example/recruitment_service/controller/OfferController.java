package com.example.recruitment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
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

import com.example.recruitment_service.dto.ApiResponse;
import com.example.recruitment_service.dto.DeclineOfferRequest;
import com.example.recruitment_service.dto.EmploymentRecordResponse;
import com.example.recruitment_service.dto.OfferResponse;
import com.example.recruitment_service.dto.PageResult;
import com.example.recruitment_service.dto.RecruitmentStaffSummary;
import com.example.recruitment_service.dto.SendOfferRequest;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.OfferService;
import com.example.recruitment_service.service.RecruitmentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1")
public class OfferController {

    private final OfferService offerService;
    private final RecruitmentService recruitmentService;

    public OfferController(OfferService offerService, RecruitmentService recruitmentService) {
        this.offerService = offerService;
        this.recruitmentService = recruitmentService;
    }

    @PostMapping({"/applications/{id}/offer", "/recruitments/{id}/offer"})
    public ResponseEntity<ApiResponse<OfferResponse>> sendOffer(
            @PathVariable UUID id,
            @Valid @RequestBody SendOfferRequest request,
            @AuthenticationPrincipal UserPrincipal hr,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        OfferResponse offer = offerService.sendOffer(id, request, hr, authorization);
        return ResponseEntity.status(201).body(ApiResponse.created("Gửi thư mời nhận việc (Offer) thành công", offer));
    }

    @PostMapping({"/applications/{id}/offer/confirm-onboard", "/recruitments/{id}/offer/confirm-onboard"})
    public ResponseEntity<ApiResponse<EmploymentRecordResponse>> confirmOnboard(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal hr,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        EmploymentRecordResponse record = offerService.confirmOnboard(id, hr, authorization);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận ứng viên đã đi làm thành công", record));
    }

    @GetMapping({"/hr/applications", "/applications"})
    public ResponseEntity<ApiResponse<PageResult<RecruitmentStaffSummary>>> listForHr(
            @AuthenticationPrincipal UserPrincipal hr,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) RecruitmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        Page<RecruitmentStaffSummary> result = recruitmentService.listForHr(hr, jobId, status, page);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn ứng tuyển của doanh nghiệp thành công", PageResult.from(result)));
    }

    @GetMapping("/me/offers")
    public ResponseEntity<ApiResponse<List<OfferResponse>>> listMyOffers(
            @AuthenticationPrincipal UserPrincipal candidate) {
        List<OfferResponse> offers = offerService.listCandidateOffers(candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách offer của bạn thành công", offers));
    }

    @GetMapping("/me/offers/{id}")
    public ResponseEntity<ApiResponse<OfferResponse>> getMyOffer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal candidate) {
        OfferResponse offer = offerService.getCandidateOffer(id, candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết offer thành công", offer));
    }

    @PostMapping("/me/offers/{id}/accept")
    public ResponseEntity<ApiResponse<OfferResponse>> acceptOffer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal candidate) {
        OfferResponse offer = offerService.acceptOffer(id, candidate);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận thư mời nhận việc thành công", offer));
    }

    @PostMapping("/me/offers/{id}/decline")
    public ResponseEntity<ApiResponse<OfferResponse>> declineOffer(
            @PathVariable UUID id,
            @Valid @RequestBody DeclineOfferRequest request,
            @AuthenticationPrincipal UserPrincipal candidate) {
        OfferResponse offer = offerService.declineOffer(id, request, candidate);
        return ResponseEntity.ok(ApiResponse.success("Từ chối thư mời nhận việc thành công", offer));
    }

    @GetMapping("/me/employment-records")
    public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> listMyEmploymentRecords(
            @AuthenticationPrincipal UserPrincipal candidate) {
        List<EmploymentRecordResponse> records = offerService.listMyEmploymentRecords(candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử nhận việc thành công", records));
    }
}

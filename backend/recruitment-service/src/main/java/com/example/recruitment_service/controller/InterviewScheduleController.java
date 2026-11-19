package com.example.recruitment_service.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.example.recruitment_service.dto.CandidateInterviewResponse;
import com.example.recruitment_service.dto.CompanyInterviewResponse;
import com.example.recruitment_service.dto.InterviewScheduleResponse;
import com.example.recruitment_service.dto.InterviewSlotInput;
import com.example.recruitment_service.dto.InterviewSlotResponse;
import com.example.recruitment_service.dto.PickInterviewSlotRequest;
import com.example.recruitment_service.dto.RescheduleResponse;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.InterviewScheduleService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/v1")
public class InterviewScheduleController {
    private final InterviewScheduleService interviewScheduleService;

    public InterviewScheduleController(InterviewScheduleService interviewScheduleService) {
        this.interviewScheduleService = interviewScheduleService;
    }

    @PostMapping("/rounds/{roundId}/slots")
    public ResponseEntity<ApiResponse<List<InterviewSlotResponse>>> proposeSlots(
            @PathVariable UUID roundId,
            @RequestBody List<InterviewSlotInput> slots,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        List<InterviewSlotResponse> created = interviewScheduleService.proposeSlots(roundId, slots, actor, authorization);
        return ResponseEntity.status(201).body(ApiResponse.created("Đề xuất khung giờ phỏng vấn thành công", created));
    }

    @GetMapping("/rounds/{roundId}/slots")
    public ResponseEntity<ApiResponse<List<InterviewSlotResponse>>> listRoundSlots(
            @PathVariable UUID roundId,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        List<InterviewSlotResponse> slots = interviewScheduleService.listRoundSlots(roundId, actor, authorization);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khung giờ phỏng vấn thành công", slots));
    }

    @PostMapping("/rounds/{roundId}/reschedule")
    public ResponseEntity<ApiResponse<RescheduleResponse>> reschedule(
            @PathVariable UUID roundId,
            @RequestBody List<InterviewSlotInput> slots,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        RescheduleResponse response = interviewScheduleService.reschedule(roundId, slots, actor, authorization);
        return ResponseEntity.ok(ApiResponse.success("Đổi lịch phỏng vấn thành công", response));
    }

    @GetMapping("/me/interviews")
    public ResponseEntity<ApiResponse<List<CandidateInterviewResponse>>> listMyInterviews(
            @AuthenticationPrincipal UserPrincipal candidate) {
        List<CandidateInterviewResponse> interviews = interviewScheduleService.listCandidateInterviews(candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch phỏng vấn thành công", interviews));
    }

    @PostMapping("/me/interviews/{roundId}/pick")
    public ResponseEntity<ApiResponse<InterviewScheduleResponse>> pickSlot(
            @PathVariable UUID roundId,
            @Valid @RequestBody PickInterviewSlotRequest request,
            @AuthenticationPrincipal UserPrincipal candidate,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        InterviewScheduleResponse schedule =
                interviewScheduleService.pickSlot(roundId, request.slotId(), candidate, authorization);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận lịch phỏng vấn thành công", schedule));
    }

    @GetMapping("/companies/me/interviews")
    public ResponseEntity<ApiResponse<List<CompanyInterviewResponse>>> listCompanyInterviews(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        List<CompanyInterviewResponse> schedules = interviewScheduleService.listCompanyInterviews(actor, from, to);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch phỏng vấn doanh nghiệp thành công", schedules));
    }
}

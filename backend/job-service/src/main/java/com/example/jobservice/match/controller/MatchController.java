package com.example.jobservice.match.controller;

import com.example.jobservice.common.ApiResponse;
import com.example.jobservice.match.dto.MatchResultResponse;
import com.example.jobservice.match.service.MatchService;
import com.example.jobservice.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Độ khớp kỹ năng ứng viên với JD", description = "API tính toán điểm khớp ứng viên với JD theo kỹ năng (DN-06)")
@RestController
@RequestMapping("/jobs")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @Operation(summary = "Xem điểm khớp của chính mình với tin tuyển dụng (CANDIDATE)",
            description = "Ứng viên xem điểm khớp kỹ năng của chính mình với một tin tuyển dụng đã phê duyệt")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/{id}/match/me")
    public ResponseEntity<ApiResponse<MatchResultResponse>> matchMe(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        MatchResultResponse response = matchService.computeMatch(id, principal.getId(), principal);
        return ResponseEntity.ok(ApiResponse.success("Lấy điểm khớp kỹ năng thành công", response));
    }

    @Operation(summary = "Xem điểm khớp của ứng viên với tin tuyển dụng (HR/STAFF/ADMIN)",
            description = "Nhà tuyển dụng hoặc cán bộ xem điểm khớp kỹ năng của ứng viên cụ thể với tin tuyển dụng")
    @PreAuthorize("hasAnyRole('HR','STAFF','ADMIN')")
    @GetMapping("/{id}/match/{userId}")
    public ResponseEntity<ApiResponse<MatchResultResponse>> matchCandidate(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        MatchResultResponse response = matchService.computeMatch(id, userId, principal);
        return ResponseEntity.ok(ApiResponse.success("Lấy điểm khớp kỹ năng ứng viên thành công", response));
    }
}

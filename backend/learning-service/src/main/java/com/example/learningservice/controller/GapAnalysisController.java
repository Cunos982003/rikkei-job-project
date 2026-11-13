package com.example.learningservice.controller;

import com.example.learningservice.model.dto.request.TargetSelectionRequest;
import com.example.learningservice.model.dto.response.ApiResponse;
import com.example.learningservice.model.dto.response.CandidateTargetResponse;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.CandidateTargetService;
import com.example.learningservice.service.GapAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/learning/me", "/learning/me", "/api/v1/me", "/me"})
@RequiredArgsConstructor
@Tag(name = "Gap Analysis & Target Selection", description = "Quản lý mục tiêu ứng viên và phân tích khoảng thiếu hụt năng lực (DT-01)")
public class GapAnalysisController {

    private final CandidateTargetService candidateTargetService;
    private final GapAnalysisService gapAnalysisService;
    private final UserContextHelper userContextHelper;

    @PutMapping("/target")
    @Operation(summary = "Thiết lập hoặc cập nhật mục tiêu nghề nghiệp (Vị trí hoặc JD)",
            description = "Học viên chọn 1 vị trí mục tiêu (positionId) hoặc 1 tin tuyển dụng (jobId). Yêu cầu đăng nhập với vai trò CANDIDATE.")
    public ResponseEntity<ApiResponse<CandidateTargetResponse>> setTarget(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody TargetSelectionRequest request) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Thiết lập mục tiêu cho userId: {}, payload: {}", userId, request);

        CandidateTargetResponse response = candidateTargetService.setTarget(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Thiết lập mục tiêu nghề nghiệp thành công", response));
    }

    @GetMapping("/target")
    @Operation(summary = "Lấy mục tiêu nghề nghiệp hiện tại của ứng viên",
            description = "Trả về thông tin vị trí mục tiêu hoặc JD đang được chọn của học viên đăng nhập hiện tại. Yêu cầu vai trò CANDIDATE.")
    public ResponseEntity<ApiResponse<CandidateTargetResponse>> getCurrentTarget(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Lấy mục tiêu hiện tại của userId: {}", userId);

        return candidateTargetService.getCurrentTarget(userId)
                .map(target -> ResponseEntity.ok(ApiResponse.success("Lấy mục tiêu hiện tại thành công", target)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Chưa thiết lập mục tiêu", null)));
    }

    @GetMapping("/gap-analysis")
    @Operation(summary = "Phân tích khoảng thiếu hụt năng lực theo mục tiêu hoặc JD cụ thể",
            description = "So sánh năng lực ứng viên (verified_score) với yêu cầu của vị trí mục tiêu hoặc JD. " +
                    "Yêu cầu đăng nhập với vai trò CANDIDATE. " +
                    "Nếu truyền query param jobId, hệ thống sẽ thực hiện phân tích tức thời theo JD đó (phục vụ nút 'Phân tích so với JD này'). " +
                    "Kết quả trả về danh sách kỹ năng thiếu sắp theo gap giảm dần và tỷ lệ sẵn sàng readiness.")
    public ResponseEntity<ApiResponse<GapAnalysisResponse>> getGapAnalysis(
            @Parameter(description = "Mã tin tuyển dụng nếu muốn phân tích tức thời theo JD", example = "job-001")
            @RequestParam(value = "jobId", required = false) String jobId,
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Yêu cầu phân tích gap cho userId: {}, jobId: {}", userId, jobId);

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(userId, jobId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Phân tích khoảng thiếu hụt năng lực thành công", response));
    }
}


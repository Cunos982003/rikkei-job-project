package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.ApiResponse;
import com.example.ai_career_service.dto.interview.CreatePracticeRequest;
import com.example.ai_career_service.dto.interview.InterviewPracticeResponse;
import com.example.ai_career_service.dto.interview.PracticeQuestionResponse;
import com.example.ai_career_service.dto.interview.SubmitAnswerRequest;
import com.example.ai_career_service.service.InterviewPracticeService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/me/interview-practices")
@RequiredArgsConstructor
@Validated
public class InterviewPracticeController {

    private final InterviewPracticeService practiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewPracticeResponse>> createPractice(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @Valid @RequestBody CreatePracticeRequest request) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        InterviewPracticeResponse response = practiceService.createPractice(userId, request.jobId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Khởi tạo buổi luyện phỏng vấn thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewPracticeResponse>>> listPractices(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        List<InterviewPracticeResponse> list = practiceService.listPractices(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách luyện phỏng vấn thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewPracticeResponse>> getPractice(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        InterviewPracticeResponse practice = practiceService.getPractice(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin buổi luyện tập thành công", practice));
    }

    @PostMapping("/{id}/questions/{qid}/answer")
    public ResponseEntity<ApiResponse<PracticeQuestionResponse>> submitAnswer(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id,
            @PathVariable("qid") UUID qid,
            @Valid @RequestBody SubmitAnswerRequest request) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        PracticeQuestionResponse question = practiceService.submitAnswer(userId, id, qid, request.answer());
        return ResponseEntity.ok(ApiResponse.success("Chấm điểm câu trả lời thành công", question));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<InterviewPracticeResponse>> finishPractice(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        InterviewPracticeResponse practice = practiceService.finishPractice(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Hoàn thành buổi luyện phỏng vấn", practice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePractice(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id) {
        UUID userId = extractUserId(principal, headerUserId, headerRole);
        practiceService.deletePractice(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa buổi luyện phỏng vấn thành công", null));
    }

    private UUID extractUserId(Principal principal, String headerUserId, String headerRole) {
        if (headerRole != null && !headerRole.isBlank()) {
            String normalizedRole = headerRole.trim().toUpperCase(java.util.Locale.ROOT);
            if (normalizedRole.startsWith("ROLE_")) {
                normalizedRole = normalizedRole.substring(5);
            }
            if (!"CANDIDATE".equals(normalizedRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chức năng này chỉ dành cho ứng viên (CANDIDATE)");
            }
        }
        String rawId = (principal != null && principal.getName() != null && !principal.getName().isBlank())
                ? principal.getName()
                : headerUserId;
        if (rawId == null || rawId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập tài khoản ứng viên");
        }
        try {
            return UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Định dạng ID người dùng không hợp lệ");
        }
    }
}

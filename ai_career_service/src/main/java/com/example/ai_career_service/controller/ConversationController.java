package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.ApiResponse;
import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import com.example.ai_career_service.dto.chat.ChatMessageResponse;
import com.example.ai_career_service.dto.chat.ConversationResponse;
import com.example.ai_career_service.dto.chat.CreateConversationRequest;
import com.example.ai_career_service.dto.chat.SendMessageRequest;
import com.example.ai_career_service.dto.chat.SendMessageResponse;
import com.example.ai_career_service.service.AiUsageService;
import com.example.ai_career_service.service.ConversationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/me/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final ConversationService conversationService;
    private final AiUsageService aiUsageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @Valid @RequestBody CreateConversationRequest request) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        ConversationResponse created = conversationService.createConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo cuộc hội thoại thành công", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách hội thoại thành công",
                conversationService.listConversations(userId)));
    }

    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<AiQuotaResponse>> getQuota(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy hạn mức sử dụng AI thành công",
                aiUsageService.getQuota(userId)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy lịch sử tin nhắn thành công",
                conversationService.getMessages(userId, id)));
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SendMessageResponse>> sendMessage(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        SendMessageResponse response = conversationService.sendMessage(userId, id, request.content());
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", response));
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = resolveCandidateId(principal, headerUserId, headerRole);
        return conversationService.streamMessage(userId, id, request.content());
    }

    private UUID resolveCandidateId(Principal principal, String headerUserId, String headerRole) {
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

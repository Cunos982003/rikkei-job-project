package com.example.profileservice.profile.api;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.profile.api.dto.EducationDto;
import com.example.profileservice.profile.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/me/educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @PostMapping
    public ResponseEntity<ApiResponse<EducationDto>> createEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody EducationDto dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        EducationDto created = educationService.createEducation(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Education created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EducationDto>>> getEducations(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        List<EducationDto> educations = educationService.getEducations(userId);
        return ResponseEntity.ok(ApiResponse.success("Educations retrieved successfully", educations));
    }

    @GetMapping("/{educationId}")
    public ResponseEntity<ApiResponse<EducationDto>> getEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        EducationDto education = educationService.getEducation(userId, educationId);
        return ResponseEntity.ok(ApiResponse.success("Education retrieved successfully", education));
    }

    @PutMapping("/{educationId}")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId,
            @RequestBody EducationDto dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        EducationDto updated = educationService.updateEducation(userId, educationId, dto);
        return ResponseEntity.ok(ApiResponse.success("Education updated successfully", updated));
    }

    @DeleteMapping("/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        educationService.deleteEducation(userId, educationId);
        return ResponseEntity.ok(ApiResponse.success("Education deleted successfully", null));
    }
}
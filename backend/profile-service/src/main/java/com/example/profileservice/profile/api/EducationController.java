package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.EducationDto;
import com.example.profileservice.profile.service.EducationService;
import com.example.profileservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/me/educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @PostMapping
    public ResponseEntity<EducationDto> createEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EducationDto dto) {
        UUID userId = principal.getUserId();
        EducationDto created = educationService.createEducation(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<EducationDto>> getEducations(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUserId();
        List<EducationDto> educations = educationService.getEducations(userId);
        return ResponseEntity.ok(educations);
    }

    @GetMapping("/{educationId}")
    public ResponseEntity<EducationDto> getEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId) {
        UUID userId = principal.getUserId();
        EducationDto education = educationService.getEducation(userId, educationId);
        return ResponseEntity.ok(education);
    }

    @PutMapping("/{educationId}")
    public ResponseEntity<EducationDto> updateEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId,
            @RequestBody EducationDto dto) {
        UUID userId = principal.getUserId();
        EducationDto updated = educationService.updateEducation(userId, educationId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{educationId}")
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId) {
        UUID userId = principal.getUserId();
        educationService.deleteEducation(userId, educationId);
        return ResponseEntity.noContent().build();
    }
}
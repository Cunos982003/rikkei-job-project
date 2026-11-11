package com.example.profileservice.profile.api;

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
    public ResponseEntity<EducationDto> createEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody EducationDto dto) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        EducationDto created = educationService.createEducation(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<EducationDto>> getEducations(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<EducationDto> educations = educationService.getEducations(userId);
        return ResponseEntity.ok(educations);
    }

    @GetMapping("/{educationId}")
    public ResponseEntity<EducationDto> getEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        EducationDto education = educationService.getEducation(userId, educationId);
        return ResponseEntity.ok(education);
    }

    @PutMapping("/{educationId}")
    public ResponseEntity<EducationDto> updateEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId,
            @RequestBody EducationDto dto) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        EducationDto updated = educationService.updateEducation(userId, educationId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{educationId}")
    public ResponseEntity<Void> deleteEducation(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID educationId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        educationService.deleteEducation(userId, educationId);
        return ResponseEntity.noContent().build();
    }
}
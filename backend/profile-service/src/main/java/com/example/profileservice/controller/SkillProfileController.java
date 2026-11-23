package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.dto.SkillProfileResponse;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillEvidence;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.repository.SkillEvidenceRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import com.example.profileservice.service.ProfileVisibilityService;
import com.example.profileservice.service.SkillProfileMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profiles")
public class SkillProfileController {

    private final SkillRepository skillRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillProfileMapper mapper;
    private final ProfileVisibilityService visibilityService;

    public SkillProfileController(
            SkillRepository skillRepo,
            SkillScoreRepository scoreRepo,
            SkillEvidenceRepository evidenceRepo,
            SkillProfileMapper mapper,
            ProfileVisibilityService visibilityService) {
        this.skillRepo = skillRepo;
        this.scoreRepo = scoreRepo;
        this.evidenceRepo = evidenceRepo;
        this.mapper = mapper;
        this.visibilityService = visibilityService;
    }

    @GetMapping("/me/skills")
    public ResponseEntity<ApiResponse<List<SkillProfileResponse>>> getMySkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy kỹ năng thành công", buildResponse(userId, userId)));
    }

    @GetMapping("/{userId}/skills")
    public ResponseEntity<ApiResponse<List<SkillProfileResponse>>> getUserSkills(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId) {
        if (requesterId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        if (!visibilityService.canViewProfile(requesterId, userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy kỹ năng thành công", buildResponse(userId, requesterId)));
    }

    private List<SkillProfileResponse> buildResponse(UUID targetUserId, UUID requesterId) {
        List<SkillScore> scores = scoreRepo.findByUserId(targetUserId);
        return scores.stream().map(score -> {
            Skill skill = skillRepo.findById(score.getSkillId()).orElseThrow();
            List<SkillEvidence> evidences = evidenceRepo.findByUserIdAndSkillId(targetUserId, skill.getId());
            return mapper.toDto(skill, score, evidences);
        }).collect(Collectors.toList());
    }
}
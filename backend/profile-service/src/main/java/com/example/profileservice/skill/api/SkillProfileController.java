package com.example.profileservice.skill.api;

import com.example.profileservice.security.UserPrincipal;
import com.example.profileservice.skill.api.dto.SkillProfileResponse;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<List<SkillProfileResponse>> getMySkills(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(buildResponse(userId, userId));
    }

    @GetMapping("/{userId}/skills")
    public ResponseEntity<List<SkillProfileResponse>> getUserSkills(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID requesterId = principal.getUserId();
        if (!visibilityService.canViewProfile(requesterId, userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildResponse(userId, requesterId));
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
package com.example.profileservice.skill.api;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.skill.api.dto.SkillScoreInternalResponse;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles/internal")
@RequiredArgsConstructor
public class InternalSkillScoreController {

    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;

    @GetMapping("/{userId}/skill-scores")
    public ResponseEntity<ApiResponse<List<SkillScoreInternalResponse>>> getCandidateSkillScores(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                || (authHeader != null && authHeader.startsWith("Bearer "));

        if (!isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập"));
        }

        log.info("Lấy điểm kỹ năng nội bộ cho userId={}", userId);
        List<SkillScore> scores = scoreRepo.findByUserId(userId);
        if (scores == null || scores.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Lấy điểm kỹ năng thành công", Collections.emptyList()));
        }

        List<SkillScoreInternalResponse> data = scores.stream()
                .map(score -> {
                    Skill skill = skillRepo.findById(score.getSkillId()).orElse(null);
                    return new SkillScoreInternalResponse(
                            score.getSkillId() != null ? score.getSkillId().toString() : (skill != null ? skill.getId().toString() : null),
                            skill != null ? skill.getCode() : null,
                            skill != null ? skill.getName() : null,
                            score.getVerifiedScore() != null ? score.getVerifiedScore().intValue() : null,
                            score.getSelfScore() != null ? score.getSelfScore().intValue() : null
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy điểm kỹ năng thành công", data));
    }
}


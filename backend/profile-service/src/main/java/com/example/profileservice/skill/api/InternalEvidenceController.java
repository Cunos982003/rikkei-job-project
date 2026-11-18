package com.example.profileservice.skill.api;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.skill.application.SkillRecomputeService;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillMapping;
import com.example.profileservice.skill.infrastructure.SkillMappingRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles/internal")
@RequiredArgsConstructor
public class InternalEvidenceController {

    private final SkillRecomputeService recomputeService;
    private final SkillRepository skillRepo;
    private final SkillMappingRepository mappingRepo;

    @Data
    public static class LmsEvidenceRequest {
        private UUID userId;
        private String skillCode;
        private String courseId;
        private String courseName;
        private Short score;
    }

    @PostMapping("/lms-evidence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addLmsEvidence(@RequestBody LmsEvidenceRequest request) {
        log.info("Nhận bằng chứng LMS nội bộ cho userId={}, courseId={}, skillCode={}, score={}",
                request.getUserId(), request.getCourseId(), request.getSkillCode(), request.getScore());

        if (request.getUserId() == null || request.getCourseId() == null || request.getScore() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc"));
        }

        // 1. Tìm kỹ năng theo skillCode và đảm bảo ánh xạ SkillMapping tồn tại
        if (request.getSkillCode() != null && !request.getSkillCode().isBlank()) {
            Optional<Skill> skillOpt = skillRepo.findByCode(request.getSkillCode());
            if (skillOpt.isPresent()) {
                Skill skill = skillOpt.get();
                List<SkillMapping> existing = mappingRepo.findBySourceAndSourceCode("LMS", request.getCourseId());
                boolean hasMapping = existing.stream().anyMatch(m -> m.getSkill().getId().equals(skill.getId()));
                if (!hasMapping) {
                    SkillMapping newMap = SkillMapping.builder()
                            .source("LMS")
                            .sourceCode(request.getCourseId())
                            .skill(skill)
                            .weight(new BigDecimal("1.00"))
                            .build();
                    mappingRepo.save(newMap);
                }
            }
        }

        // 2. Tạo ExternalRecordDto và chạy recomputeSkills
        ExternalRecordDto record = new ExternalRecordDto(
                "LMS",
                request.getCourseId(),
                request.getCourseName() != null ? request.getCourseName() : request.getCourseId(),
                request.getScore(),
                OffsetDateTime.now()
        );

        recomputeService.recomputeSkills(request.getUserId(), List.of(record));
        log.info("Đã kích hoạt recomputeSkills thành công cho userId={}", request.getUserId());

        Map<String, Object> data = Map.of(
                "userId", request.getUserId().toString(),
                "courseId", request.getCourseId()
        );
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật bằng chứng LMS và tính toán lại điểm kỹ năng", data));
    }
}


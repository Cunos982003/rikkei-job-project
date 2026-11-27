package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.entity.*;
import com.example.profileservice.exception.ResourceNotFoundException;
import com.example.profileservice.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles/internal/cvs")
@RequiredArgsConstructor
public class InternalCvDraftController {

    private final CVRepository cvRepository;
    private final CVSectionRepository cvSectionRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final SkillScoreRepository skillScoreRepository;
    private final SkillRepository skillRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final EducationRepository educationRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/{cvId}/snapshot")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCvDraftSnapshot(
            @PathVariable UUID cvId,
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID projectId) {

        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập CV này");
        }

        List<PortfolioProject> allProjects = portfolioProjectRepository.findByUserIdOrderBySortOrderAsc(userId);
        List<PortfolioProject> targetProjects;
        if (projectId != null) {
            PortfolioProject project = portfolioProjectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án"));
            if (!project.getUserId().equals(userId)) {
                throw new AccessDeniedException("Dự án không thuộc về ứng viên này");
            }
            targetProjects = List.of(project);
        } else {
            targetProjects = allProjects.stream()
                    .filter(PortfolioProject::isVisible)
                    .toList();
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);

        Map<UUID, Skill> skillMap = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, s -> s));

        // Chỉ lấy kỹ năng ĐÃ XÁC THỰC (verifiedScore != null), tuyệt đối không đưa kỹ năng tự khai vào
        List<Map<String, Object>> verifiedSkills = skillScoreRepository.findByUserId(userId).stream()
                .filter(ss -> ss.getVerifiedScore() != null)
                .map(ss -> {
                    Skill skill = skillMap.get(ss.getSkillId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("skillId", ss.getSkillId().toString());
                    item.put("code", skill != null ? skill.getCode() : null);
                    item.put("name", skill != null ? skill.getName() : null);
                    item.put("verifiedScore", ss.getVerifiedScore());
                    item.put("level", ss.getLevel() != null ? ss.getLevel().label() : null);
                    return item;
                })
                .toList();

        List<Map<String, Object>> projects = targetProjects.stream()
                .map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", p.getId().toString());
                    item.put("name", p.getName());
                    item.put("role", p.getRole());
                    item.put("description", p.getDescription());
                    item.put("techStack", p.getTechStack());
                    item.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
                    item.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
                    return item;
                })
                .toList();

        List<Map<String, Object>> education = educationRepository.findByUserId(userId).stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", e.getId().toString());
                    item.put("school", e.getSchool());
                    item.put("major", e.getMajor());
                    item.put("fromDate", e.getFromDate() != null ? e.getFromDate().toString() : null);
                    item.put("toDate", e.getToDate() != null ? e.getToDate().toString() : null);
                    item.put("gpa", e.getGpa() != null ? e.getGpa().toString() : null);
                    item.put("note", e.getNote());
                    return item;
                })
                .toList();

        List<Map<String, Object>> internships = loadInternships(userId);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("cvId", cv.getId().toString());
        snapshot.put("cvTitle", cv.getTitle());
        snapshot.put("targetPosition", cv.getTargetPosition());
        snapshot.put("headline", profile != null ? profile.getHeadline() : null);
        snapshot.put("currentSummary", profile != null ? profile.getSummary() : null);
        snapshot.put("verifiedSkills", verifiedSkills);
        snapshot.put("projects", projects);
        snapshot.put("internships", internships);
        snapshot.put("education", education);

        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu hồ sơ cho AI viết CV thành công", snapshot));
    }

    @PostMapping("/{cvId}/apply-draft")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyDraftToCv(
            @PathVariable UUID cvId,
            @Valid @RequestBody ApplyDraftRequest request) {

        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(request.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa CV này");
        }

        String section = request.getSection().trim().toUpperCase(Locale.ROOT);
        if ("SUMMARY".equals(section)) {
            CandidateProfile profile = candidateProfileRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng viên"));
            profile.setSummary(request.getContent());
            candidateProfileRepository.save(profile);

            cvSectionRepository.findByCvIdAndSectionType(cvId, SectionType.SUMMARY)
                    .orElseGet(() -> cvSectionRepository.save(CVSection.builder()
                            .cv(cv)
                            .sectionType(SectionType.SUMMARY)
                            .itemIds(List.of())
                            .included(true)
                            .sortOrder(0)
                            .build()));
        } else if ("PROJECT".equals(section)) {
            if (request.getProjectId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId là bắt buộc khi áp dụng bản nháp dự án");
            }
            PortfolioProject project = portfolioProjectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án"));
            if (!project.getUserId().equals(request.getUserId())) {
                throw new AccessDeniedException("Dự án không thuộc về ứng viên này");
            }
            project.setDescription(request.getContent());
            portfolioProjectRepository.save(project);

            CVSection projectSection = cvSectionRepository.findByCvIdAndSectionType(cvId, SectionType.PROJECTS)
                    .orElseGet(() -> CVSection.builder()
                            .cv(cv)
                            .sectionType(SectionType.PROJECTS)
                            .itemIds(new ArrayList<>())
                            .included(true)
                            .sortOrder(3)
                            .build());
            List<UUID> itemIds = new ArrayList<>(projectSection.getItemIds() != null ? projectSection.getItemIds() : List.of());
            if (!itemIds.contains(project.getId())) {
                itemIds.add(project.getId());
                projectSection.setItemIds(itemIds);
            }
            cvSectionRepository.save(projectSection);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section không hợp lệ: " + request.getSection());
        }

        log.info("Đã áp dụng AI draft {} vào CV {} (section={}, projectId={})",
                request.getDraftId(), cvId, section, request.getProjectId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cvId", cvId.toString());
        result.put("draftId", request.getDraftId() != null ? request.getDraftId().toString() : null);
        result.put("section", section);
        result.put("projectId", request.getProjectId() != null ? request.getProjectId().toString() : null);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nội dung bản nháp vào CV", result));
    }

    private List<Map<String, Object>> loadInternships(UUID userId) {
        String sql = """
                SELECT i.id, i.batch_name, i.topic_name, i.group_name, i.start_date, i.end_date, i.final_score,
                       COALESCE((
                           SELECT json_agg(json_build_object('title', t.title, 'acceptedScore', t.accepted_score))
                           FROM internship_tasks t
                           WHERE t.internship_id = i.id
                       ), '[]'::json) AS tasks
                FROM internships i
                WHERE i.user_id = ?
                ORDER BY i.start_date DESC NULLS LAST
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id") != null ? row.get("id").toString() : null);
            item.put("batchName", row.get("batch_name"));
            item.put("topicName", row.get("topic_name"));
            item.put("groupName", row.get("group_name"));
            item.put("startDate", row.get("start_date") != null ? row.get("start_date").toString() : null);
            item.put("endDate", row.get("end_date") != null ? row.get("end_date").toString() : null);
            item.put("finalScore", row.get("final_score"));
            item.put("tasks", row.get("tasks"));
            result.add(item);
        }
        return result;
    }

    @Data
    public static class ApplyDraftRequest {
        @NotNull
        private UUID userId;
        private UUID draftId;
        @NotBlank
        private String section;
        private UUID projectId;
        @NotBlank
        private String content;
    }
}

package com.example.profileservice.service;

import com.example.profileservice.dto.CVDto;
import com.example.profileservice.entity.*;
import com.example.profileservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileDataService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final EducationRepository educationRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final CertificateRepository certificateRepository;
    private final SkillScoreRepository skillScoreRepository;
    private final SkillRepository skillRepository;
    private final JdbcTemplate jdbcTemplate;

    // Date formatter for Vietnamese format
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CVDto.PersonalInfoPreview getPersonalInfo(UUID userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return CVDto.PersonalInfoPreview.builder().build();
        }
        return CVDto.PersonalInfoPreview.builder()
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .dob(profile.getDob() != null ? profile.getDob().format(DATE_FORMATTER) : null)
                .gender(profile.getGender())
                .headline(profile.getHeadline())
                .build();
    }

    public List<CVDto.EducationPreview> getEducation(UUID userId, List<UUID> itemIds) {
        List<Education> allEducations = educationRepository.findByUserId(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> idSet = new HashSet<>(itemIds);
        return allEducations.stream()
                .filter(e -> idSet.contains(e.getId()))
                .map(this::toEducationPreview)
                .collect(Collectors.toList());
    }

    public List<CVDto.ProjectPreview> getProjects(UUID userId, List<UUID> itemIds) {
        List<PortfolioProject> allProjects = portfolioProjectRepository.findByUserIdOrderBySortOrderAsc(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> idSet = new HashSet<>(itemIds);
        return allProjects.stream()
                .filter(p -> idSet.contains(p.getId()))
                .map(this::toProjectPreview)
                .collect(Collectors.toList());
    }

    public List<CVDto.CertificatePreview> getCertificates(UUID userId, List<UUID> itemIds) {
        List<Certificate> allCertificates = certificateRepository.findByUserId(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> idSet = new HashSet<>(itemIds);
        return allCertificates.stream()
                .filter(c -> idSet.contains(c.getId()))
                .map(this::toCertificatePreview)
                .collect(Collectors.toList());
    }

    public List<CVDto.SkillPreview> getSkills(UUID userId, List<UUID> itemIds) {
        List<SkillScore> allSkillScores = skillScoreRepository.findByUserId(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> idSet = new HashSet<>(itemIds);

        // Build skill name map
        Map<UUID, String> skillNames = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));

        return allSkillScores.stream()
                .filter(s -> idSet.contains(s.getSkillId()))
                .map(ss -> toSkillPreview(ss, skillNames.get(ss.getSkillId())))
                .collect(Collectors.toList());
    }

    public List<CVDto.InternshipPreview> getInternships(UUID userId, List<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        // Use JdbcTemplate since there's no JPA entity for Internship
        String placeholders = itemIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
            SELECT i.*,
                   COALESCE((
                       SELECT json_agg(t ORDER BY t.accepted_at)
                       FROM internship_tasks t
                       WHERE t.internship_id = i.id
                   ), '[]'::json) AS tasks
            FROM internships i
            WHERE i.user_id = ? AND i.id IN (""" + placeholders + ")";

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(itemIds);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        return rows.stream()
                .map(this::toInternshipPreview)
                .collect(Collectors.toList());
    }

    public CVDto.SummaryPreview getSummary(UUID userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        return CVDto.SummaryPreview.builder()
                .summary(profile != null ? profile.getSummary() : null)
                .build();
    }

    private CVDto.EducationPreview toEducationPreview(Education e) {
        return CVDto.EducationPreview.builder()
                .id(e.getId())
                .school(e.getSchool())
                .major(e.getMajor())
                .fromDate(e.getFromDate() != null ? e.getFromDate().format(DATE_FORMATTER) : null)
                .toDate(e.getToDate() != null ? e.getToDate().format(DATE_FORMATTER) : null)
                .gpa(e.getGpa() != null ? e.getGpa().toString() : null)
                .note(e.getNote())
                .build();
    }

    private CVDto.ProjectPreview toProjectPreview(PortfolioProject p) {
        return CVDto.ProjectPreview.builder()
                .id(p.getId())
                .name(p.getName())
                .role(p.getRole())
                .description(p.getDescription())
                .techStack(p.getTechStack())
                .repoUrl(p.getRepoUrl())
                .demoUrl(p.getDemoUrl())
                .startDate(p.getStartDate() != null ? p.getStartDate().format(DATE_FORMATTER) : null)
                .endDate(p.getEndDate() != null ? p.getEndDate().format(DATE_FORMATTER) : null)
                .build();
    }

    private CVDto.CertificatePreview toCertificatePreview(Certificate c) {
        return CVDto.CertificatePreview.builder()
                .id(c.getId())
                .name(c.getName())
                .issuer(c.getIssuer())
                .issuedAt(c.getIssuedAt() != null ? c.getIssuedAt().format(DATE_FORMATTER) : null)
                .expiresAt(c.getExpiresAt() != null ? c.getExpiresAt().format(DATE_FORMATTER) : null)
                .build();
    }

    private CVDto.SkillPreview toSkillPreview(SkillScore ss, String skillName) {
        boolean hasVerified = ss.getVerifiedScore() != null;
        return CVDto.SkillPreview.builder()
                .skillId(ss.getSkillId())
                .skillName(skillName)
                .level(ss.getLevel().label())
                .verifiedScore(ss.getVerifiedScore())
                .selfScore(ss.getSelfScore())
                .verificationStatus(hasVerified ? "Đã xác thực" : "Tự khai")
                .build();
    }

    private CVDto.InternshipPreview toInternshipPreview(Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) row.get("tasks");

        List<CVDto.InternshipTaskPreview> taskPreviews = tasks != null ? tasks.stream()
                .map(t -> CVDto.InternshipTaskPreview.builder()
                        .title((String) t.get("title"))
                        .acceptedScore(t.get("accepted_score") != null ? t.get("accepted_score").toString() : null)
                        .acceptedAt(t.get("accepted_at") != null ? t.get("accepted_at").toString() : null)
                        .build())
                .collect(Collectors.toList()) : List.of();

        return CVDto.InternshipPreview.builder()
                .id((UUID) row.get("id"))
                .batchName((String) row.get("batch_name"))
                .topicName((String) row.get("topic_name"))
                .groupName((String) row.get("group_name"))
                .startDate(row.get("start_date") != null ? row.get("start_date").toString() : null)
                .endDate(row.get("end_date") != null ? row.get("end_date").toString() : null)
                .finalScore(row.get("final_score") != null ? row.get("final_score").toString() : null)
                .tasks(taskPreviews)
                .build();
    }
}
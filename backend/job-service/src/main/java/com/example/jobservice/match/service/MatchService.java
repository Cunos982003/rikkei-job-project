package com.example.jobservice.match.service;

import com.example.jobservice.common.exception.ForbiddenException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.job.domain.Job;
import com.example.jobservice.job.domain.JobRepository;
import com.example.jobservice.job.domain.JobSkill;
import com.example.jobservice.job.domain.JobSkillRepository;
import com.example.jobservice.job.domain.JobStatus;
import com.example.jobservice.match.client.CandidateSkillClient;
import com.example.jobservice.match.client.CandidateSkillScore;
import com.example.jobservice.match.domain.MatchScore;
import com.example.jobservice.match.domain.MatchScoreId;
import com.example.jobservice.match.domain.MatchScoreRepository;
import com.example.jobservice.match.dto.MatchResultResponse;
import com.example.jobservice.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final CandidateSkillClient candidateSkillClient;
    private final ObjectMapper objectMapper;

    public MatchService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            MatchScoreRepository matchScoreRepository,
            CandidateSkillClient candidateSkillClient,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.matchScoreRepository = matchScoreRepository;
        this.candidateSkillClient = candidateSkillClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    public MatchResultResponse computeMatch(UUID jobId, UUID candidateUserId, UserPrincipal principal) {
        log.info("Tính toán điểm khớp cho jobId={}, candidateUserId={}, requester={}",
                jobId, candidateUserId, principal != null ? principal.getEmail() : "anonymous");

        // 1. Kiểm tra quyền truy cập JD theo quy tắc sẵn có của JobService.getJob
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        String role = principal != null && principal.getRole() != null ? principal.getRole() : "";
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        if ("HR".equalsIgnoreCase(role)) {
            if (principal == null || principal.getCompanyId() == null || !job.getCompanyId().equals(principal.getCompanyId())) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
        } else if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            // STAFF / ADMIN được xem mọi JD
        } else if ("CANDIDATE".equalsIgnoreCase(role)) {
            // CANDIDATE chỉ xem được JD APPROVED và candidateUserId PHẢI là id của chính họ
            if (job.getStatus() != JobStatus.APPROVED) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
            if (principal == null || principal.getId() == null || !principal.getId().equals(candidateUserId)) {
                throw new ForbiddenException("Không có quyền xem điểm khớp của ứng viên khác");
            }
        } else {
            if (job.getStatus() != JobStatus.APPROVED) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
        }

        // 2. Lấy danh sách kỹ năng JD và kỹ năng ứng viên
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(jobId);
        List<CandidateSkillScore> candidateSkills = candidateSkillClient.getSkills(candidateUserId);

        // 3. Tính điểm khớp qua MatchCalculator
        MatchCalculator.MatchResult matchResult = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        // 4. UPSERT match_scores (LUÔN tính lại, không bao giờ trả bản đã lưu)
        OffsetDateTime now = OffsetDateTime.now();
        String detailJson;
        try {
            Map<String, Object> detailMap = new LinkedHashMap<>();
            detailMap.put("met", matchResult.met());
            detailMap.put("missing", matchResult.missing());
            detailJson = objectMapper.writeValueAsString(detailMap);
        } catch (Exception e) {
            log.warn("Không thể tuần tự hóa chi tiết kỹ năng thành JSON", e);
            detailJson = "{}";
        }

        MatchScoreId id = new MatchScoreId(jobId, candidateUserId);
        MatchScore entity = matchScoreRepository.findById(id).orElse(null);
        if (entity == null) {
            entity = new MatchScore(
                    jobId,
                    candidateUserId,
                    matchResult.score(),
                    matchResult.belowMinimum(),
                    detailJson,
                    false,
                    now
            );
        } else {
            entity.setScore(matchResult.score());
            entity.setBelowMinimum(matchResult.belowMinimum());
            entity.setDetail(detailJson);
            entity.setStale(false);
            entity.setComputedAt(now);
        }
        matchScoreRepository.save(entity);

        return new MatchResultResponse(
                jobId,
                candidateUserId,
                matchResult.score().doubleValue(),
                matchResult.belowMinimum(),
                matchResult.met(),
                matchResult.missing(),
                now,
                false
        );
    }
}


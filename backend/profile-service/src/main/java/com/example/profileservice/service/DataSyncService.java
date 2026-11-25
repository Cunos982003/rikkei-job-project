package com.example.profileservice.service;

import com.example.profileservice.client.ExternalMockClient;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.ExternalRecord;
import com.example.profileservice.entity.ManualOverride;
import com.example.profileservice.entity.SyncRun;
import com.example.profileservice.repository.CandidateProfileRepository;
import com.example.profileservice.repository.ExternalRecordRepository;
import com.example.profileservice.repository.ManualOverrideRepository;
import com.example.profileservice.repository.SyncRunRepository;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final ExternalMockClient mockClient;
    private final ExternalRecordRepository externalRecordRepo;
    private final ManualOverrideRepository manualOverrideRepo;
    private final SyncRunRepository syncRunRepo;
    private final CandidateProfileRepository candidateProfileRepo;
    private final SkillRecomputeService skillRecomputeService;
    private final ObjectMapper objectMapper;

    /**
     * Đồng bộ dữ liệu cho một ứng viên cụ thể
     */
    @Transactional
    public SyncRun syncUser(UUID userId, String trigger) {
        log.info("Starting sync for candidate userId={} with trigger={}", userId, trigger);
        SyncRun run = SyncRun.builder()
                .trigger(trigger != null ? trigger : "MANUAL")
                .userId(userId)
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .counts("{}")
                .build();
        run = syncRunRepo.save(run);

        try {
            CandidateProfile profile = candidateProfileRepo.findById(userId).orElse(null);
            String email = profile != null ? profile.getEmail() : null;
            String studentId = mockClient.resolveStudentId(userId, email);

            // Fetch from mock endpoints
            JsonNode lmsResults = mockClient.getLmsResults(studentId);
            JsonNode examResults = mockClient.getExamResults(studentId);
            JsonNode pmInternships = mockClient.getPmInternships(studentId);

            List<ExternalRecordDto> dtosToRecompute = new ArrayList<>();
            int lmsCount = processLmsResults(userId, studentId, lmsResults, dtosToRecompute);
            int examCount = processExamResults(userId, studentId, examResults, dtosToRecompute);
            int pmCount = processPmInternships(userId, studentId, pmInternships, dtosToRecompute);
            int total = lmsCount + examCount + pmCount;

            Map<String, Object> countsMap = Map.of(
                    "lms", lmsCount,
                    "exam", examCount,
                    "pm", pmCount,
                    "total", total
            );

            run.setStatus("SUCCESS");
            OffsetDateTime now = OffsetDateTime.now();
            run.setFinishedAt(now);
            run.setCompletedAt(now);
            run.setCounts(objectMapper.writeValueAsString(countsMap));
            run = syncRunRepo.save(run);

            // Tự động tính toán lại điểm kỹ năng (HS-01)
            if (!dtosToRecompute.isEmpty()) {
                try {
                    skillRecomputeService.recomputeSkills(userId, dtosToRecompute);
                    log.info("Successfully recomputed skills for candidate {}", userId);
                } catch (Exception ex) {
                    log.error("Failed to recompute skills for candidate {}: {}", userId, ex.getMessage(), ex);
                }
            }

            log.info("Sync finished successfully for candidate {}: total {} records", userId, total);
            return run;
        } catch (Exception e) {
            log.error("Sync failed for candidate {}: {}", userId, e.getMessage(), e);
            run.setStatus("FAILED");
            OffsetDateTime now = OffsetDateTime.now();
            run.setFinishedAt(now);
            run.setCompletedAt(now);
            run.setError(e.getMessage());
            return syncRunRepo.save(run);
        }
    }

    /**
     * Đồng bộ cho toàn bộ ứng viên (Dùng cho Nightly Job)
     */
    @Transactional
    public SyncRun syncAllCandidates(String trigger) {
        log.info("Starting nightly sync for ALL candidates with trigger={}", trigger);
        SyncRun run = SyncRun.builder()
                .trigger(trigger != null ? trigger : "NIGHTLY")
                .userId(null)
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .counts("{}")
                .build();
        run = syncRunRepo.save(run);

        try {
            List<CandidateProfile> candidates = candidateProfileRepo.findAll();
            int totalRecords = 0;
            int totalLms = 0;
            int totalExam = 0;
            int totalPm = 0;

            for (CandidateProfile c : candidates) {
                try {
                    SyncRun subRun = syncUser(c.getUserId(), trigger);
                    if ("SUCCESS".equals(subRun.getStatus()) && subRun.getCounts() != null) {
                        JsonNode countsNode = objectMapper.readTree(subRun.getCounts());
                        totalLms += countsNode.path("lms").asInt(0);
                        totalExam += countsNode.path("exam").asInt(0);
                        totalPm += countsNode.path("pm").asInt(0);
                        totalRecords += countsNode.path("total").asInt(0);
                    }
                } catch (Exception ex) {
                    log.warn("Error syncing candidate {}: {}", c.getUserId(), ex.getMessage());
                }
            }

            Map<String, Object> countsMap = Map.of(
                    "candidates", candidates.size(),
                    "lms", totalLms,
                    "exam", totalExam,
                    "pm", totalPm,
                    "total", totalRecords
            );

            run.setStatus("SUCCESS");
            OffsetDateTime now = OffsetDateTime.now();
            run.setFinishedAt(now);
            run.setCompletedAt(now);
            run.setCounts(objectMapper.writeValueAsString(countsMap));
            return syncRunRepo.save(run);
        } catch (Exception e) {
            log.error("Nightly sync failed: {}", e.getMessage(), e);
            run.setStatus("FAILED");
            OffsetDateTime now = OffsetDateTime.now();
            run.setFinishedAt(now);
            run.setCompletedAt(now);
            run.setError(e.getMessage());
            return syncRunRepo.save(run);
        }
    }

    /**
     * Kiểm tra và kích hoạt đồng bộ nền khi ứng viên mở hồ sơ nếu đã quá 1 giờ
     */
    public void triggerBackgroundSyncIfStale(UUID userId) {
        if (userId == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Optional<OffsetDateTime> lastSynced = externalRecordRepo.findMaxSyncedAtByUserId(userId);
                boolean shouldSync = lastSynced.isEmpty() ||
                        Duration.between(lastSynced.get(), OffsetDateTime.now()).toHours() >= 1;

                if (shouldSync) {
                    log.info("Background sync triggered for userId={} (last sync: {})", userId, lastSynced.orElse(null));
                    syncUser(userId, "ON_OPEN");
                }
            } catch (Exception e) {
                log.warn("Background on-open sync error for userId={}: {}", userId, e.getMessage());
            }
        });
    }

    /**
     * Cán bộ can thiệp sửa tay một trường của bản ghi ngoài
     */
    @Transactional
    public ManualOverride applyManualOverride(UUID recordId, String field, String value, String reason, UUID staffId) {
        ExternalRecord record = externalRecordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("External record not found: " + recordId));

        ManualOverride override = manualOverrideRepo.findByRecordIdAndField(recordId, field)
                .orElse(ManualOverride.builder()
                        .recordId(recordId)
                        .field(field)
                        .staffId(staffId)
                        .build());

        override.setValue(value);
        override.setReason(reason);
        override.setStaffId(staffId);
        override = manualOverrideRepo.save(override);

        // Cập nhật giá trị vào payload của external_records
        try {
            JsonNode root = objectMapper.readTree(record.getPayload());
            if (root.isObject()) {
                ObjectNode obj = (ObjectNode) root;
                try {
                    double numVal = Double.parseDouble(value);
                    obj.put(field, numVal);
                } catch (NumberFormatException ignored) {
                    obj.put(field, value);
                }
                record.setPayload(objectMapper.writeValueAsString(obj));
                record.setSyncedAt(OffsetDateTime.now());
                externalRecordRepo.save(record);
            }
        } catch (Exception e) {
            log.error("Failed to update payload with override: {}", e.getMessage());
        }

        // Tự động tính toán lại điểm cho ứng viên này (HS-01)
        recomputeForUser(record.getUserId());

        return override;
    }

    private void recomputeForUser(UUID userId) {
        try {
            List<ExternalRecord> records = externalRecordRepo.findByUserId(userId);
            List<ExternalRecordDto> dtos = new ArrayList<>();
            for (ExternalRecord r : records) {
                JsonNode p = objectMapper.readTree(r.getPayload());
                if ("LMS".equals(r.getSource())) {
                    String subCode = p.path("subjectCode").asText("");
                    String subName = p.path("subjectName").asText(subCode);
                    double score = p.path("score").asDouble(0.0);
                    short score100 = (short) Math.round(score * 10);
                    dtos.add(new ExternalRecordDto("LMS", subCode, subName, score100, r.getSyncedAt()));
                } else if ("EXAM".equals(r.getSource())) {
                    String subCode = p.path("subjectCode").asText("");
                    String examCode = p.path("examCode").asText(subCode);
                    double score = p.path("score").asDouble(0.0);
                    dtos.add(new ExternalRecordDto("EXAM", subCode.isEmpty() ? examCode : subCode, examCode, (short) Math.round(score), r.getSyncedAt()));
                } else if ("PM".equals(r.getSource())) {
                    String batchId = p.path("batchId").asText("IOC");
                    String batchName = p.path("batchName").asText(batchId);
                    double finalScore = p.path("finalScore").asDouble(0.0);
                    dtos.add(new ExternalRecordDto("PM", batchId, batchName, (short) Math.round(finalScore), r.getSyncedAt()));
                }
            }
            if (!dtos.isEmpty()) {
                skillRecomputeService.recomputeSkills(userId, dtos);
            }
        } catch (Exception e) {
            log.warn("Error recomputing skills after override for user {}: {}", userId, e.getMessage());
        }
    }

    private int processLmsResults(UUID userId, String studentId, JsonNode lmsResults, List<ExternalRecordDto> dtos) throws JsonProcessingException {
        if (lmsResults == null || !lmsResults.isArray()) return 0;
        int count = 0;
        for (JsonNode item : lmsResults) {
            String subjectCode = item.path("subjectCode").asText();
            if (subjectCode == null || subjectCode.isBlank()) continue;

            String sourceKey = "LMS_" + studentId + "_" + subjectCode;
            ExternalRecord record = externalRecordRepo.findBySourceAndSourceKey("LMS", sourceKey)
                    .orElse(ExternalRecord.builder()
                            .userId(userId)
                            .source("LMS")
                            .sourceKey(sourceKey)
                            .build());

            // Merge with manual overrides if exists
            ObjectNode payloadNode = item.isObject() ? ((ObjectNode) item).deepCopy() : objectMapper.createObjectNode();
            if (record.getId() != null) {
                List<ManualOverride> overrides = manualOverrideRepo.findByRecordId(record.getId());
                for (ManualOverride ov : overrides) {
                    try {
                        double d = Double.parseDouble(ov.getValue());
                        payloadNode.put(ov.getField(), d);
                    } catch (NumberFormatException ignored) {
                        payloadNode.put(ov.getField(), ov.getValue());
                    }
                }
            }

            record.setPayload(objectMapper.writeValueAsString(payloadNode));
            record.setSyncedAt(OffsetDateTime.now());
            record = externalRecordRepo.save(record);
            count++;

            double effectiveScore = payloadNode.path("score").asDouble(0.0);
            short score100 = (short) Math.min(100, Math.max(0, Math.round(effectiveScore * 10)));
            String title = payloadNode.path("subjectName").asText(subjectCode);
            dtos.add(new ExternalRecordDto("LMS", subjectCode, title, score100, record.getSyncedAt()));
        }
        return count;
    }

    private int processExamResults(UUID userId, String studentId, JsonNode examResults, List<ExternalRecordDto> dtos) throws JsonProcessingException {
        if (examResults == null || !examResults.isArray()) return 0;
        int count = 0;
        for (JsonNode item : examResults) {
            String examCode = item.path("examCode").asText("");
            String subjectCode = item.path("subjectCode").asText("");
            String keyIdentifier = !examCode.isEmpty() ? examCode : subjectCode;
            if (keyIdentifier.isBlank()) continue;

            String sourceKey = "EXAM_" + studentId + "_" + examCode + "_" + subjectCode;
            ExternalRecord record = externalRecordRepo.findBySourceAndSourceKey("EXAM", sourceKey)
                    .orElse(ExternalRecord.builder()
                            .userId(userId)
                            .source("EXAM")
                            .sourceKey(sourceKey)
                            .build());

            ObjectNode payloadNode = item.isObject() ? ((ObjectNode) item).deepCopy() : objectMapper.createObjectNode();
            if (record.getId() != null) {
                List<ManualOverride> overrides = manualOverrideRepo.findByRecordId(record.getId());
                for (ManualOverride ov : overrides) {
                    try {
                        double d = Double.parseDouble(ov.getValue());
                        payloadNode.put(ov.getField(), d);
                    } catch (NumberFormatException ignored) {
                        payloadNode.put(ov.getField(), ov.getValue());
                    }
                }
            }

            record.setPayload(objectMapper.writeValueAsString(payloadNode));
            record.setSyncedAt(OffsetDateTime.now());
            record = externalRecordRepo.save(record);
            count++;

            double effectiveScore = payloadNode.path("score").asDouble(0.0);
            short score = (short) Math.min(100, Math.max(0, Math.round(effectiveScore)));
            String mappedCode = !subjectCode.isEmpty() ? subjectCode : examCode;
            dtos.add(new ExternalRecordDto("EXAM", mappedCode, examCode, score, record.getSyncedAt()));
        }
        return count;
    }

    private int processPmInternships(UUID userId, String studentId, JsonNode pmInternships, List<ExternalRecordDto> dtos) throws JsonProcessingException {
        if (pmInternships == null || !pmInternships.isArray()) return 0;
        int count = 0;
        for (JsonNode item : pmInternships) {
            String batchId = item.path("batchId").asText("IOC");
            String sourceKey = "PM_" + studentId + "_" + batchId;

            ExternalRecord record = externalRecordRepo.findBySourceAndSourceKey("PM", sourceKey)
                    .orElse(ExternalRecord.builder()
                            .userId(userId)
                            .source("PM")
                            .sourceKey(sourceKey)
                            .build());

            ObjectNode payloadNode = item.isObject() ? ((ObjectNode) item).deepCopy() : objectMapper.createObjectNode();
            if (record.getId() != null) {
                List<ManualOverride> overrides = manualOverrideRepo.findByRecordId(record.getId());
                for (ManualOverride ov : overrides) {
                    try {
                        double d = Double.parseDouble(ov.getValue());
                        payloadNode.put(ov.getField(), d);
                    } catch (NumberFormatException ignored) {
                        payloadNode.put(ov.getField(), ov.getValue());
                    }
                }
            }

            record.setPayload(objectMapper.writeValueAsString(payloadNode));
            record.setSyncedAt(OffsetDateTime.now());
            record = externalRecordRepo.save(record);
            count++;

            double finalScore = payloadNode.path("finalScore").asDouble(0.0);
            short score = (short) Math.min(100, Math.max(0, Math.round(finalScore)));
            String batchName = payloadNode.path("batchName").asText(batchId);
            dtos.add(new ExternalRecordDto("PM", batchId, batchName, score, record.getSyncedAt()));
        }
        return count;
    }
}

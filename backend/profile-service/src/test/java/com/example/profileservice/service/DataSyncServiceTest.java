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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSyncServiceTest {

    @Mock
    private ExternalMockClient mockClient;

    @Mock
    private ExternalRecordRepository externalRecordRepo;

    @Mock
    private ManualOverrideRepository manualOverrideRepo;

    @Mock
    private SyncRunRepository syncRunRepo;

    @Mock
    private CandidateProfileRepository candidateProfileRepo;

    @Mock
    private SkillRecomputeService skillRecomputeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DataSyncService dataSyncService;

    private final UUID userId = UUID.fromString("ed854c07-f3eb-48f1-a186-4e7135f54645");

    @BeforeEach
    void setUp() {
        dataSyncService = new DataSyncService(
                mockClient,
                externalRecordRepo,
                manualOverrideRepo,
                syncRunRepo,
                candidateProfileRepo,
                skillRecomputeService,
                objectMapper
        );
    }

    @Test
    @DisplayName("AC1 & AC4: Đồng bộ thành công, upsert bản ghi, gọi recomputeSkills và ghi nhận sync_runs SUCCESS")
    void testSyncUser_Success() throws Exception {
        when(syncRunRepo.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun sr = invocation.getArgument(0);
            if (sr.getId() == null) sr.setId(UUID.randomUUID());
            return sr;
        });

        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .email("candidate1@rikkeijobs.test")
                .fullName("Nguyễn Văn An")
                .build();
        when(candidateProfileRepo.findById(userId)).thenReturn(Optional.of(profile));
        when(mockClient.resolveStudentId(userId, profile.getEmail())).thenReturn("std-001");

        // Mock LMS response with 1 subject
        String lmsJson = "[{\"subjectCode\":\"JV-01\",\"subjectName\":\"Java Core & OOP\",\"score\":8.8,\"attendanceRate\":100}]";
        JsonNode lmsNode = objectMapper.readTree(lmsJson);
        when(mockClient.getLmsResults("std-001")).thenReturn(lmsNode);
        when(mockClient.getExamResults("std-001")).thenReturn(objectMapper.createArrayNode());
        when(mockClient.getPmInternships("std-001")).thenReturn(objectMapper.createArrayNode());

        when(externalRecordRepo.findBySourceAndSourceKey(eq("LMS"), anyString())).thenReturn(Optional.empty());
        when(externalRecordRepo.save(any(ExternalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncRun result = dataSyncService.syncUser(userId, "MANUAL");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("MANUAL", result.getTrigger());
        assertNotNull(result.getFinishedAt());
        assertTrue(result.getCounts().contains("\"lms\":1"));
        assertTrue(result.getCounts().contains("\"total\":1"));

        // Verify SkillRecomputeService was called
        verify(skillRecomputeService, times(1)).recomputeSkills(eq(userId), anyList());
    }

    @Test
    @DisplayName("AC2: Tắt mock server -> sync_runs FAILED, lưu vết lỗi, không xóa dữ liệu cũ")
    void testSyncUser_MockFailure() {
        when(syncRunRepo.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun sr = invocation.getArgument(0);
            if (sr.getId() == null) sr.setId(UUID.randomUUID());
            return sr;
        });

        when(candidateProfileRepo.findById(userId)).thenReturn(Optional.empty());
        when(mockClient.resolveStudentId(userId, null)).thenReturn("std-001");
        when(mockClient.getLmsResults("std-001")).thenThrow(new ExternalMockClient.ExternalServiceException("Connection refused to mock server :9000", new RuntimeException()));

        SyncRun result = dataSyncService.syncUser(userId, "MANUAL");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Connection refused"));
        // Ensure no external records were deleted or cleared
        verify(externalRecordRepo, never()).deleteAll();
        verify(externalRecordRepo, never()).delete(any());
    }

    @Test
    @DisplayName("AC3: Cán bộ sửa tay 1 trường -> Đồng bộ lại giữ nguyên trường đó, chỉ update các trường khác")
    void testSyncUser_PreservesManualOverrides() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(syncRunRepo.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun sr = invocation.getArgument(0);
            if (sr.getId() == null) sr.setId(UUID.randomUUID());
            return sr;
        });

        when(candidateProfileRepo.findById(userId)).thenReturn(Optional.empty());
        when(mockClient.resolveStudentId(userId, null)).thenReturn("std-001");

        // Mock incoming new score from LMS is 8.0, but staff overridden score = 9.5
        String lmsJson = "[{\"subjectCode\":\"JV-01\",\"subjectName\":\"Java Core & OOP\",\"score\":8.0,\"attendanceRate\":100,\"homeworkRate\":95}]";
        JsonNode lmsNode = objectMapper.readTree(lmsJson);
        when(mockClient.getLmsResults("std-001")).thenReturn(lmsNode);
        when(mockClient.getExamResults("std-001")).thenReturn(objectMapper.createArrayNode());
        when(mockClient.getPmInternships("std-001")).thenReturn(objectMapper.createArrayNode());

        // Existing record in DB
        ExternalRecord existingRecord = ExternalRecord.builder()
                .id(recordId)
                .userId(userId)
                .source("LMS")
                .sourceKey("LMS_std-001_JV-01")
                .payload("{\"subjectCode\":\"JV-01\",\"score\":9.5}")
                .build();
        when(externalRecordRepo.findBySourceAndSourceKey("LMS", "LMS_std-001_JV-01")).thenReturn(Optional.of(existingRecord));

        // Existing manual override for field 'score' = '9.5'
        ManualOverride override = ManualOverride.builder()
                .recordId(recordId)
                .field("score")
                .value("9.5")
                .reason("Phúc khảo điểm thi")
                .staffId(UUID.randomUUID())
                .build();
        when(manualOverrideRepo.findByRecordId(recordId)).thenReturn(List.of(override));
        when(externalRecordRepo.save(any(ExternalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncRun result = dataSyncService.syncUser(userId, "MANUAL");

        assertEquals("SUCCESS", result.getStatus());
        // Verify that the saved record has the overridden score 9.5 and fresh attendanceRate 100
        verify(externalRecordRepo).save(argThat(saved -> {
            try {
                JsonNode p = objectMapper.readTree(saved.getPayload());
                return p.path("score").asDouble() == 9.5 && p.path("attendanceRate").asInt() == 100;
            } catch (Exception e) {
                return false;
            }
        }));
    }
}

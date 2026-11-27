package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.common.api.PageData;
import com.example.profileservice.dto.*;
import com.example.profileservice.entity.ExternalRecord;
import com.example.profileservice.entity.ManualOverride;
import com.example.profileservice.entity.SyncRun;
import com.example.profileservice.repository.ExternalRecordRepository;
import com.example.profileservice.repository.ManualOverrideRepository;
import com.example.profileservice.repository.SyncRunRepository;
import com.example.profileservice.service.DataSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffSyncController {

    private final DataSyncService dataSyncService;
    private final SyncRunRepository syncRunRepo;
    private final ExternalRecordRepository externalRecordRepo;
    private final ManualOverrideRepository manualOverrideRepo;
    private final ObjectMapper objectMapper;

    /**
     * Kích hoạt đồng bộ lại cho một ứng viên cụ thể
     */
    @PostMapping("/sync/{userId}")
    public ResponseEntity<ApiResponse<SyncRunResponseDto>> syncCandidate(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerStaffId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        UUID staffId = resolveStaffId(headerStaffId);
        String role = resolveRole(headerRole);
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<SyncRunResponseDto>>) denied;
        }

        SyncRun run = dataSyncService.syncUser(userId, "MANUAL");
        SyncRunResponseDto dto = toSyncRunDto(run);
        if ("FAILED".equals(run.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Đồng bộ thất bại: " + run.getError(), dto));
        }
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ dữ liệu thành công", dto));
    }

    /**
     * Kích hoạt chạy thủ công Job đêm (Nightly Job) cho toàn bộ ứng viên
     */
    @PostMapping("/sync/trigger-nightly")
    public ResponseEntity<ApiResponse<SyncRunResponseDto>> triggerNightlySync(
            @RequestHeader(value = "X-User-Id", required = false) UUID headerStaffId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        UUID staffId = resolveStaffId(headerStaffId);
        String role = resolveRole(headerRole);
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<SyncRunResponseDto>>) denied;
        }

        SyncRun run = dataSyncService.syncAllCandidates("NIGHTLY");
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt chạy Job đêm thành công", toSyncRunDto(run)));
    }

    /**
     * Lấy lịch sử các lần chạy đồng bộ
     */
    @GetMapping("/sync-runs")
    public ResponseEntity<ApiResponse<PageData<SyncRunResponseDto>>> getSyncRuns(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerStaffId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        UUID staffId = resolveStaffId(headerStaffId);
        String role = resolveRole(headerRole);
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<PageData<SyncRunResponseDto>>>) denied;
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 10 : size);
        Page<SyncRun> runPage;

        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim().toUpperCase() : null;

        if (cleanStatus != null && userId != null) {
            runPage = syncRunRepo.findByStatusAndUserIdOrderByStartedAtDesc(cleanStatus, userId, pageable);
        } else if (cleanStatus != null) {
            runPage = syncRunRepo.findByStatusOrderByStartedAtDesc(cleanStatus, pageable);
        } else if (userId != null) {
            runPage = syncRunRepo.findByUserIdOrderByStartedAtDesc(userId, pageable);
        } else {
            runPage = syncRunRepo.findAllByOrderByStartedAtDesc(pageable);
        }

        PageData<SyncRunResponseDto> responseData = PageData.from(runPage.map(this::toSyncRunDto));
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đồng bộ thành công", responseData));
    }

    /**
     * Lấy danh sách bản ghi nguồn ngoài của ứng viên kèm thông tin sửa tay
     */
    @GetMapping("/external-records")
    public ResponseEntity<ApiResponse<List<ExternalRecordDetailDto>>> getExternalRecords(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "source", required = false) String source,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerStaffId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        UUID staffId = resolveStaffId(headerStaffId);
        String role = resolveRole(headerRole);
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<List<ExternalRecordDetailDto>>>) denied;
        }

        List<ExternalRecord> records;
        if (userId != null && source != null && !source.isBlank() && !"ALL".equalsIgnoreCase(source)) {
            records = externalRecordRepo.findByUserIdAndSource(userId, source.trim().toUpperCase());
        } else if (userId != null) {
            records = externalRecordRepo.findByUserId(userId);
        } else {
            records = externalRecordRepo.findAll();
        }

        List<UUID> recordIds = records.stream().map(ExternalRecord::getId).filter(Objects::nonNull).toList();
        Map<UUID, List<ManualOverride>> overridesByRecord = recordIds.isEmpty() ? Map.of() :
                manualOverrideRepo.findByRecordIdIn(recordIds).stream()
                        .collect(Collectors.groupingBy(ManualOverride::getRecordId));

        List<ExternalRecordDetailDto> dtoList = records.stream().map(r -> {
            List<ManualOverride> overrides = overridesByRecord.getOrDefault(r.getId(), List.of());
            List<ManualOverrideDto> overrideDtos = overrides.stream()
                    .map(o -> new ManualOverrideDto(o.getId(), o.getRecordId(), o.getField(), o.getValue(), o.getReason(), o.getStaffId(), o.getCreatedAt()))
                    .toList();

            Object parsedPayload = null;
            try {
                parsedPayload = objectMapper.readValue(r.getPayload(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                parsedPayload = r.getPayload();
            }

            return ExternalRecordDetailDto.builder()
                    .id(r.getId())
                    .userId(r.getUserId())
                    .source(r.getSource())
                    .sourceKey(r.getSourceKey())
                    .payload(parsedPayload)
                    .syncedAt(r.getSyncedAt())
                    .isOverridden(!overrides.isEmpty())
                    .overrides(overrideDtos)
                    .build();
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bản ghi nguồn ngoài thành công", dtoList));
    }

    /**
     * Cán bộ can thiệp sửa tay một trường của bản ghi ngoài
     */
    @PutMapping("/external-records/{id}/override")
    public ResponseEntity<ApiResponse<ManualOverrideDto>> overrideField(
            @PathVariable UUID id,
            @Valid @RequestBody ManualOverrideRequestDto request,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerStaffId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        UUID staffId = resolveStaffId(headerStaffId);
        String role = resolveRole(headerRole);
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<ManualOverrideDto>>) denied;
        }

        ManualOverride override = dataSyncService.applyManualOverride(
                id, request.getField(), request.getValue(), request.getReason(), staffId);

        ManualOverrideDto dto = new ManualOverrideDto(
                override.getId(), override.getRecordId(), override.getField(),
                override.getValue(), override.getReason(), override.getStaffId(), override.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success("Sửa tay bản ghi thành công", dto));
    }

    private SyncRunResponseDto toSyncRunDto(SyncRun run) {
        Object countsObj = null;
        if (run.getCounts() != null && !run.getCounts().isBlank()) {
            try {
                countsObj = objectMapper.readValue(run.getCounts(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                countsObj = run.getCounts();
            }
        }
        return SyncRunResponseDto.builder()
                .id(run.getId())
                .trigger(run.getTrigger())
                .userId(run.getUserId())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt() != null ? run.getFinishedAt() : run.getCompletedAt())
                .error(run.getError())
                .counts(countsObj)
                .build();
    }

    private UUID resolveStaffId(UUID headerStaffId) {
        if (headerStaffId != null) return headerStaffId;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String resolveRole(String headerRole) {
        if (headerRole != null && !headerRole.isBlank()) return headerRole;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }

    private ResponseEntity<?> requireStaff(UUID staffId, String role) {
        if (staffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Thiếu thông tin xác thực nhân viên"));
        }
        String normalized = role == null ? "" : role.trim().toUpperCase().replace("ROLE_", "");
        if (!"STAFF".equals(normalized) && !"ADMIN".equals(normalized)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN, "Chức năng chỉ dành cho Cán bộ vận hành (STAFF / ADMIN)"));
        }
        return null;
    }
}

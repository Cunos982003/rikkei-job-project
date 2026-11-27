package com.example.jobservice.job.controller;

import com.example.jobservice.common.ApiResponse;
import com.example.jobservice.common.PageResult;
import com.example.jobservice.job.dto.*;
import com.example.jobservice.job.service.JobService;
import com.example.jobservice.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Tin tuyển dụng", description = "API quản lý tin tuyển dụng (JD) theo vị trí và kỹ năng yêu cầu")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping({ "/api/v1/jobs", "/jobs" })
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Tạo mới tin tuyển dụng (Bản nháp)", description = "HR tạo bản nháp tin tuyển dụng cho công ty của mình.")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới tin tuyển dụng thành công", response));
    }

    @PutMapping({ "/api/v1/jobs/{id}", "/jobs/{id}" })
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Cập nhật thông tin tin tuyển dụng", description = "HR chỉnh sửa thông tin JD. Nếu JD đang APPROVED sẽ chuyển sang PENDING.")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateJobRequest request) {
        JobResponse response = jobService.updateJob(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tin tuyển dụng thành công", response));
    }

    @PutMapping({ "/api/v1/jobs/{id}/skills", "/jobs/{id}/skills" })
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Cập nhật danh sách kỹ năng yêu cầu cho JD", description = "Thay thế toàn bộ kỹ năng của tin tuyển dụng.")
    public ResponseEntity<ApiResponse<JobResponse>> replaceSkills(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<@Valid JobSkillItem> skills) {
        JobResponse response = jobService.replaceSkills(id, principal, skills);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh sách kỹ năng thành công", response));
    }

    @PostMapping({ "/api/v1/jobs/{id}/submit", "/jobs/{id}/submit" })
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Gửi duyệt tin tuyển dụng", description = "Chuyển trạng thái tin từ DRAFT/REJECTED sang PENDING để chờ duyệt.")
    public ResponseEntity<ApiResponse<JobResponse>> submitJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.submitJob(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Gửi duyệt tin tuyển dụng thành công", response));
    }

    @PostMapping({ "/api/v1/jobs/{id}/close", "/jobs/{id}/close" })
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Đóng tin tuyển dụng", description = "Đóng tin tuyển dụng. Không áp dụng cho tin đã đóng.")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.closeJob(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Đóng tin tuyển dụng thành công", response));
    }

    @GetMapping({ "/api/v1/companies/me/jobs", "/companies/me/jobs" })
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Danh sách tin tuyển dụng của công ty tôi", description = "HR xem danh sách các tin tuyển dụng của công ty mình có phân trang và lọc trạng thái.")
    public ResponseEntity<ApiResponse<PageResult<JobListItemResponse>>> listMyJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageResult<JobListItemResponse> response = jobService.listMyJobs(principal, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tuyển dụng thành công", response));
    }

    @GetMapping({ "/api/v1/jobs/{id}", "/jobs/{id}" })
    @Operation(summary = "Xem chi tiết tin tuyển dụng", description = "HR xem tin công ty mình, STAFF/ADMIN xem mọi tin, ứng viên chỉ xem được tin APPROVED.")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.getJob(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết tin tuyển dụng thành công", response));
    }

    @GetMapping({ "/api/v1/staff/jobs", "/staff/jobs" })
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Danh sách tin tuyển dụng cho Cán bộ", description = "Cán bộ xem danh sách tin tuyển dụng chờ duyệt hoặc theo trạng thái.")
    public ResponseEntity<ApiResponse<PageResult<JobResponse>>> listStaffJobs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageResult<JobResponse> response = jobService.listStaffJobs(status, companyId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tuyển dụng thành công", response));
    }

    @PostMapping({ "/api/v1/staff/jobs/{id}/approve", "/staff/jobs/{id}/approve" })
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Phê duyệt tin tuyển dụng", description = "Cán bộ duyệt tin tuyển dụng PENDING sang APPROVED.")
    public ResponseEntity<ApiResponse<JobResponse>> approveJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.approveJob(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt tin tuyển dụng thành công", response));
    }

    @PostMapping({ "/api/v1/staff/jobs/{id}/reject", "/staff/jobs/{id}/reject" })
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Từ chối tin tuyển dụng", description = "Cán bộ từ chối tin tuyển dụng kèm lý do.")
    public ResponseEntity<ApiResponse<JobResponse>> rejectJob(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = body != null ? body.get("reason") : null;
        JobResponse response = jobService.rejectJob(id, reason, principal);
        return ResponseEntity.ok(ApiResponse.success("Từ chối tin tuyển dụng thành công", response));
    }
}

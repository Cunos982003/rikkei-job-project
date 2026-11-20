package com.example.jobservice.job.controller;

import com.example.jobservice.company.dto.PageResponse;
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

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Tạo mới tin tuyển dụng (Bản nháp)", description = "HR tạo bản nháp tin tuyển dụng cho công ty của mình.")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Cập nhật thông tin tin tuyển dụng", description = "HR chỉnh sửa thông tin JD. Nếu JD đang APPROVED sẽ chuyển sang PENDING.")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateJobRequest request) {
        JobResponse response = jobService.updateJob(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/jobs/{id}/skills")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Cập nhật danh sách kỹ năng yêu cầu cho JD", description = "Thay thế toàn bộ kỹ năng của tin tuyển dụng.")
    public ResponseEntity<JobResponse> replaceSkills(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<@Valid JobSkillItem> skills) {
        JobResponse response = jobService.replaceSkills(id, principal, skills);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jobs/{id}/submit")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Gửi duyệt tin tuyển dụng", description = "Chuyển trạng thái tin từ DRAFT/REJECTED sang PENDING để chờ duyệt.")
    public ResponseEntity<JobResponse> submitJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.submitJob(id, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jobs/{id}/close")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Đóng tin tuyển dụng", description = "Đóng tin tuyển dụng. Không áp dụng cho tin đã đóng.")
    public ResponseEntity<JobResponse> closeJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.closeJob(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies/me/jobs")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Danh sách tin tuyển dụng của công ty tôi", description = "HR xem danh sách các tin tuyển dụng của công ty mình có phân trang và lọc trạng thái.")
    public ResponseEntity<PageResponse<JobListItemResponse>> listMyJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageResponse<JobListItemResponse> response = jobService.listMyJobs(principal, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Xem chi tiết tin tuyển dụng", description = "HR xem tin công ty mình, STAFF/ADMIN xem mọi tin, ứng viên chỉ xem được tin APPROVED.")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobResponse response = jobService.getJob(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/staff/jobs")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Danh sách tin tuyển dụng cho Cán bộ", description = "Cán bộ xem danh sách tin tuyển dụng chờ duyệt hoặc theo trạng thái.")
    public ResponseEntity<PageResponse<JobResponse>> listStaffJobs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageResponse<JobResponse> response = jobService.listStaffJobs(status, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/jobs/{id}/approve")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Phê duyệt tin tuyển dụng", description = "Cán bộ duyệt tin tuyển dụng PENDING sang APPROVED.")
    public ResponseEntity<JobResponse> approveJob(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(jobService.approveJob(id));
    }

    @PostMapping("/staff/jobs/{id}/reject")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Từ chối tin tuyển dụng", description = "Cán bộ từ chối tin tuyển dụng kèm lý do.")
    public ResponseEntity<JobResponse> rejectJob(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(jobService.rejectJob(id, reason));
    }
}


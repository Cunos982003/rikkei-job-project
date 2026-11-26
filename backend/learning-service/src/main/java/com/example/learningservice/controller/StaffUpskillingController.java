package com.example.learningservice.controller;

import com.example.learningservice.model.dto.response.ApiResponse;
import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import com.example.learningservice.model.dto.response.staff.UpskillingSummaryResponse;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.ExcelExportService;
import com.example.learningservice.service.StaffUpskillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/staff/upskilling", "/staff/upskilling"})
@RequiredArgsConstructor
@Tag(name = "Staff Upskilling Tracking", description = "Cán bộ theo dõi đào tạo bổ sung theo lớp, khoá (DT-04)")
public class StaffUpskillingController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StaffUpskillingService staffUpskillingService;
    private final ExcelExportService excelExportService;
    private final UserContextHelper userContextHelper;

    @GetMapping
    @Operation(summary = "Lấy danh sách theo dõi đào tạo bổ sung theo lớp, khoá (DT-04)",
            description = "Hỗ trợ lọc theo mã lớp (classCode), tên khoá (courseName), trạng thái học tập (status), kỹ năng thiếu (skillId) và phân trang.")
    public ResponseEntity<ApiResponse<List<UpskillingCandidateDto>>> getUpskillingList(
            @Parameter(description = "Tên khoá đào tạo")
            @RequestParam(required = false) String courseName,
            @Parameter(description = "Mã lớp đào tạo")
            @RequestParam(required = false) String classCode,
            @Parameter(description = "Trạng thái học tập (IN_PROGRESS, COMPLETED, ENROLLED, FAILED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Mã kỹ năng đang thiếu (JAVA_CORE, SPRING_BOOT, REACT,...)")
            @RequestParam(required = false) String skillId,
            @Parameter(description = "Số trang (0-indexed)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Kích thước trang")
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        userContextHelper.requireStaffOrAdmin(headerUserId, headerUserRole, authHeader);

        log.info("Cán bộ tra cứu danh sách đào tạo bổ sung: classCode={}, courseName={}, status={}, skillId={}, page={}, size={}",
                classCode, courseName, status, skillId, page, size);

        List<UpskillingCandidateDto> items = staffUpskillingService.getUpskillingList(courseName, classCode, status, skillId, page, size);
        long totalElements = staffUpskillingService.countUpskillingList(courseName, classCode, status, skillId);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(totalElements));
        headers.add("X-Page-Number", String.valueOf(page));
        headers.add("X-Page-Size", String.valueOf(size));
        headers.add("X-Total-Pages", String.valueOf(totalPages));

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("Lấy danh sách đào tạo bổ sung thành công", items));
    }

    @GetMapping("/summary")
    @Operation(summary = "Lấy thống kê tổng hợp đào tạo bổ sung (DT-04)",
            description = "Trả về số ứng viên đang học, đã hoàn thành, và Top 5 kỹ năng bị thiếu nhiều nhất.")
    public ResponseEntity<ApiResponse<UpskillingSummaryResponse>> getUpskillingSummary(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        userContextHelper.requireStaffOrAdmin(headerUserId, headerUserRole, authHeader);

        log.info("Cán bộ tra cứu báo cáo tổng hợp đào tạo bổ sung");

        UpskillingSummaryResponse summary = staffUpskillingService.getUpskillingSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo tổng hợp đào tạo bổ sung thành công", summary));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất báo cáo danh sách đào tạo bổ sung ra file Excel (.xlsx) (DT-04)",
            description = "Xuất toàn bộ danh sách ứng viên khớp với bộ lọc hiện tại (classCode, courseName, status, skillId) ra tệp .xlsx.")
    public ResponseEntity<byte[]> exportUpskillingExcel(
            @Parameter(description = "Tên khoá đào tạo")
            @RequestParam(required = false) String courseName,
            @Parameter(description = "Mã lớp đào tạo")
            @RequestParam(required = false) String classCode,
            @Parameter(description = "Trạng thái học tập (IN_PROGRESS, COMPLETED, ENROLLED, FAILED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Mã kỹ năng đang thiếu")
            @RequestParam(required = false) String skillId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        userContextHelper.requireStaffOrAdmin(headerUserId, headerUserRole, authHeader);

        log.info("Cán bộ xuất báo cáo Excel đào tạo bổ sung: classCode={}, courseName={}, status={}, skillId={}",
                classCode, courseName, status, skillId);

        List<UpskillingCandidateDto> items = staffUpskillingService.getUpskillingList(courseName, classCode, status, skillId, null, null);
        byte[] excelBytes = excelExportService.exportUpskillingCandidates(items);

        String filename = "bao-cao-dao-tao-bo-sung-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }
}

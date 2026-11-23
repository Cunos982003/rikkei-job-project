package com.example.jobservice.company.controller;

import com.example.jobservice.common.ApiResponse;
import com.example.jobservice.common.PageResult;
import com.example.jobservice.company.dto.*;
import com.example.jobservice.company.service.CompanyService;
import com.example.jobservice.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/staff/companies")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Staff Companies", description = "API quản lý doanh nghiệp và cấp tài khoản HR dành cho cán bộ")
public class StaffCompanyController {

    private final CompanyService companyService;

    public StaffCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @Operation(summary = "Tạo doanh nghiệp mới", description = "Cán bộ tạo hồ sơ doanh nghiệp. Mã số thuế phải gồm 10 hoặc 13 chữ số và duy nhất.")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String createdBy = principal != null ? principal.getEmail() : "staff";
        CompanyResponse response = companyService.createCompany(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới doanh nghiệp thành công", response));
    }

    @PostMapping("/{id}/hr-accounts")
    @Operation(summary = "Cấp tài khoản HR cho doanh nghiệp", description = "Tạo user role HR, mật khẩu tạm ngẫu nhiên, must_change_password=true, gửi email thông tin đăng nhập.")
    public ResponseEntity<ApiResponse<HrAccountResponse>> createHrAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateHrAccountRequest request) {
        HrAccountResponse response = companyService.createHrAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Cấp tài khoản HR thành công", response));

    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên logo doanh nghiệp", description = "Cán bộ tải lên logo cho doanh nghiệp.")
    public ResponseEntity<ApiResponse<LogoUploadResponse>> uploadLogo(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file) {
        LogoUploadResponse response = companyService.uploadCompanyLogoById(id, file);
        return ResponseEntity.ok(ApiResponse.success("Tải lên logo doanh nghiệp thành công", response));
    }

    @GetMapping
    @Operation(summary = "Danh sách doanh nghiệp", description = "Tìm kiếm theo tên/MST, lọc theo trạng thái, hỗ trợ phân trang.")
    public ResponseEntity<ApiResponse<PageResult<CompanyListItemResponse>>> getCompanies(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResult<CompanyListItemResponse> response = companyService.getStaffCompanies(q, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách doanh nghiệp thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết doanh nghiệp theo ID", description = "Endpoint trả về chi tiết doanh nghiệp và danh sách HR trực thuộc.")
    public ResponseEntity<ApiResponse<StaffCompanyDetailResponse>> getCompanyById(@PathVariable("id") UUID id) {
        StaffCompanyDetailResponse response = companyService.getStaffCompanyDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết doanh nghiệp thành công", response));
    }
}

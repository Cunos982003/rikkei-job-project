package com.example.jobservice.company.controller;

import com.example.jobservice.common.ApiResponse;
import com.example.jobservice.company.dto.CompanyResponse;
import com.example.jobservice.company.dto.LogoUploadResponse;
import com.example.jobservice.company.dto.UpdateCompanyRequest;
import com.example.jobservice.company.service.CompanyService;
import com.example.jobservice.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/companies")
@Tag(name = "Companies", description = "API quản lý hồ sơ doanh nghiệp dành cho HR")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Xem hồ sơ công ty của tôi", description = "HR lấy thông tin chi tiết về công ty mình thuộc về.")
    public ResponseEntity<ApiResponse<CompanyResponse>> getMyCompany(@AuthenticationPrincipal UserPrincipal principal) {
        CompanyResponse response = companyService.getCompanyForCurrentHr(principal);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ doanh nghiệp thành công", response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Chỉnh sửa hồ sơ công ty của tôi", description = "HR tự cập nhật hồ sơ công ty. Không được sửa mã số thuế và trạng thái.")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateMyCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompanyForCurrentHr(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hồ sơ doanh nghiệp thành công", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Chỉnh sửa hồ sơ công ty theo ID", description = "HR chỉ sửa được công ty mình; nếu gọi sửa công ty khác sẽ trả về 404.")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompanyById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompanyById(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin doanh nghiệp thành công", response));
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Tải lên logo công ty", description = "HR tải lên ảnh logo. Định dạng JPG hoặc PNG, dung lượng tối đa 2MB.")
    public ResponseEntity<ApiResponse<LogoUploadResponse>> uploadLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        LogoUploadResponse response = companyService.uploadLogo(principal, file);
        return ResponseEntity.ok(ApiResponse.success("Tải lên logo doanh nghiệp thành công", response));
    }
}

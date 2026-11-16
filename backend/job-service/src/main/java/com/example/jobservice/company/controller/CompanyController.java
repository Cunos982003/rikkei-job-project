package com.example.jobservice.company.controller;

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
@Tag(name = "Companies", description = "API quan ly ho so doanh nghiep danh cho HR")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Xem ho so cong ty cua toi", description = "HR lay thong tin chi tiet ve cong ty minh thuoc ve.")
    public ResponseEntity<CompanyResponse> getMyCompany(@AuthenticationPrincipal UserPrincipal principal) {
        CompanyResponse response = companyService.getCompanyForCurrentHr(principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Chinh sua ho so cong ty cua toi", description = "HR tu cap nhat ho so cong ty. Khong duoc sua ma so thue va trang thai.")
    public ResponseEntity<CompanyResponse> updateMyCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompanyForCurrentHr(principal, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Chinh sua ho so cong ty theo ID", description = "HR chi sua duoc cong ty minh; neu goi sua cong ty khac se tra ve 404.")
    public ResponseEntity<CompanyResponse> updateCompanyById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompanyById(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Tai len logo cong ty", description = "HR tai len anh logo. Dinh dang JPG hoac PNG, dung luong toi da 2MB.")
    public ResponseEntity<LogoUploadResponse> uploadLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        LogoUploadResponse response = companyService.uploadLogo(principal, file);
        return ResponseEntity.ok(response);
    }
}

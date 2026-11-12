package com.example.jobservice.company.controller;

import com.example.jobservice.company.dto.*;
import com.example.jobservice.company.service.CompanyService;
import com.example.jobservice.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/companies")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Staff Companies", description = "API quan ly doanh nghiep va cap tai khoan HR danh cho can bo")
public class StaffCompanyController {

    private final CompanyService companyService;

    public StaffCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @Operation(summary = "Tao doanh nghiep moi", description = "Can bo tao ho so doanh nghiep. Ma so thue phai gom 10 hoac 13 chu so va duy nhat.")
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String createdBy = principal != null ? principal.getEmail() : "staff";
        CompanyResponse response = companyService.createCompany(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/hr-accounts")
    @Operation(summary = "Cap tai khoan HR cho doanh nghiep", description = "Tao user role HR, mat khau tam ngau nhien, must_change_password=true, gui email thong tin dang nhap.")
    public ResponseEntity<HrAccountResponse> createHrAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateHrAccountRequest request) {
        HrAccountResponse response = companyService.createHrAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết doanh nghiệp theo ID", description = "Endpoint được giữ cho STAFF/ADMIN để xem thông tin doanh nghiệp.")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable("id") UUID id) {
        CompanyResponse response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }
}

package com.example.jobservice.company.controller;

import com.example.jobservice.company.dto.LockHrAccountRequest;
import com.example.jobservice.company.dto.StaffHrAccountResponse;
import com.example.jobservice.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/hr-accounts")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Staff HR Accounts", description = "API quản lý trạng thái khóa/mở khóa tài khoản HR")
public class StaffHrAccountController {

    private final CompanyService companyService;

    public StaffHrAccountController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "Khóa tài khoản HR", description = "STAFF khóa tài khoản HR và ghi nhận lý do.")
    public ResponseEntity<StaffHrAccountResponse> lockHrAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody LockHrAccountRequest request) {
        StaffHrAccountResponse response = companyService.lockHrAccount(id, request.reason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Mở khóa tài khoản HR", description = "STAFF mở khóa tài khoản HR.")
    public ResponseEntity<StaffHrAccountResponse> unlockHrAccount(@PathVariable("id") UUID id) {
        StaffHrAccountResponse response = companyService.unlockHrAccount(id);
        return ResponseEntity.ok(response);
    }
}


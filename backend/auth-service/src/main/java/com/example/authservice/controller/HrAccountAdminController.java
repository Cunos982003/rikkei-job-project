package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.HrAccountAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/internal/hr-accounts")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class HrAccountAdminController {

    private final HrAccountAdminService hrAccountAdminService;

    public HrAccountAdminController(HrAccountAdminService hrAccountAdminService) {
        this.hrAccountAdminService = hrAccountAdminService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HrAccountAdminResponse>> createHrAccount(
        @Valid @RequestBody CreateHrAccountAdminRequest request
    ) {
        HrAccountAdminResponse response = hrAccountAdminService.createHrAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Tạo tài khoản HR thành công", response));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<HrAccountAdminResponse>> lockHrAccount(
        @PathVariable UUID id,
        @Valid @RequestBody LockHrAccountAdminRequest request
    ) {
        HrAccountAdminResponse response = hrAccountAdminService.lockHrAccount(id, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Khóa tài khoản HR thành công", response));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<HrAccountAdminResponse>> unlockHrAccount(
        @PathVariable UUID id
    ) {
        HrAccountAdminResponse response = hrAccountAdminService.unlockHrAccount(id);
        return ResponseEntity.ok(ApiResponse.ok("Mở khóa tài khoản HR thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HrAccountAdminResponse>>> listHrAccounts(
        @RequestParam("companyId") UUID companyId
    ) {
        List<HrAccountAdminResponse> response = hrAccountAdminService.listHrAccounts(companyId);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách tài khoản HR", response));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countHrAccounts(
        @RequestParam(value = "companyIds", required = false) List<UUID> companyIds
    ) {
        Map<String, Long> response = hrAccountAdminService.countHrAccounts(
            companyIds != null ? companyIds : Collections.emptyList()
        );
        return ResponseEntity.ok(ApiResponse.ok("Số lượng tài khoản HR", response));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse err = new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getStatusCode().toString(),
            ex.getReason() != null ? ex.getReason() : ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(err);
    }
}


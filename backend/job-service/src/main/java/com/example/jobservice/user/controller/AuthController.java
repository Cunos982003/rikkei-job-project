package com.example.jobservice.user.controller;

import com.example.jobservice.security.UserPrincipal;
import com.example.jobservice.user.dto.ChangePasswordRequest;
import com.example.jobservice.user.dto.LoginRequest;
import com.example.jobservice.user.dto.LoginResponse;
import com.example.jobservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Dang nhap va doi mat khau nguoi dung")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Dang nhap nguoi dung", description = "Kiem tra email va mat khau, tra ve JWT va trang thai must_change_password")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Doi mat khau nguoi dung", description = "Doi mat khau lan dau hoac doi mat khau dinh ky")

    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal, request);
        return ResponseEntity.ok(Map.of("message", "Doi mat khau thanh cong"));
    }
}

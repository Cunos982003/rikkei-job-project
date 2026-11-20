package com.example.authservice.controller;

import com.example.authservice.dto.ApiResponse;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.FirstTimePasswordRequest;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.MessageResponse;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.dto.SsoRequest;
import com.example.authservice.dto.UserDto;
import com.example.authservice.entity.User;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.HrAuthService;
import com.example.authservice.service.SsoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
public class AuthController {

    private final SsoService ssoService;
    private final HrAuthService hrAuthService;
    private final UserRepository userRepository;

    public AuthController(
        SsoService ssoService,
        HrAuthService hrAuthService,
        UserRepository userRepository
    ) {
        this.ssoService = ssoService;
        this.hrAuthService = hrAuthService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sso")
    public ResponseEntity<ApiResponse<AuthResponse>> ssoLogin(@Valid @RequestBody SsoRequest request) {
        String ticket = request.ticket() == null ? null : request.ticket().trim();
        AuthResponse response = ssoService.authenticateWithTicket(ticket);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập SSO thành công", response));
    }

    @PostMapping({"/login", "/hr/login"})
    public ResponseEntity<ApiResponse<AuthResponse>> hrLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = hrAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = hrAuthService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu đặt lại mật khẩu đã được xử lý", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        MessageResponse response = hrAuthService.changePassword(user.getEmail(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", response));
    }

    @PostMapping("/first-time-password")
    public ResponseEntity<ApiResponse<MessageResponse>> firstTimePassword(@Valid @RequestBody FirstTimePasswordRequest request, Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        MessageResponse response = hrAuthService.firstTimePassword(user.getId(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Đặt mật khẩu lần đầu thành công", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = hrAuthService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", UserDto.from(user)));
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Authentication required");
        }
        String principal = authentication.getName().trim();
        try {
            UUID userId = UUID.fromString(principal);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return user;
            }
        } catch (IllegalArgumentException ignored) {
            // principal may be an email instead of UUID
        }

        return userRepository.findByEmail(principal)
            .orElseThrow(() -> new SsoException(ErrorCode.INVALID_CREDENTIALS, "User not found"));
    }
}

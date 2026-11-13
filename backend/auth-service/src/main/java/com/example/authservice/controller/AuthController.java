package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.CreateHrUserRequest;
import com.example.authservice.dto.DemoAccountDto;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/demo-accounts")
    public ResponseEntity<List<DemoAccountDto>> getDemoAccounts() {
        return ResponseEntity.ok(List.of(
            new DemoAccountDto("CANDIDATE", "candidate1@rikkeijobs.test", "Ứng viên (Nguyễn Văn An)"),
            new DemoAccountDto("HR", "hr1@rikkeijobs.test", "Doanh nghiệp HR (Rikkeisoft)"),
            new DemoAccountDto("MENTOR", "mentor1@rikkeijobs.test", "Mentor (Phạm Minh Đức)"),
            new DemoAccountDto("STAFF", "staff1@rikkeijobs.test", "Cán bộ tuyển dụng"),
            new DemoAccountDto("ADMIN", "admin1@rikkeijobs.test", "Quản trị hệ thống")
        ));
    }

    @PostMapping("/sso")
    public ResponseEntity<AuthResponse> ssoLogin(@Valid @RequestBody SsoRequest request) {
        String ticket = request.ticket() == null ? null : request.ticket().trim();
        AuthResponse response = ssoService.authenticateWithTicket(ticket);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/login", "/hr/login"})
    public ResponseEntity<AuthResponse> hrLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = hrAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = hrAuthService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Authentication required");
        }
        String principal = authentication.getName().trim();
        User user = null;
        try {
            UUID userId = UUID.fromString(principal);
            user = userRepository.findById(userId).orElse(null);
        } catch (IllegalArgumentException ignored) {
        }
        if (user == null) {
            user = userRepository.findByEmail(principal.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new SsoException(ErrorCode.INVALID_CREDENTIALS, "User not found"));
        }
        return ResponseEntity.ok(hrAuthService.changePassword(user.getEmail(), request.currentPassword(), request.newPassword()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = hrAuthService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = parseUserId(authentication.getName());
        return userRepository.findById(userId)
            .map(UserDto::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return userRepository.findById(id)
            .map(UserDto::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam("email") String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(false);
        }
        boolean exists = userRepository.existsByEmail(email.trim().toLowerCase(Locale.ROOT));
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/users/hr")
    public ResponseEntity<UserDto> createHrUser(@Valid @RequestBody CreateHrUserRequest request) {
        String email = request.email();
        String fullName = request.fullName();
        UUID companyId = request.companyId();
        String temporaryPassword = request.temporaryPassword();
        UserDto created = hrAuthService.createHrUser(email, fullName, companyId, temporaryPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "Authentication principal is not a valid user id");
        }
    }
}

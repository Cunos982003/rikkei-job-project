package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.MessageResponse;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.dto.SsoRequest;
import com.example.authservice.dto.UserDto;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.HrAuthService;
import com.example.authservice.service.SsoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
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
    public ResponseEntity<AuthResponse> ssoLogin(@Valid @RequestBody SsoRequest request) {
        AuthResponse response = ssoService.authenticateWithTicket(request.ticket());
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

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = hrAuthService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
            .map(UserDto::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

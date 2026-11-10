package com.example.jobservice.user.service;

import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.security.JwtTokenProvider;
import com.example.jobservice.security.UserPrincipal;
import com.example.jobservice.user.domain.User;
import com.example.jobservice.user.domain.UserRepository;
import com.example.jobservice.user.dto.ChangePasswordRequest;
import com.example.jobservice.user.dto.LoginRequest;
import com.example.jobservice.user.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Sai email hoac mat khau"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Sai email hoac mat khau");
        }

        UserPrincipal principal = UserPrincipal.of(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCompanyId()
        );

        String token = tokenProvider.generateToken(principal);
        String message = user.isMustChangePassword()
                ? "Dang nhap thanh cong. Ban bat buoc phai doi mat khau lan dau."
                : "Dang nhap thanh cong";

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCompanyId(),
                user.isMustChangePassword(),
                message
        );
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mat khau hien tai khong chinh xac");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}

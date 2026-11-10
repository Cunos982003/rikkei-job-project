package com.example.authservice.service;

import com.example.authservice.client.lms.LmsClient;
import com.example.authservice.client.lms.LmsUserResponse;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.UserDto;
import com.example.authservice.entity.UsedSsoTicket;
import com.example.authservice.entity.User;
import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import com.example.authservice.repository.UsedSsoTicketRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.security.RoleMapper;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);
    private static final long TICKET_EXPIRATION_SECONDS = 60L;

    private final LmsClient lmsClient;
    private final UsedSsoTicketRepository usedSsoTicketRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public SsoService(
        LmsClient lmsClient,
        UsedSsoTicketRepository usedSsoTicketRepository,
        UserRepository userRepository,
        RoleMapper roleMapper,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.lmsClient = lmsClient;
        this.usedSsoTicketRepository = usedSsoTicketRepository;
        this.userRepository = userRepository;
        this.roleMapper = roleMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse authenticateWithTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new SsoException(ErrorCode.TICKET_INVALID, "Ticket is required and cannot be blank");
        }

        Instant now = Instant.now();

        if (usedSsoTicketRepository.existsByTicket(ticket)) {
            log.warn("Rejected replayed SSO ticket: ticket={} ", ticket);
            throw new SsoException(ErrorCode.TICKET_ALREADY_USED, "SSO ticket has already been used");
        }

        LmsUserResponse lmsResponse;
        try {
            lmsResponse = lmsClient.verifyTicket(ticket);
        } catch (SsoException ex) {
            log.warn("Mock LMS verification failed for ticket {}: {}", ticket, ex.getMessage());
            throw ex;
        }

        if (lmsResponse == null || lmsResponse.lmsUserId() == null || lmsResponse.lmsUserId().isBlank()) {
            throw new SsoException(ErrorCode.TICKET_INVALID, "Mock LMS returned empty or invalid user data");
        }

        Instant issuedAt = lmsResponse.getIssuedAtInstant();
        if (issuedAt == null) {
            throw new SsoException(ErrorCode.TICKET_INVALID, "Mock LMS response is missing a valid issuedAt timestamp");
        }

        long ageInSeconds = Duration.between(issuedAt, now).getSeconds();
        if (ageInSeconds > TICKET_EXPIRATION_SECONDS) {
            log.warn("SSO ticket expired: issuedAt={}, ageSeconds={}", issuedAt, ageInSeconds);
            throw new SsoException(ErrorCode.TICKET_EXPIRED, "SSO ticket has expired (valid for 60 seconds)");
        }

        String role = roleMapper.mapAndValidateLmsRole(lmsResponse.role());

        try {
            usedSsoTicketRepository.saveAndFlush(new UsedSsoTicket(ticket, now));
        } catch (DataIntegrityViolationException ex) {
            log.warn("SSO ticket replay prevented for ticket={} in concurrent request", ticket);
            throw new SsoException(ErrorCode.TICKET_ALREADY_USED, "SSO ticket has already been used");
        }

        User user = findOrCreateUser(lmsResponse, role, now);
        String accessToken = jwtTokenProvider.generateToken(user);

        return new AuthResponse(
            accessToken,
            "Bearer",
            jwtTokenProvider.getExpirationSeconds(),
            UserDto.from(user)
        );
    }

    private User findOrCreateUser(LmsUserResponse lmsResponse, String role, Instant now) {
        return userRepository.findByLmsUserId(lmsResponse.lmsUserId())
            .map(existingUser -> {
                if (existingUser.getEmail() == null || existingUser.getEmail().isBlank()) {
                    existingUser.setEmail(lmsResponse.email());
                }
                if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                    existingUser.setFullName(lmsResponse.fullName());
                }
                existingUser.setEmail(lmsResponse.email());
                existingUser.setFullName(lmsResponse.fullName());
                existingUser.setRole(role);
                existingUser.setUpdatedAt(now);
                existingUser.setStatus(com.example.authservice.entity.UserStatus.ACTIVE);
                existingUser.setLockedUntil(null);
                existingUser.setFailedLoginCount(0);
                if (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isBlank()) {
                    existingUser.setMustChangePassword(false);
                }
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> userRepository.findByEmail(lmsResponse.email())
                .map(existingEmailUser -> {
                    existingEmailUser.setLmsUserId(lmsResponse.lmsUserId());
                    existingEmailUser.setFullName(lmsResponse.fullName());
                    existingEmailUser.setRole(role);
                    existingEmailUser.setUpdatedAt(now);
                    existingEmailUser.setStatus(com.example.authservice.entity.UserStatus.ACTIVE);
                    existingEmailUser.setLockedUntil(null);
                    existingEmailUser.setFailedLoginCount(0);
                    if (existingEmailUser.getPasswordHash() == null || existingEmailUser.getPasswordHash().isBlank()) {
                        existingEmailUser.setMustChangePassword(false);
                    }
                    return userRepository.save(existingEmailUser);
                })
                .orElseGet(() -> {
                    User newUser = new User(
                        lmsResponse.lmsUserId(),
                        lmsResponse.email(),
                        lmsResponse.fullName(),
                        role,
                        null,
                        now,
                        now
                    );
                    newUser.setStatus(com.example.authservice.entity.UserStatus.ACTIVE);
                    newUser.setMustChangePassword(false);
                    newUser.setFailedLoginCount(0);
                    newUser.setLockedUntil(null);
                    return userRepository.save(newUser);
                }));
    }
}


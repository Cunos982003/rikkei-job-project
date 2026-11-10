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

        // 1. Check if ticket has already been used
        if (usedSsoTicketRepository.existsByTicket(ticket)) {
            throw new SsoException(ErrorCode.TICKET_ALREADY_USED, "SSO ticket has already been used");
        }

        // 2. Verify ticket with Mock LMS
        LmsUserResponse lmsResponse = lmsClient.verifyTicket(ticket);
        if (lmsResponse == null || lmsResponse.lmsUserId() == null || lmsResponse.lmsUserId().isBlank()) {
            throw new SsoException(ErrorCode.TICKET_INVALID, "Mock LMS returned empty or invalid user data");
        }

        // 3. Check ticket expiration (max 60 seconds)
        Instant issuedAt = lmsResponse.getIssuedAtInstant();
        if (issuedAt == null) {
            throw new SsoException(ErrorCode.TICKET_INVALID, "Mock LMS response is missing a valid issuedAt timestamp");
        }

        Instant now = Instant.now();
        long ageInSeconds = Duration.between(issuedAt, now).getSeconds();
        if (ageInSeconds > TICKET_EXPIRATION_SECONDS) {
            log.warn("SSO ticket expired: issued at {}, age {}s", issuedAt, ageInSeconds);
            throw new SsoException(ErrorCode.TICKET_EXPIRED, "SSO ticket has expired (valid for 60 seconds)");
        }

        // 4. Validate and map role from LMS
        String role = roleMapper.mapAndValidateLmsRole(lmsResponse.role());

        // 5. Persist ticket to database to prevent replay attack and handle race conditions
        try {
            usedSsoTicketRepository.saveAndFlush(new UsedSsoTicket(ticket, now));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition / duplicate ticket prevented for SSO ticket attempt");
            throw new SsoException(ErrorCode.TICKET_ALREADY_USED, "SSO ticket has already been used");
        }

        // 6. Find or create user
        User user = findOrCreateUser(lmsResponse, role, now);

        // 7. Issue JWT
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
                existingUser.setEmail(lmsResponse.email());
                existingUser.setFullName(lmsResponse.fullName());
                existingUser.setRole(role);
                existingUser.setUpdatedAt(now);
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> {
                // If user doesn't exist by lmsUserId, check email uniqueness
                return userRepository.findByEmail(lmsResponse.email())
                    .map(existingEmailUser -> {
                        existingEmailUser.setLmsUserId(lmsResponse.lmsUserId());
                        existingEmailUser.setFullName(lmsResponse.fullName());
                        existingEmailUser.setRole(role);
                        existingEmailUser.setUpdatedAt(now);
                        return userRepository.save(existingEmailUser);
                    })
                    .orElseGet(() -> {
                        User newUser = new User(
                            lmsResponse.lmsUserId(),
                            lmsResponse.email(),
                            lmsResponse.fullName(),
                            role,
                            now,
                            now
                        );
                        return userRepository.save(newUser);
                    });
            });
    }
}


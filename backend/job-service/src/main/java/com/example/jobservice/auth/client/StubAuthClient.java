package com.example.jobservice.auth.client;

import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local", "test"})
@ConditionalOnProperty(name = "auth.client.stub", havingValue = "true", matchIfMissing = false)
public class StubAuthClient implements AuthClient {

    private final Map<UUID, AuthHrAccount> accounts = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public StubAuthClient() {
    }

    public void initDefaultAccounts(UUID company1Id, UUID company2Id) {
        if (company1Id != null) {
            seedAccount(new AuthHrAccount(
                    UUID.fromString("44444444-4444-4444-4444-444444444441"),
                    "hr1@rikkeijobs.test",
                    "HR Account One",
                    company1Id,
                    "ACTIVE",
                    null,
                    null,
                    false,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    null,
                    true
            ));
        }
        if (company2Id != null) {
            seedAccount(new AuthHrAccount(
                    UUID.fromString("44444444-4444-4444-4444-444444444442"),
                    "hr2@rikkeijobs.test",
                    "HR Account Two",
                    company2Id,
                    "ACTIVE",
                    null,
                    null,
                    false,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    null,
                    true
            ));
        }
    }

    public void seedAccount(AuthHrAccount account) {
        accounts.put(account.id(), account);
    }

    public void clear() {
        accounts.clear();
    }

    public Optional<AuthHrAccount> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String lower = email.trim().toLowerCase(Locale.ROOT);
        return accounts.values().stream()
                .filter(a -> lower.equalsIgnoreCase(a.email()))
                .findFirst();
    }

    public Optional<AuthHrAccount> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public Map<UUID, AuthHrAccount> getAllAccounts() {
        return Collections.unmodifiableMap(accounts);
    }

    @Override
    public AuthHrAccount createHrAccount(String email, String fullName, UUID companyId) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email không được để trống");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean exists = accounts.values().stream()
                .anyMatch(a -> normalizedEmail.equalsIgnoreCase(a.email()));
        if (exists) {
            throw new ConflictException("Email đã tồn tại");
        }

        UUID id = UUID.randomUUID();
        String tempPassword = generateTempPassword();
        AuthHrAccount account = new AuthHrAccount(
                id,
                normalizedEmail,
                fullName != null ? fullName.trim() : "",
                companyId,
                "ACTIVE",
                null,
                null,
                true,
                OffsetDateTime.now(ZoneOffset.UTC),
                tempPassword,
                true
        );
        accounts.put(id, account);
        return account;
    }

    @Override
    public AuthHrAccount lockHrAccount(UUID userId, String reason) {
        AuthHrAccount existing = accounts.get(userId);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản HR có id: " + userId);
        }
        if ("LOCKED".equalsIgnoreCase(existing.status())) {
            throw new BadRequestException("Tài khoản HR đã bị khóa");
        }
        AuthHrAccount updated = new AuthHrAccount(
                existing.id(),
                existing.email(),
                existing.fullName(),
                existing.companyId(),
                "LOCKED",
                reason,
                OffsetDateTime.now(ZoneOffset.UTC),
                existing.mustChangePassword(),
                existing.createdAt(),
                existing.temporaryPassword(),
                existing.emailSent()
        );
        accounts.put(userId, updated);
        return updated;
    }

    @Override
    public AuthHrAccount unlockHrAccount(UUID userId) {
        AuthHrAccount existing = accounts.get(userId);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản HR có id: " + userId);
        }
        if (!"LOCKED".equalsIgnoreCase(existing.status())) {
            throw new BadRequestException("Tài khoản HR chưa bị khóa");
        }
        AuthHrAccount updated = new AuthHrAccount(
                existing.id(),
                existing.email(),
                existing.fullName(),
                existing.companyId(),
                "ACTIVE",
                null,
                null,
                existing.mustChangePassword(),
                existing.createdAt(),
                existing.temporaryPassword(),
                existing.emailSent()
        );
        accounts.put(userId, updated);
        return updated;
    }

    @Override
    public List<AuthHrAccount> listHrAccounts(UUID companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        return accounts.values().stream()
                .filter(a -> companyId.equals(a.companyId()))
                .sorted(Comparator.comparing(AuthHrAccount::createdAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public Map<UUID, Long> countHrAccounts(Collection<UUID> companyIds) {
        Map<UUID, Long> result = new HashMap<>();
        if (companyIds == null || companyIds.isEmpty()) {
            return result;
        }
        for (UUID cid : companyIds) {
            long count = accounts.values().stream()
                    .filter(a -> cid.equals(a.companyId()))
                    .count();
            result.put(cid, count);
        }
        return result;
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String special = "@#$!";
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        String all = upper + lower + digits + special;
        for (int i = 0; i < 6; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        return sb.toString();
    }
}



package com.example.profileservice.badge;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminBadgeService {

    private final JdbcTemplate jdbcTemplate;

    public record CandidateItem(
            UUID userId,
            String fullName,
            String email,
            String classCode
    ) {}

    public record BadgeRuleItem(
            UUID id,
            String code,
            String skillName,
            Double minScore,
            boolean needInternshipTask
    ) {}

    public record BadgeHistoryItem(
            UUID id,
            UUID userId,
            UUID badgeRuleId,
            String badgeCode,
            String skillName,
            String grantedType,
            OffsetDateTime grantedAt,
            OffsetDateTime revokedAt,
            String grantReason,
            String revokeReason,
            UUID grantAdminId,
            UUID revokeAdminId,
            boolean active
    ) {}

    public record BadgeActionResult(
            UUID id,
            UUID userId,
            UUID badgeRuleId,
            String action,
            OffsetDateTime actionAt
    ) {}

    public List<CandidateItem> searchCandidates(String keyword) {
        String q = keyword == null ? "" : keyword.trim();

        return jdbcTemplate.query("""
                SELECT user_id, full_name, email, class_code
                FROM candidate_profiles
                WHERE (? = ''
                    OR LOWER(COALESCE(full_name, '')) LIKE LOWER(?)
                    OR LOWER(COALESCE(email, '')) LIKE LOWER(?))
                ORDER BY full_name NULLS LAST
                LIMIT 100
                """,
                (rs, rowNum) -> new CandidateItem(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("class_code")
                ),
                q, "%" + q + "%", "%" + q + "%"
        );
    }

    public List<BadgeRuleItem> getBadgeRules() {
        return jdbcTemplate.query("""
                SELECT br.id,
                       br.code,
                       s.name AS skill_name,
                       br.min_score,
                       br.need_internship_task
                FROM badge_rules br
                LEFT JOIN skills s ON s.id = br.skill_id
                ORDER BY br.code
                """,
                (rs, rowNum) -> new BadgeRuleItem(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("skill_name"),
                        rs.getBigDecimal("min_score") == null
                                ? null
                                : rs.getBigDecimal("min_score").doubleValue(),
                        rs.getBoolean("need_internship_task")
                )
        );
    }

    public List<BadgeHistoryItem> getCandidateBadgeHistory(UUID userId) {
        ensureCandidateExists(userId);

        return jdbcTemplate.query("""
                SELECT pb.id,
                       pb.user_id,
                       pb.badge_rule_id,
                       br.code AS badge_code,
                       s.name AS skill_name,
                       pb.granted_type,
                       pb.granted_at,
                       pb.revoked_at,
                       pb.grant_reason,
                       pb.revoke_reason,
                       pb.grant_admin_id,
                       pb.revoke_admin_id
                FROM profile_badges pb
                JOIN badge_rules br ON br.id = pb.badge_rule_id
                LEFT JOIN skills s ON s.id = br.skill_id
                WHERE pb.user_id = ?
                ORDER BY pb.granted_at DESC
                """,
                (rs, rowNum) -> mapHistory(rs),
                userId
        );
    }

    @Transactional
    public BadgeActionResult grantBadge(
            UUID userId,
            UUID badgeRuleId,
            String reason,
            UUID adminId
    ) {
        validateReason(reason);
        ensureCandidateExists(userId);
        ensureBadgeRuleExists(badgeRuleId);

        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM profile_badges
                WHERE user_id = ?
                  AND badge_rule_id = ?
                  AND revoked_at IS NULL
                """, Integer.class, userId, badgeRuleId);

        if (activeCount != null && activeCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ứng viên đã sở hữu huy hiệu này"
            );
        }

        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO profile_badges (
                    id,
                    user_id,
                    badge_rule_id,
                    granted_type,
                    granted_at,
                    revoked_at,
                    grant_reason,
                    grant_admin_id
                )
                VALUES (?, ?, ?, 'MANUAL', ?, NULL, ?, ?)
                """,
                id,
                userId,
                badgeRuleId,
                now,
                reason.trim(),
                adminId
        );

        return new BadgeActionResult(
                id, userId, badgeRuleId, "GRANTED", now
        );
    }

    @Transactional
    public BadgeActionResult revokeBadge(
            UUID profileBadgeId,
            String reason,
            UUID adminId
    ) {
        validateReason(reason);

        List<BadgeActionResult> rows = jdbcTemplate.query("""
                SELECT id, user_id, badge_rule_id
                FROM profile_badges
                WHERE id = ?
                  AND revoked_at IS NULL
                FOR UPDATE
                """,
                (rs, rowNum) -> new BadgeActionResult(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("badge_rule_id", UUID.class),
                        "REVOKED",
                        null
                ),
                profileBadgeId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy huy hiệu đang có hiệu lực"
            );
        }

        BadgeActionResult current = rows.get(0);
        OffsetDateTime now = OffsetDateTime.now();

        jdbcTemplate.update("""
                UPDATE profile_badges
                SET revoked_at = ?,
                    revoke_reason = ?,
                    revoke_admin_id = ?
                WHERE id = ?
                  AND revoked_at IS NULL
                """,
                now,
                reason.trim(),
                adminId,
                profileBadgeId
        );

        return new BadgeActionResult(
                current.id(),
                current.userId(),
                current.badgeRuleId(),
                "REVOKED",
                now
        );
    }

    private BadgeHistoryItem mapHistory(ResultSet rs) throws SQLException {
        OffsetDateTime revokedAt =
                rs.getObject("revoked_at", OffsetDateTime.class);

        return new BadgeHistoryItem(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("badge_rule_id", UUID.class),
                rs.getString("badge_code"),
                rs.getString("skill_name"),
                rs.getString("granted_type"),
                rs.getObject("granted_at", OffsetDateTime.class),
                revokedAt,
                rs.getString("grant_reason"),
                rs.getString("revoke_reason"),
                rs.getObject("grant_admin_id", UUID.class),
                rs.getObject("revoke_admin_id", UUID.class),
                revokedAt == null
        );
    }

    private void ensureCandidateExists(UUID userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM candidate_profiles
                WHERE user_id = ?
                """, Integer.class, userId);

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy hồ sơ ứng viên"
            );
        }
    }

    private void ensureBadgeRuleExists(UUID badgeRuleId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM badge_rules
                WHERE id = ?
                """, Integer.class, badgeRuleId);

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy quy tắc huy hiệu"
            );
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do là bắt buộc"
            );
        }

        if (reason.trim().length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do phải có ít nhất 5 ký tự"
            );
        }

        if (reason.trim().length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do không được vượt quá 1000 ký tự"
            );
        }
    }
}

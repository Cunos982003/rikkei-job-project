
package com.example.authservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role = UserRole.CANDIDATE;

    @Column(name = "lms_user_id", unique = true, length = 100)
    private String lmsUserId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "locked_reason", length = 500)
    private String lockedReason;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.failedLoginCount = 0;
        this.mustChangePassword = false;
    }

    public User(
            String lmsUserId,
            String email,
            String fullName,
            UserRole role,
            Instant createdAt,
            Instant updatedAt) {
        this(lmsUserId, email, fullName, role, null, createdAt, updatedAt);
    }

    public User(
            String lmsUserId,
            String email,
            String fullName,
            String role,
            Instant createdAt,
            Instant updatedAt) {
        this(lmsUserId, email, fullName, UserRole.fromString(role), null, createdAt, updatedAt);
    }

    public User(
            String lmsUserId,
            String email,
            String fullName,
            UserRole role,
            String passwordHash,
            Instant createdAt,
            Instant updatedAt) {
        this.lmsUserId = lmsUserId;
        this.email = email;
        this.fullName = fullName;
        this.role = role != null ? role : UserRole.CANDIDATE;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.failedLoginCount = 0;
        this.mustChangePassword = false;
        this.status = UserStatus.ACTIVE;
    }

    public User(
            String lmsUserId,
            String email,
            String fullName,
            String role,
            String passwordHash,
            Instant createdAt,
            Instant updatedAt) {
        this(lmsUserId, email, fullName, UserRole.fromString(role), passwordHash, createdAt, updatedAt);
    }

    @PrePersist
    @PreUpdate
    public void syncTimestamps() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.failedLoginCount < 0) {
            this.failedLoginCount = 0;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (this.mustChangePassword) {
            this.mustChangePassword = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role != null ? role : UserRole.CANDIDATE;
    }

    public void setRole(String role) {
        this.role = UserRole.fromString(role);
    }

    public String getLmsUserId() {
        return lmsUserId;
    }

    public void setLmsUserId(String lmsUserId) {
        this.lmsUserId = lmsUserId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLockedReason() {
        return lockedReason;
    }

    public void setLockedReason(String lockedReason) {
        this.lockedReason = lockedReason;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAccountLocked() {
        return status == UserStatus.LOCKED
                || (lockedUntil != null && lockedUntil.isAfter(Instant.now()));
    }
}

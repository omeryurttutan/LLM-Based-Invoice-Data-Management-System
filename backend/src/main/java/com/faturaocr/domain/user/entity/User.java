package com.faturaocr.domain.user.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.domain.audit.annotation.AuditExclude;
import com.faturaocr.domain.audit.annotation.AuditMask;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User domain entity.
 */
@Getter
public class User extends BaseEntity {

    private UUID companyId;
    @AuditMask(AuditMask.MaskType.EMAIL)
    private Email email;
    @AuditExclude
    private String passwordHash;
    @AuditMask(AuditMask.MaskType.PARTIAL)
    private String fullName;
    @AuditMask(AuditMask.MaskType.PHONE)
    private String phone;
    private String avatarUrl;
    private Role role;
    private boolean isActive;
    private boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;
    @AuditExclude
    private int failedLoginAttempts;
    @AuditExclude
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordChangedAt;

    // Private constructor for builder pattern
    private User() {
        super();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Domain methods
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        markAsUpdated();
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        markAsUpdated();
    }

    public void lock(int durationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        markAsUpdated();
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        resetFailedLoginAttempts();
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        markAsUpdated();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
        markAsUpdated();
    }

    public void deactivate() {
        this.isActive = false;
        markAsUpdated();
    }

    public void activate() {
        this.isActive = true;
        markAsUpdated();
    }

    public void updateDetails(String fullName, String phone, String avatarUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        markAsUpdated();
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
        markAsUpdated();
    }

    public void updateForAnonymization(String newEmail, String newPasswordHash) {
        this.email = Email.of(newEmail);
        this.passwordHash = newPasswordHash;
        markAsUpdated();
    }

    // Getters and helper methods for specific needs like validation/logic can
    // remain if needed,
    // but standard getters are covered by @Getter.
    // However, getEmailValue() is a convenience method, so we keep it.

    public String getEmailValue() {
        return email.getValue();
    }

    // Builder
    public static class Builder {
        private final User user = new User();

        public Builder id(UUID id) {
            user.id = id;
            return this;
        }

        public Builder companyId(UUID companyId) {
            user.companyId = companyId;
            return this;
        }

        public Builder email(String email) {
            user.email = Email.of(email);
            return this;
        }

        public Builder email(Email email) {
            user.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            user.passwordHash = passwordHash;
            return this;
        }

        public Builder fullName(String fullName) {
            user.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            user.phone = phone;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            user.avatarUrl = avatarUrl;
            return this;
        }

        public Builder role(Role role) {
            user.role = role;
            return this;
        }

        public Builder isActive(boolean isActive) {
            user.isActive = isActive;
            return this;
        }

        public Builder emailVerified(boolean emailVerified) {
            user.emailVerified = emailVerified;
            return this;
        }

        public Builder failedLoginAttempts(int attempts) {
            user.failedLoginAttempts = attempts;
            return this;
        }

        public Builder lockedUntil(LocalDateTime lockedUntil) {
            user.lockedUntil = lockedUntil;
            return this;
        }

        public Builder lastLoginAt(LocalDateTime lastLoginAt) {
            user.lastLoginAt = lastLoginAt;
            return this;
        }

        public User build() {
            // Validation
            if (user.companyId == null) {
                throw new IllegalStateException("Company ID is required");
            }
            if (user.email == null) {
                throw new IllegalStateException("Email is required");
            }
            if (user.passwordHash == null) {
                throw new IllegalStateException("Password hash is required");
            }
            if (user.fullName == null || user.fullName.isBlank()) {
                throw new IllegalStateException("Full name is required");
            }
            if (user.role == null) {
                user.role = Role.ACCOUNTANT; // Default role
            }
            return user;
        }
    }
}

package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import com.faturaocr.infrastructure.security.encryption.EncryptedStringConverter;
import jakarta.persistence.Convert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for users table.
 */
@Entity
@Table(name = "users")
@lombok.Getter
@lombok.Setter
public class UserJpaEntity extends BaseJpaEntity {

    @Column(name = "default_company_id")
    private UUID companyId;

    @jakarta.persistence.OneToMany(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<UserCompanyAccessJpaEntity> companyAccesses = new java.util.ArrayList<>();

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone")
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleJpa role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    public enum RoleJpa {
        ADMIN, MANAGER, ACCOUNTANT, INTERN
    }
}

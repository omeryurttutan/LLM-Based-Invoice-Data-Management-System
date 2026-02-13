package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for User persistence.
 * Separate from domain entity to keep domain layer pure.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserJpaEntity extends BaseJpaEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserJpaRole role;

    /**
     * JPA-specific role enum (mirrors domain UserRole).
     */
    public enum UserJpaRole {
        ADMIN,
        USER,
        VIEWER
    }
}

package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * JPA entity for User persistence.
 * Separate from domain entity to keep domain layer pure.
 */
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

    protected UserJpaEntity() {
        super();
    }

    // Getters and setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public UserJpaRole getRole() {
        return role;
    }

    public void setRole(UserJpaRole role) {
        this.role = role;
    }

    /**
     * JPA-specific role enum (mirrors domain UserRole).
     */
    public enum UserJpaRole {
        ADMIN,
        USER,
        VIEWER
    }
}

package com.faturaocr.domain.user.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.UserRole;

import java.util.Objects;
import java.util.UUID;

/**
 * User domain entity representing a system user.
 */
public class User extends BaseEntity {

    private String firstName;
    private String lastName;
    private Email email;
    private String passwordHash;
    private UserRole role;

    protected User() {
        super();
    }

    private User(String firstName, String lastName, Email email, String passwordHash, UserRole role) {
        super();
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.role = Objects.requireNonNull(role, "Role cannot be null");
    }

    public static User create(String firstName, String lastName, Email email,
            String passwordHash, UserRole role) {
        return new User(firstName, lastName, email, passwordHash, role);
    }

    public static User reconstitute(
            UUID id,
            String firstName,
            String lastName,
            Email email,
            String passwordHash,
            UserRole role,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            boolean isDeleted,
            java.time.LocalDateTime deletedAt) {
        User user = new User();
        user.id = id;
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        user.isDeleted = isDeleted;
        user.deletedAt = deletedAt;
        return user;
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        markAsUpdated();
    }

    public void changeEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail);
        markAsUpdated();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
        markAsUpdated();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }
}

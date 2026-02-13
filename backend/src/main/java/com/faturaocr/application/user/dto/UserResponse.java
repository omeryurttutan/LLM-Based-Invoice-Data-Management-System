package com.faturaocr.application.user.dto;

import java.util.UUID;

/**
 * Response DTO for user data.
 */
public class UserResponse {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String role;

    public UserResponse(UUID id, String firstName, String lastName, String email, String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}

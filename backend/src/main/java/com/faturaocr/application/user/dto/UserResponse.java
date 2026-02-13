package com.faturaocr.application.user.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response DTO for user data.
 */
@Getter
@AllArgsConstructor
public class UserResponse {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String role;
}

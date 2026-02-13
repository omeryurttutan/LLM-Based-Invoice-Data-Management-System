package com.faturaocr.interfaces.rest.user.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API response DTO for user data.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserApiResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}

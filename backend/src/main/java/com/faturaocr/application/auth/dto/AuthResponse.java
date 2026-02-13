package com.faturaocr.application.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication response with tokens.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    public record UserInfo(
            UUID id,
            String email,
            String fullName,
            String role,
            UUID companyId) {
    }
}

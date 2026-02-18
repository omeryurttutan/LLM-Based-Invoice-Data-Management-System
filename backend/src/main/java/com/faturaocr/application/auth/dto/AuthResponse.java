package com.faturaocr.application.auth.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Authentication response with tokens.
 */
@Schema(description = "Authentication response containing JWT tokens and user info")
public record AuthResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,

        @Schema(description = "Refresh token for obtaining new access tokens", example = "d29a...82j1") String refreshToken,

        @Schema(description = "Token type", example = "Bearer") String tokenType,

        @Schema(description = "Expiration time in milliseconds", example = "3600000") long expiresIn,

        @Schema(description = "Authenticated user information") UserInfo user) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    @Schema(description = "User details")
    public record UserInfo(
            @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000") UUID id,

            @Schema(description = "User email address", example = "user@example.com") String email,

            @Schema(description = "User full name", example = "Ahmet Yılmaz") String fullName,

            @Schema(description = "User role", example = "MANAGER") String role,

            @Schema(description = "Company ID", example = "987e6543-e21b-56d3-a456-426614174000") UUID companyId) {
    }
}

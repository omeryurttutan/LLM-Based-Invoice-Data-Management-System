package com.faturaocr.application.user.dto;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "User details response")
public class UserResponse {
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Full name", example = "Ahmet Yılmaz")
    private String fullName;

    @Schema(description = "Phone number", example = "+905551234567")
    private String phone;

    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User role", example = "ACCOUNTANT")
    private Role role;

    @Schema(description = "Company ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID companyId;

    @Schema(description = "Company name", example = "Acme Corp")
    private String companyName; // Will be populated if company loaded

    @Schema(description = "Is user active", example = "true")
    private boolean isActive;

    @Schema(description = "Is email verified", example = "true")
    private boolean emailVerified;

    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public static UserResponse fromDomain(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmailValue())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .companyId(user.getCompanyId())
                .isActive(user.isActive())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

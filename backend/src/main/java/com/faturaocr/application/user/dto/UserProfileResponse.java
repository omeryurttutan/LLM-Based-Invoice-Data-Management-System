package com.faturaocr.application.user.dto;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private UUID companyId;
    private String companyName; // Populated if available
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean emailVerified;
    private LocalDateTime passwordChangedAt;

    public static UserProfileResponse fromDomain(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmailValue())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .companyId(user.getCompanyId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .emailVerified(user.isEmailVerified())
                .passwordChangedAt(user.getPasswordChangedAt())
                .build();
    }
}

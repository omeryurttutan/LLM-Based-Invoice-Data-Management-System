package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to change user password")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password", example = "OldPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Schema(description = "New password (min 8 chars)", example = "NewPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirmation of new password", example = "NewPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    public ChangePasswordCommand toCommand() {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        return ChangePasswordCommand.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();
    }
}

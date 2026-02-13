package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
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

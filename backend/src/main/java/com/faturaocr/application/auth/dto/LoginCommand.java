package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Command for user login.
 */
@Schema(description = "Login credentials")
public record LoginCommand(

                @Schema(description = "User email address", example = "admin@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

                @Schema(description = "User password", example = "P@ssw0rd123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password") @NotBlank(message = "Password is required") String password

) implements Command {
}

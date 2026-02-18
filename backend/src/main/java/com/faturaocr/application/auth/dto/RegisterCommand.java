package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Command for user registration.
 */
@Schema(description = "User registration request")
public record RegisterCommand(

                @Schema(description = "Company ID to join", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "Company ID is required") java.util.UUID companyId,

                @Schema(description = "User email address", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255, message = "Email must be less than 255 characters") String email,

                @Schema(description = "Strong password", example = "StrongP@ssw0rd!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password") @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "Password must contain at least one digit, one lowercase, "
                                +
                                "one uppercase, and one special character") String password,

                @Schema(description = "User full name", example = "Mehmet Demir", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

                @Schema(description = "User phone number", example = "+905551234567") @Size(max = 20, message = "Phone must be less than 20 characters") String phone

) implements Command {
}

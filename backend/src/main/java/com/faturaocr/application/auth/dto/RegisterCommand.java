package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Command for user registration.
 */
public record RegisterCommand(

                @NotNull(message = "Company ID is required") java.util.UUID companyId,

                @NotBlank(message = "Email is required")
                @Email(message = "Invalid email format")
                @Size(max = 255, message = "Email must be less than 255 characters") String email,

                @NotBlank(message = "Password is required")
                @Size(min = 8, max = 100, 
                        message = "Password must be between 8 and 100 characters")
                @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", 
                        message = "Password must contain at least one digit, one lowercase, " + 
                        "one uppercase, and one special character")
                        String password,

                @NotBlank(message = "Full name is required")
                @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

                @Size(max = 20, message = "Phone must be less than 20 characters") String phone

) implements Command {
}

package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for user login.
 */
public record LoginCommand(

        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

        @NotBlank(message = "Password is required") String password

) implements Command {
}

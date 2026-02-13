package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for token refresh.
 */
public record RefreshTokenCommand(

        @NotBlank(message = "Refresh token is required") String refreshToken

) implements Command {
}

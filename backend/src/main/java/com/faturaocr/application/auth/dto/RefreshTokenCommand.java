package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Command for token refresh.
 */
@Schema(description = "Token refresh request")
public record RefreshTokenCommand(

                @Schema(description = "Valid refresh token", example = "d29a...82j1", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Refresh token is required") String refreshToken

) implements Command {
}

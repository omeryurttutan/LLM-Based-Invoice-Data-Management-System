package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.UpdateUserCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to update another user")
public class UpdateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    @Schema(description = "Full name", example = "Ahmet Yılmaz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must act as valid phone format")
    @Schema(description = "Phone number", example = "+905551234567")
    private String phone;

    public UpdateUserCommand toCommand() {
        return UpdateUserCommand.builder()
                .fullName(fullName)
                .phone(phone)
                .build();
    }
}

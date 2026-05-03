package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.ChangeRoleCommand;
import com.faturaocr.domain.user.valueobject.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to change user role")
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    @Schema(description = "New role", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    public ChangeRoleCommand toCommand() {
        return ChangeRoleCommand.builder()
                .role(role)
                .build();
    }
}

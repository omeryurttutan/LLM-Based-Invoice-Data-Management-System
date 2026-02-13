package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.ChangeRoleCommand;
import com.faturaocr.domain.user.valueobject.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    public ChangeRoleCommand toCommand() {
        return ChangeRoleCommand.builder()
                .role(role)
                .build();
    }
}

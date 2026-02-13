package com.faturaocr.application.user.dto;

import com.faturaocr.domain.user.valueobject.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeRoleCommand {
    private Role role;
}

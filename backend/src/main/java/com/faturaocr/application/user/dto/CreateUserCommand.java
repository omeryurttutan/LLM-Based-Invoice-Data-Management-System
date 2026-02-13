package com.faturaocr.application.user.dto;

import com.faturaocr.domain.user.valueobject.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserCommand {
    private String email;
    private String fullName;
    private String password;
    private String phone;
    private Role role;
}

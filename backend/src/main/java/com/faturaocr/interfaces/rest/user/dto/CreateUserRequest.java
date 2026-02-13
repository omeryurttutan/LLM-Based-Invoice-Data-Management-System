package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.domain.user.valueobject.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    // Note: Complex password validation logic can be added via custom annotation or
    // pattern
    // For now simple check
    private String password;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must act as valid phone format")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    public CreateUserCommand toCommand() {
        return CreateUserCommand.builder()
                .email(email)
                .fullName(fullName)
                .password(password)
                .phone(phone)
                .role(role)
                .build();
    }
}

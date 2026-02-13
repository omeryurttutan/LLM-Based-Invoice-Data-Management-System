package com.faturaocr.interfaces.rest.user.dto;

import com.faturaocr.application.user.dto.UpdateProfileCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must act as valid phone format")
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    public UpdateProfileCommand toCommand() {
        return UpdateProfileCommand.builder()
                .fullName(fullName)
                .phone(phone)
                .avatarUrl(avatarUrl)
                .build();
    }
}

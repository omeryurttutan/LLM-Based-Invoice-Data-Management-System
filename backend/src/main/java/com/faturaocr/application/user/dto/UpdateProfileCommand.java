package com.faturaocr.application.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileCommand {
    private String fullName;
    private String phone;
    private String avatarUrl;
}

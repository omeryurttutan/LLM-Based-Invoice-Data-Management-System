package com.faturaocr.application.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordCommand {
    private String currentPassword;
    private String newPassword;
}

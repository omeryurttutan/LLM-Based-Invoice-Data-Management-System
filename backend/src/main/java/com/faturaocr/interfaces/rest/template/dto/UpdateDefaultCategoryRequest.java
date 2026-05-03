package com.faturaocr.interfaces.rest.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@Schema(description = "Request to update default category for template")
public class UpdateDefaultCategoryRequest {
    @NotNull
    @Schema(description = "Category ID to set as default", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID categoryId;
}

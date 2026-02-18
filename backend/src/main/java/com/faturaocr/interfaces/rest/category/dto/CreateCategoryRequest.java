package com.faturaocr.interfaces.rest.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to create a new category")
public class CreateCategoryRequest {
    @NotBlank
    @Schema(description = "Category name", example = "Office Supplies", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Category description", example = "Expenses for office materials")
    private String description;

    @Schema(description = "Color hex code", example = "#FF5733")
    private String color;

    @Schema(description = "Icon identifier", example = "briefcase")
    private String icon;

    @Schema(description = "Parent category ID (for subcategories)", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentId;
}

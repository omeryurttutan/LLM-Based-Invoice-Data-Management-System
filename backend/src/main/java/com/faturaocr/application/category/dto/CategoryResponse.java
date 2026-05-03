package com.faturaocr.application.category.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Category details")
public class CategoryResponse {
    @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Category name", example = "Office Supplies")
    private String name;

    @Schema(description = "Category description", example = "Expenses for office materials")
    private String description;

    @Schema(description = "Color hex code", example = "#FF5733")
    private String color;

    @Schema(description = "Icon identifier", example = "briefcase")
    private String icon;

    @Schema(description = "Parent category ID")
    private UUID parentId;

    @Schema(description = "Parent category name")
    private String parentName;

    @Schema(description = "Active status", example = "true")
    private boolean isActive;

    @Schema(description = "Number of invoices in this category")
    private long invoiceCount;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}

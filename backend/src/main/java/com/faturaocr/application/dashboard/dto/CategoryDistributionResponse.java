package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Invoice distribution by category")
public class CategoryDistributionResponse {
    @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID categoryId;

    @Schema(description = "Category name", example = "Office Supplies")
    private String categoryName;

    @Schema(description = "Category color", example = "#FF5733")
    private String categoryColor;

    @Schema(description = "Number of invoices in category", example = "25")
    private int invoiceCount;

    @Schema(description = "Total amount in category", example = "5000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Percentage of total amount", example = "20.0")
    private BigDecimal percentage;
}

package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Monthly invoice trend data")
public class MonthlyTrendResponse {
    @Schema(description = "Month identifier", example = "2024-02")
    private String month; // YYYY-MM

    @Schema(description = "Display label", example = "Şubat 2024")
    private String label; // "Ocak 2024" etc.

    @Schema(description = "Total number of invoices", example = "50")
    private int invoiceCount;

    @Schema(description = "Total invoice amount", example = "10000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Total verified amount", example = "9000.00")
    private BigDecimal verifiedAmount;

    @Schema(description = "Average invoice amount", example = "200.00")
    private BigDecimal averageAmount;
}

package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Top suppliers statistics")
public class TopSuppliersResponse {
    @Schema(description = "List of top suppliers")
    private List<SupplierStats> suppliers;

    @Schema(description = "Count of other suppliers", example = "5")
    private int othersCount;

    @Schema(description = "Total amount for other suppliers", example = "2000.00")
    private BigDecimal othersAmount;

    @Data
    @Builder
    @Schema(description = "Supplier statistics")
    public static class SupplierStats {
        @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.")
        private String supplierName;

        @Schema(description = "Supplier tax number", example = "1234567890")
        private String supplierTaxNumber;

        @Schema(description = "Invoice count", example = "10")
        private int invoiceCount;

        @Schema(description = "Total amount", example = "15000.00")
        private BigDecimal totalAmount;

        @Schema(description = "Percentage of total amount", example = "15.0")
        private BigDecimal percentage;
    }
}

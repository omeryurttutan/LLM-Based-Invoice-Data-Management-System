package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Dashboard summary statistics")
public class DashboardStatsResponse {
    @Schema(description = "Time period for statistics")
    private Period period;

    @Schema(description = "Summary metrics")
    private Summary summary;

    @Schema(description = "Invoice source breakdown (MANUAL, EMAIL, UPLOAD, LLM)")
    private Map<String, SourceStats> sourceBreakdown;

    @Schema(description = "Confidence score statistics")
    private ConfidenceStats confidenceStats;

    @Data
    @Builder
    @Schema(description = "Statistics period")
    public static class Period {
        @Schema(description = "Start date", example = "2026-02-01")
        private LocalDate dateFrom;

        @Schema(description = "End date", example = "2026-02-28")
        private LocalDate dateTo;

        @Schema(description = "Currency", example = "TRY")
        private Currency currency;
    }

    @Data
    @Builder
    @Schema(description = "Summary metrics")
    public static class Summary {
        @Schema(description = "Total number of invoices", example = "150")
        private int totalInvoices;

        @Schema(description = "Total amount", example = "25000.00")
        private BigDecimal totalAmount;

        @Schema(description = "Average invoice amount", example = "166.67")
        private BigDecimal averageAmount;

        @Schema(description = "Number of pending invoices", example = "10")
        private int pendingCount;

        @Schema(description = "Total amount of pending invoices", example = "1500.00")
        private BigDecimal pendingAmount;

        @Schema(description = "Number of verified invoices", example = "135")
        private int verifiedCount;

        @Schema(description = "Total amount of verified invoices", example = "23000.00")
        private BigDecimal verifiedAmount;

        @Schema(description = "Number of rejected invoices", example = "5")
        private int rejectedCount;

        @Schema(description = "Number of invoices currently processing", example = "0")
        private int processingCount;
    }

    @Data
    @Builder
    @Schema(description = "Source statistics")
    public static class SourceStats {
        @Schema(description = "Count from this source", example = "50")
        private int count;

        @Schema(description = "Percentage of total", example = "33.3")
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @Schema(description = "Confidence statistics")
    public static class ConfidenceStats {
        @Schema(description = "Average confidence score", example = "92.5")
        private BigDecimal averageScore;

        @Schema(description = "Count of high confidence invoices (>90)", example = "100")
        private int highConfidence;

        @Schema(description = "Count of medium confidence invoices (70-90)", example = "40")
        private int mediumConfidence;

        @Schema(description = "Count of low confidence invoices (<70)", example = "10")
        private int lowConfidence;
    }
}

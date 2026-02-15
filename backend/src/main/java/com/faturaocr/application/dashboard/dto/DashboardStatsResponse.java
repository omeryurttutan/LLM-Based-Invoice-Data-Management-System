package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private Period period;
    private Summary summary;
    private Map<String, SourceStats> sourceBreakdown;
    private ConfidenceStats confidenceStats;

    @Data
    @Builder
    public static class Period {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Currency currency;
    }

    @Data
    @Builder
    public static class Summary {
        private int totalInvoices;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private int pendingCount;
        private BigDecimal pendingAmount;
        private int verifiedCount;
        private BigDecimal verifiedAmount;
        private int rejectedCount;
        private int processingCount;
    }

    @Data
    @Builder
    public static class SourceStats {
        private int count;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    public static class ConfidenceStats {
        private BigDecimal averageScore;
        private int highConfidence;
        private int mediumConfidence;
        private int lowConfidence;
    }
}

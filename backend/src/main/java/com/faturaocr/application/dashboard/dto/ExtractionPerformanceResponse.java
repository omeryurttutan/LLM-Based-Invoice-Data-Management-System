package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ExtractionPerformanceResponse {
    private int totalExtractions;
    private BigDecimal successRate;
    private BigDecimal averageConfidence;
    private BigDecimal averageDuration;
    private List<ProviderStats> byProvider;
    private List<FailureReason> failureReasons;

    @Data
    @Builder
    public static class ProviderStats {
        private LlmProvider provider;
        private int attempts;
        private int successCount;
        private int failureCount;
        private BigDecimal averageConfidence;
        private int fallbackCount;
    }

    @Data
    @Builder
    public static class FailureReason {
        private String reason;
        private int count;
    }
}

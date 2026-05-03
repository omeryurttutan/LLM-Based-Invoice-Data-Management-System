package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "LLM extraction performance metrics")
public class ExtractionPerformanceResponse {
    @Schema(description = "Total extraction attempts", example = "100")
    private int totalExtractions;

    @Schema(description = "Success rate (0-100)", example = "95.0")
    private BigDecimal successRate;

    @Schema(description = "Average confidence score", example = "92.5")
    private BigDecimal averageConfidence;

    @Schema(description = "Average processing duration (seconds)", example = "2.5")
    private BigDecimal averageDuration;

    @Schema(description = "Performance breakdown by provider")
    private List<ProviderStats> byProvider;

    @Schema(description = "Common failure reasons")
    private List<FailureReason> failureReasons;

    @Data
    @Builder
    @Schema(description = "Provider-specific statistics")
    public static class ProviderStats {
        @Schema(description = "LLM Provider", example = "GEMINI")
        private LlmProvider provider;

        @Schema(description = "Total attempts", example = "50")
        private int attempts;

        @Schema(description = "Successful extractions", example = "48")
        private int successCount;

        @Schema(description = "Failed extractions", example = "2")
        private int failureCount;

        @Schema(description = "Average confidence score", example = "94.0")
        private BigDecimal averageConfidence;

        @Schema(description = "Number of fallbacks triggered", example = "1")
        private int fallbackCount;
    }

    @Data
    @Builder
    @Schema(description = "Failure reason statistics")
    public static class FailureReason {
        @Schema(description = "Failure reason description", example = "Timeout")
        private String reason;

        @Schema(description = "Occurrence count", example = "5")
        private int count;
    }
}

package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@Schema(description = "Available filter options and value ranges")
public class FilterOptionsResponse {
    @Schema(description = "Available invoice statuses")
    private List<StatusOption> statuses;

    @Schema(description = "Available categories")
    private List<CategoryOption> categories;

    @Schema(description = "Available currencies")
    private List<Currency> currencies;

    @Schema(description = "Available source types")
    private List<SourceType> sourceTypes;

    @Schema(description = "Available LLM providers")
    private List<LlmProvider> llmProviders;

    @Schema(description = "Min/Max invoice amounts")
    private Range<BigDecimal> amountRange;

    @Schema(description = "Min/Max invoice dates")
    private Range<LocalDate> dateRange;

    @Schema(description = "Min/Max confidence scores")
    private Range<Double> confidenceRange;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    @Schema(description = "Invoice status option")
    public static class StatusOption {
        @Schema(description = "Status enum value", example = "PENDING")
        private InvoiceStatus value;

        @Schema(description = "Display label", example = "Pending Approval")
        private String label;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    @Schema(description = "Category option")
    public static class CategoryOption {
        @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID id;

        @Schema(description = "Category name", example = "Office Supplies")
        private String name;

        @Schema(description = "Category color hex code", example = "#FF5733")
        private String color;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    @Schema(description = "Value range")
    public static class Range<T> {
        @Schema(description = "Minimum value")
        private T min;

        @Schema(description = "Maximum value")
        private T max;
    }
}

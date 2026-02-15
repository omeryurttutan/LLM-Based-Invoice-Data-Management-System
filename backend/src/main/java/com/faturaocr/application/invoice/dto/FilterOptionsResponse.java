package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
public class FilterOptionsResponse {
    private List<StatusOption> statuses;
    private List<CategoryOption> categories;
    private List<Currency> currencies;
    private List<SourceType> sourceTypes;
    private List<LlmProvider> llmProviders;
    private Range<BigDecimal> amountRange;
    private Range<LocalDate> dateRange;
    private Range<Double> confidenceRange;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    public static class StatusOption {
        private InvoiceStatus value;
        private String label;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CategoryOption {
        private UUID id;
        private String name;
        private String color;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    public static class Range<T> {
        private T min;
        private T max;
    }
}

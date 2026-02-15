package com.faturaocr.interfaces.rest.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFilterRequest {

    // Date Filters
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    // Status Filter (Multi-value)
    private List<InvoiceStatus> status;

    // Supplier Filter
    private List<String> supplierName;

    // Category Filter
    private List<UUID> categoryId;

    // Amount Range Filters
    private BigDecimal amountMin;
    private BigDecimal amountMax;

    // Currency Filter
    private List<Currency> currency;

    // Source Type Filter
    private List<SourceType> sourceType;

    // LLM Provider Filter
    private List<String> llmProvider;

    // Confidence Score Filter
    private Double confidenceMin;
    private Double confidenceMax;

    // Full-Text Search
    private String search;

    // Created By Filter
    private UUID createdByUserId;

    // Date Created Range
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}

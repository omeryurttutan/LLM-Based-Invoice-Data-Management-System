package com.faturaocr.application.invoice.dto;

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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criteria for filtering invoices")
public class InvoiceFilterRequest {

    // Date Filters
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Start date (YYYY-MM-DD)", example = "2026-01-01")
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "End date (YYYY-MM-DD)", example = "2026-12-31")
    private LocalDate dateTo;

    // Status Filter (Multi-value)
    @Parameter(description = "List of statuses to include")
    private List<InvoiceStatus> status;

    // Supplier Filter
    @Parameter(description = "List of supplier names to include")
    private List<String> supplierName;

    // Category Filter
    @Parameter(description = "List of category IDs to include")
    private List<UUID> categoryId;

    // Amount Range Filters
    @Parameter(description = "Minimum total amount", example = "100.00")
    private BigDecimal amountMin;
    @Parameter(description = "Maximum total amount", example = "10000.00")
    private BigDecimal amountMax;

    // Currency Filter
    @Parameter(description = "List of currencies")
    private List<Currency> currency;

    // Source Type Filter
    @Parameter(description = "List of source types")
    private List<SourceType> sourceType;

    // LLM Provider Filter
    @Parameter(description = "List of LLM providers")
    private List<String> llmProvider;

    // Confidence Score Filter
    @Parameter(description = "Minimum confidence score (0-100)", example = "80.0")
    private Double confidenceMin;
    @Parameter(description = "Maximum confidence score (0-100)", example = "100.0")
    private Double confidenceMax;

    // Full-Text Search
    @Parameter(description = "Search term (invoice number or supplier name)")
    private String search;

    // Created By Filter
    @Parameter(description = "Filter by creator user ID", hidden = true)
    private UUID createdByUserId;

    // Date Created Range
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(description = "Created after timestamp", hidden = true)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(description = "Created before timestamp", hidden = true)
    private LocalDateTime createdTo;
}

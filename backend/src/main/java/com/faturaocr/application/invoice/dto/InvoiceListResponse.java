package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(description = "Summary of invoice for list view")
public class InvoiceListResponse {
    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Invoice number", example = "FTR-2026-00123")
    private String invoiceNumber;

    @Schema(description = "Invoice date", example = "2026-02-18")
    private LocalDate invoiceDate;

    @Schema(description = "Due date", example = "2026-03-18")
    private LocalDate dueDate;

    @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.")
    private String supplierName;

    @Schema(description = "Total amount", example = "1500.00")
    private BigDecimal totalAmount;

    @Schema(description = "Currency", example = "TRY")
    private Currency currency;

    @Schema(description = "Current status", example = "PENDING")
    private InvoiceStatus status;

    @Schema(description = "Source type", example = "MANUAL")
    private SourceType sourceType;

    @Schema(description = "Category name", example = "Hosting Services")
    private String categoryName;

    @Schema(description = "Number of line items", example = "3")
    private int itemCount;

    @Schema(description = "Created by user", example = "Mehmet Demir")
    private String createdByUserName;

    @Schema(description = "Creation timestamp", example = "2026-02-18T10:30:00")
    private LocalDateTime createdAt;
}

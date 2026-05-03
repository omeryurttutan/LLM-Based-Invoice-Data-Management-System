package com.faturaocr.interfaces.rest.invoice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to check for potential duplicate invoices")
public class DuplicateCheckRequestDTO {
    @Schema(description = "Invoice number to check", example = "FTR-2026-00123")
    private String invoiceNumber;

    @Schema(description = "Invoice date to check", example = "2026-02-18")
    private LocalDate invoiceDate;

    @Schema(description = "Total amount to check", example = "1200.00")
    private BigDecimal totalAmount;

    @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.")
    private String supplierName;

    @Schema(description = "Supplier tax number", example = "1234567890")
    private String supplierTaxNumber;
}

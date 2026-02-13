package com.faturaocr.application.invoice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DuplicateCheckRequest {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private String supplierName;
    private String supplierTaxNumber;
    private UUID companyId;
    private UUID excludeInvoiceId; // Exclude this invoice from results (used during updates)
}

package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DuplicateMatch {
    private UUID invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String supplierName;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private DuplicateConfidence confidence;
    private String matchReason;
}

package com.faturaocr.interfaces.rest.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private UUID companyId;
    private UUID categoryId;
    private UUID createdByUserId;
    private UUID verifiedByUserId;
    private UUID batchId;

    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    private String supplierName;
    private String supplierTaxNumber;
    private String supplierTaxOffice;
    private String supplierAddress;
    private String supplierPhone;
    private String supplierEmail;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Currency currency;
    private BigDecimal exchangeRate;

    private InvoiceStatus status;
    private SourceType sourceType;
    private LlmProvider llmProvider;
    private BigDecimal confidenceScore;

    private String originalFileName;
    private Integer originalFileSize;

    private String notes;
    private String rejectionReason;

    private LocalDateTime verifiedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<InvoiceItemResponse> items;

    @Data
    @Builder
    public static class InvoiceItemResponse {
        private UUID id;
        private int lineNumber;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal subtotal;
        private BigDecimal totalAmount;
        private String productCode;
        private String barcode;
    }
}

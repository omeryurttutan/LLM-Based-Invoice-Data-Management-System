package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceDetailResponse {
    private UUID id;
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
    private UUID categoryId;
    private String categoryName;
    private String notes;
    private String rejectionReason;
    private UUID createdByUserId;
    private String createdByUserName;
    private UUID verifiedByUserId;
    private String verifiedByUserName;
    private LocalDateTime verifiedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemResponse> items;
}

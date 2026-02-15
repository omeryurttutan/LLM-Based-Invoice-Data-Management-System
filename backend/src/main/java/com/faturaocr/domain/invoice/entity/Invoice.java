package com.faturaocr.domain.invoice.entity;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Invoice {
    private UUID id;
    private UUID companyId;
    private UUID categoryId;
    private UUID createdByUserId;
    private UUID verifiedByUserId;
    private UUID batchId;
    private String correlationId;

    // Identification
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Supplier
    private String supplierName;
    private String supplierTaxNumber;
    private String supplierTaxOffice;
    private String supplierAddress;
    private String supplierPhone;
    private String supplierEmail;

    // Financial
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Currency currency;
    private BigDecimal exchangeRate;

    // Status
    private InvoiceStatus status;

    // Source
    private SourceType sourceType;
    private LlmProvider llmProvider;
    private BigDecimal confidenceScore;
    private Integer processingDurationMs;

    // File
    private String originalFilePath;
    private String storedFilePath;
    private String originalFileName;
    private String fileHash;
    private Integer originalFileSize;
    private String originalFileType;

    // E-Invoice
    private String eInvoiceUuid;
    private String eInvoiceEttn;

    private List<ExtractionCorrection> extractionCorrections;

    // Notes
    private String notes;
    private String rejectionReason;

    // Timestamps
    private LocalDateTime verifiedAt;
    private LocalDateTime rejectedAt;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Items
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
        this.id = UUID.randomUUID();
        this.status = InvoiceStatus.PENDING;
        this.sourceType = SourceType.MANUAL;
        this.currency = Currency.TRY;
        this.exchangeRate = BigDecimal.ONE;
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    public void addItem(InvoiceItem item) {
        item.setInvoiceId(this.id);
        this.items.add(item);
    }

    public void removeItem(InvoiceItem item) {
        this.items.remove(item);
    }

    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;

        for (InvoiceItem item : items) {
            this.subtotal = this.subtotal.add(item.getSubtotal());
            this.taxAmount = this.taxAmount.add(item.getTaxAmount());
            this.totalAmount = this.totalAmount.add(item.getTotalAmount());
        }
    }
}

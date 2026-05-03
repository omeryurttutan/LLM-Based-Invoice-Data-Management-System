package com.faturaocr.application.export.dto;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceExportData {
    // Invoice Leve Fields
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String supplierName;
    private String supplierTaxNumber;
    private String supplierTaxOffice;
    private String buyerName;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Currency currency;
    private InvoiceStatus status;
    private SourceType sourceType;
    private String categoryName;
    private Float confidenceScore;
    private String llmProvider;
    private String createdBy;
    private LocalDateTime createdAt;
    private String notes;

    // Item Level Fields (Optional)
    private String itemDescription;
    private BigDecimal itemQuantity;
    private String itemUnit;
    private BigDecimal itemUnitPrice;
    private BigDecimal itemTaxRate;
    private BigDecimal itemTaxAmount;
    private BigDecimal itemTotalAmount;

    // Helper to map from Domain Entity
    public static InvoiceExportData from(Invoice invoice, String buyerName, String categoryName, String createdByName) {
        return InvoiceExportData.builder()
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .supplierName(invoice.getSupplierName())
                .supplierTaxNumber(invoice.getSupplierTaxNumber())
                .supplierTaxOffice(invoice.getSupplierTaxOffice())
                .buyerName(buyerName)
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .sourceType(invoice.getSourceType())
                .categoryName(categoryName)
                .confidenceScore(
                        invoice.getConfidenceScore() != null ? invoice.getConfidenceScore().floatValue() : null)
                .llmProvider(invoice.getLlmProvider() != null ? invoice.getLlmProvider().name() : "")
                .createdBy(createdByName)
                .createdAt(invoice.getCreatedAt())
                .notes(invoice.getNotes())
                .build();
    }

    // Helper to create copy with item details
    public InvoiceExportData withItem(InvoiceItem item) {
        InvoiceExportData copy = InvoiceExportData.builder()
                .invoiceNumber(this.invoiceNumber)
                .invoiceDate(this.invoiceDate)
                .dueDate(this.dueDate)
                .supplierName(this.supplierName)
                .supplierTaxNumber(this.supplierTaxNumber)
                .supplierTaxOffice(this.supplierTaxOffice)
                .buyerName(this.buyerName)
                .subtotal(this.subtotal)
                .taxAmount(this.taxAmount)
                .totalAmount(this.totalAmount)
                .currency(this.currency)
                .status(this.status)
                .sourceType(this.sourceType)
                .categoryName(this.categoryName)
                .confidenceScore(this.confidenceScore)
                .llmProvider(this.llmProvider)
                .createdBy(this.createdBy)
                .createdAt(this.createdAt)
                .notes(this.notes)
                // Item details
                .itemDescription(item.getDescription())
                .itemQuantity(item.getQuantity())
                .itemUnit(item.getUnit())
                .itemUnitPrice(item.getUnitPrice())
                .itemTaxRate(item.getTaxRate())
                .itemTaxAmount(item.getTaxAmount())
                .itemTotalAmount(item.getTotalAmount())
                .build();
        return copy;
    }
}

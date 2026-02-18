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

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Detailed invoice information")
public class InvoiceDetailResponse {
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

    @Schema(description = "Supplier tax number", example = "1234567890")
    private String supplierTaxNumber;

    @Schema(description = "Supplier tax office", example = "Kadıköy")
    private String supplierTaxOffice;

    @Schema(description = "Supplier address", example = "Merkez Mah. No:1")
    private String supplierAddress;

    @Schema(description = "Supplier phone", example = "+902125551234")
    private String supplierPhone;

    @Schema(description = "Supplier email", example = "info@tech.com")
    private String supplierEmail;

    @Schema(description = "Subtotal amount (excluding tax)", example = "1000.00")
    private BigDecimal subtotal;

    @Schema(description = "Total tax amount", example = "200.00")
    private BigDecimal taxAmount;

    @Schema(description = "Total amount (including tax)", example = "1200.00")
    private BigDecimal totalAmount;

    @Schema(description = "Currency", example = "TRY")
    private Currency currency;

    @Schema(description = "Exchange rate", example = "1.0")
    private BigDecimal exchangeRate;

    @Schema(description = "Status", example = "VERIFIED")
    private InvoiceStatus status;

    @Schema(description = "Source type", example = "LLM")
    private SourceType sourceType;

    @Schema(description = "LLM Provider used (if applicable)", example = "GEMINI")
    private LlmProvider llmProvider;

    @Schema(description = "Extraction confidence score (0-100)", example = "95.5")
    private BigDecimal confidenceScore;

    @Schema(description = "Category ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID categoryId;

    @Schema(description = "Category name", example = "Hosting Services")
    private String categoryName;

    @Schema(description = "Notes", example = "Approved by manager")
    private String notes;

    @Schema(description = "Rejection reason (if rejected)", example = "Duplicate invoice")
    private String rejectionReason;

    @Schema(description = "ID of user who created the invoice")
    private UUID createdByUserId;

    @Schema(description = "Name of user who created the invoice", example = "Mehmet Demir")
    private String createdByUserName;

    @Schema(description = "ID of user who verified the invoice")
    private UUID verifiedByUserId;

    @Schema(description = "Name of user who verified the invoice", example = "Ayşe Yılmaz")
    private String verifiedByUserName;

    @Schema(description = "Verification timestamp")
    private LocalDateTime verifiedAt;

    @Schema(description = "Rejection timestamp")
    private LocalDateTime rejectedAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "List of invoice line items")
    private List<InvoiceItemResponse> items;
}

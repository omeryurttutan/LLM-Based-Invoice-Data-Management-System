package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Invoices requiring user action")
public class PendingActionsResponse {
    @Schema(description = "Total number of pending invoices", example = "5")
    private int totalPending;

    @Schema(description = "List of pending invoices")
    private List<PendingInvoice> invoices;

    @Data
    @Builder
    @Schema(description = "Pending invoice summary")
    public static class PendingInvoice {
        @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID id;

        @Schema(description = "Invoice number", example = "FTR-2026-00123")
        private String invoiceNumber;

        @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.")
        private String supplierName;

        @Schema(description = "Total amount", example = "1500.00")
        private BigDecimal totalAmount;

        @Schema(description = "Currency", example = "TRY")
        private Currency currency;

        @Schema(description = "Source type", example = "LLM")
        private SourceType sourceType;

        @Schema(description = "Confidence score (if extracted)", example = "85.5")
        private BigDecimal confidenceScore;

        @Schema(description = "Creation timestamp")
        private LocalDateTime createdAt;

        @Schema(description = "Days pending since creation", example = "2")
        private long daysPending;
    }
}

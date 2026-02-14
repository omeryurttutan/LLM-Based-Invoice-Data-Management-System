package com.faturaocr.domain.invoice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ExtractionResultMessage {
    @JsonProperty("message_id")
    private UUID messageId;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    private Integer attempt;

    private String status; // COMPLETED, FAILED

    @JsonProperty("invoice_data")
    private InvoiceDataDto invoiceData;

    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    private LlmProvider provider;

    @JsonProperty("suggested_status")
    private String suggestedStatus;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("processing_duration_ms")
    private Integer processingDurationMs;

    private LocalDateTime timestamp;

    // Nested DTO for extracted data to avoid dependency on Entity
    @Data
    @NoArgsConstructor
    public static class InvoiceDataDto {
        @JsonProperty("invoice_number")
        private String invoiceNumber;

        @JsonProperty("invoice_date")
        private String invoiceDate;

        @JsonProperty("due_date")
        private String dueDate;

        @JsonProperty("supplier_name")
        private String supplierName;

        @JsonProperty("supplier_tax_id")
        private String supplierTaxId;

        @JsonProperty("supplier_address")
        private String supplierAddress;

        private BigDecimal subtotal;

        @JsonProperty("tax_amount")
        private BigDecimal taxAmount;

        @JsonProperty("total_amount")
        private BigDecimal totalAmount;

        private String currency;

        @JsonProperty("line_items")
        private java.util.List<LineItemDto> lineItems;
    }

    @Data
    @NoArgsConstructor
    public static class LineItemDto {
        private String description;
        private BigDecimal quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;

        @JsonProperty("total_price")
        private BigDecimal totalPrice;

        @JsonProperty("tax_rate")
        private BigDecimal taxRate;
    }
}

package com.faturaocr.infrastructure.adapter.extraction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExtractionResult {
    @JsonProperty("invoice_data")
    private InvoiceData invoiceData;

    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("validation_result")
    private ValidationResult validationResult;

    @Data
    public static class InvoiceData {
        @JsonProperty("invoice_number")
        private String invoiceNumber;

        @JsonProperty("invoice_date")
        private LocalDate invoiceDate; // JSON format YYYY-MM-DD usually works

        @JsonProperty("due_date")
        private LocalDate dueDate;

        @JsonProperty("supplier_name")
        private String supplierName;

        @JsonProperty("supplier_tax_id")
        private String supplierTaxId;

        @JsonProperty("supplier_address")
        private String supplierAddress;

        @JsonProperty("total_amount")
        private BigDecimal totalAmount;

        @JsonProperty("tax_amount")
        private BigDecimal taxAmount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("items")
        private List<InvoiceItemData> items;
    }

    @Data
    public static class InvoiceItemData {
        @JsonProperty("description")
        private String description;

        @JsonProperty("quantity")
        private BigDecimal quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;

        @JsonProperty("total_price")
        private BigDecimal totalPrice;

        @JsonProperty("tax_rate")
        private BigDecimal taxRate;
    }

    @Data
    public static class ValidationResult {
        @JsonProperty("is_valid")
        private boolean isValid;

        @JsonProperty("errors")
        private List<String> errors;
    }
}

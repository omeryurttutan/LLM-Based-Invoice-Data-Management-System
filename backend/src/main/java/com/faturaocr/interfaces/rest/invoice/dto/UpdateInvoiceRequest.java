package com.faturaocr.interfaces.rest.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateInvoiceRequest {
    @NotBlank
    private String invoiceNumber;

    @NotNull
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotBlank
    private String supplierName;

    private String supplierTaxNumber;
    private String supplierTaxOffice;
    private String supplierAddress;
    private String supplierPhone;
    private String supplierEmail;

    @NotBlank
    private String currency;

    private BigDecimal exchangeRate;

    private UUID categoryId;
    private String notes;

    @Valid
    private List<UpdateInvoiceItemRequest> items;

    @Data
    public static class UpdateInvoiceItemRequest {
        private UUID id; // Null for new items

        @NotBlank
        private String description;

        @NotNull
        private BigDecimal quantity;

        private String unit;

        @NotNull
        private BigDecimal unitPrice;

        @NotNull
        private BigDecimal taxRate;

        private String productCode;
        private String barcode;
    }
}

package com.faturaocr.interfaces.rest.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to create a new manual invoice")
public class CreateInvoiceRequest {
    @Schema(description = "Invoice number", example = "FTR-2026-00123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String invoiceNumber;

    @Schema(description = "Invoice date", example = "2026-02-18", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LocalDate invoiceDate;

    @Schema(description = "Due date for payment", example = "2026-03-18")
    private LocalDate dueDate;

    @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String supplierName;

    @Schema(description = "Supplier tax number (VKN or TCKN)", example = "1234567890")
    private String supplierTaxNumber;

    @Schema(description = "Supplier tax office", example = "Kadıköy")
    private String supplierTaxOffice;

    @Schema(description = "Supplier full address", example = "Merkez Mah. Atatürk Cad. No:1, İstanbul")
    private String supplierAddress;

    @Schema(description = "Supplier phone number", example = "+902125551234")
    private String supplierPhone;

    @Schema(description = "Supplier email address", example = "accounting@techsolutions.com")
    private String supplierEmail;

    @Schema(description = "Invoice currency code", example = "TRY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String currency;

    @Schema(description = "Exchange rate to base currency (if foreign currency)", example = "32.50")
    private BigDecimal exchangeRate;

    @Schema(description = "Category ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID categoryId;

    @Schema(description = "Additional notes", example = "Monthly server maintenance fee")
    private String notes;

    @Schema(description = "List of invoice line items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<CreateInvoiceItemRequest> items;

    @Data
    @Schema(description = "Invoice line item details")
    public static class CreateInvoiceItemRequest {
        @Schema(description = "Item description", example = "Cloud Server Hosting", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String description;

        @Schema(description = "Quantity", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal quantity;

        @Schema(description = "Unit of measure", example = "ADET")
        private String unit;

        @Schema(description = "Unit price (excluding tax)", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal unitPrice;

        @Schema(description = "Tax rate percentage (0-100)", example = "20.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal taxRate; // e.g. 18.00 or 20.00

        @Schema(description = "Product or service code", example = "SRV-001")
        private String productCode;

        @Schema(description = "Barcode / GTIN", example = "8691234567890")
        private String barcode;
    }
}

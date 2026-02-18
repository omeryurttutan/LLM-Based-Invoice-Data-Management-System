package com.faturaocr.interfaces.rest.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to update an existing invoice")
public class UpdateInvoiceRequest {
    @Schema(description = "Invoice number", example = "FTR-2026-00123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String invoiceNumber;

    @Schema(description = "Invoice date", example = "2026-02-18", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LocalDate invoiceDate;

    @Schema(description = "Due date", example = "2026-03-18")
    private LocalDate dueDate;

    @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
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

    @Schema(description = "Currency code", example = "TRY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String currency;

    @Schema(description = "Exchange rate", example = "1.0")
    private BigDecimal exchangeRate;

    @Schema(description = "Category ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID categoryId;

    @Schema(description = "Notes", example = "Updated via API")
    private String notes;

    @Schema(description = "List of line items")
    @Valid
    private List<UpdateInvoiceItemRequest> items;

    @Schema(description = "List of extraction corrections (for LLM auditing)")
    @JsonProperty("extraction_corrections")
    private List<ExtractionCorrection> extractionCorrections;

    @Data
    @Schema(description = "Invoice line item for update")
    public static class UpdateInvoiceItemRequest {
        @Schema(description = "Item ID (null for new items)", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID id; // Null for new items

        @Schema(description = "Item description", example = "Cloud Server Hosting", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String description;

        @Schema(description = "Quantity", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal quantity;

        @Schema(description = "Unit", example = "ADET")
        private String unit;

        @Schema(description = "Unit price", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal unitPrice;

        @Schema(description = "Tax rate", example = "20.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal taxRate;

        @Schema(description = "Product code", example = "SRV-001")
        private String productCode;

        @Schema(description = "Barcode", example = "8691234567890")
        private String barcode;
    }
}

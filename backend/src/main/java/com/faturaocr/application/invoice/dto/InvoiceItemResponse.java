package com.faturaocr.application.invoice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Invoice line item details")
public class InvoiceItemResponse {
    @Schema(description = "Item ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Line number", example = "1")
    private Integer lineNumber;

    @Schema(description = "Item description", example = "Cloud Server Hosting")
    private String description;

    @Schema(description = "Quantity", example = "1")
    private BigDecimal quantity;

    @Schema(description = "Unit", example = "ADET")
    private String unit;

    @Schema(description = "Unit price", example = "1000.00")
    private BigDecimal unitPrice;

    @Schema(description = "Tax rate", example = "20.00")
    private BigDecimal taxRate;

    @Schema(description = "Calculated tax amount", example = "200.00")
    private BigDecimal taxAmount;

    @Schema(description = "Calculated subtotal", example = "1000.00")
    private BigDecimal subtotal;

    @Schema(description = "Calculated total amount", example = "1200.00")
    private BigDecimal totalAmount;

    @Schema(description = "Product code", example = "SRV-001")
    private String productCode;

    @Schema(description = "Barcode", example = "8691234567890")
    private String barcode;
}

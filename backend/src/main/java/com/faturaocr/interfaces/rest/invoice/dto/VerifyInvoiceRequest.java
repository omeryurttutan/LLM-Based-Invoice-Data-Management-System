package com.faturaocr.interfaces.rest.invoice.dto;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to verify an invoice")
public class VerifyInvoiceRequest {
    @Schema(description = "Verification notes", example = "Verified against PO #123")
    private String notes;
}

package com.faturaocr.interfaces.rest.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to reject an invoice")
public class RejectInvoiceRequest {
    @Schema(description = "Reason for rejection", example = "Duplicate entry found", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}

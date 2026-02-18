package com.faturaocr.application.invoice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Generic invoice operation response")
public class InvoiceResponse {
    @Schema(description = "Affected Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Operation message", example = "Invoice created successfully")
    private String message;
}

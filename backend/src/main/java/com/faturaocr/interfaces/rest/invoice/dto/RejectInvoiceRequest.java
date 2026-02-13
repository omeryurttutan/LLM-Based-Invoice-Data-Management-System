package com.faturaocr.interfaces.rest.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectInvoiceRequest {
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}

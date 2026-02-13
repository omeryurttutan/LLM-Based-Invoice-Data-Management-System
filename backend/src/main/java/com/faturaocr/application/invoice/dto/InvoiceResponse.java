package com.faturaocr.application.invoice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private String message;
}

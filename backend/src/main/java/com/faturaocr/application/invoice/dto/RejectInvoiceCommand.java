package com.faturaocr.application.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectInvoiceCommand {
    private String rejectionReason;
}

package com.faturaocr.interfaces.rest.invoice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DuplicateCheckRequestDTO {
    private String invoiceNumber;

    private LocalDate invoiceDate;

    private BigDecimal totalAmount;

    private String supplierName;

    private String supplierTaxNumber;
}

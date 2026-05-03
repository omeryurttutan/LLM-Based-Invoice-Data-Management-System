package com.faturaocr.application.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceCommand {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String supplierName;
    private String supplierTaxNumber;
    private String supplierTaxOffice;
    private String supplierAddress;
    private String supplierPhone;
    private String supplierEmail;
    private String currency;
    private BigDecimal exchangeRate;
    private UUID categoryId;
    private String notes;
    private List<CreateInvoiceItemCommand> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInvoiceItemCommand {
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private String productCode;
        private String barcode;
    }
}

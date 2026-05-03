package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;

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
public class UpdateInvoiceCommand {
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
    private List<UpdateInvoiceItemCommand> items;
    private List<ExtractionCorrection> extractionCorrections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInvoiceItemCommand {
        private UUID id; // Null for new items
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private String productCode;
        private String barcode;
    }
}

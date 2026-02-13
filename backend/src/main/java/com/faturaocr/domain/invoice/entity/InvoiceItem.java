package com.faturaocr.domain.invoice.entity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InvoiceItem {
    private UUID id;
    private UUID invoiceId;
    private Integer lineNumber;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private String productCode;
    private String barcode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvoiceItem() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.quantity = BigDecimal.ONE;
        this.unit = "ADET";
        this.taxRate = new BigDecimal("18.00");
    }
}

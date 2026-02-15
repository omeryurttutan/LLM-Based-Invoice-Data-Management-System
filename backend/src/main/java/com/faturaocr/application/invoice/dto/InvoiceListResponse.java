package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class InvoiceListResponse {
    private UUID id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String supplierName;
    private BigDecimal totalAmount;
    private Currency currency;
    private InvoiceStatus status;
    private SourceType sourceType;
    private String categoryName;
    private int itemCount;
    private String createdByUserName;
    private LocalDateTime createdAt;
}

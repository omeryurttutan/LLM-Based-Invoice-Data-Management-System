package com.faturaocr.application.dashboard.dto;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PendingActionsResponse {
    private int totalPending;
    private List<PendingInvoice> invoices;

    @Data
    @Builder
    public static class PendingInvoice {
        private UUID id;
        private String invoiceNumber;
        private String supplierName;
        private BigDecimal totalAmount;
        private Currency currency;
        private SourceType sourceType;
        private BigDecimal confidenceScore;
        private LocalDateTime createdAt;
        private long daysPending;
    }
}

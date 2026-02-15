package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TopSuppliersResponse {
    private List<SupplierStats> suppliers;
    private int othersCount;
    private BigDecimal othersAmount;

    @Data
    @Builder
    public static class SupplierStats {
        private String supplierName;
        private String supplierTaxNumber;
        private int invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal percentage;
    }
}

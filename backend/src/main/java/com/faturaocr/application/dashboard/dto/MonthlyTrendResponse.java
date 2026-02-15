package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendResponse {
    private String month; // YYYY-MM
    private String label; // "Ocak 2024" etc.
    private int invoiceCount;
    private BigDecimal totalAmount;
    private BigDecimal verifiedAmount;
    private BigDecimal averageAmount;
}

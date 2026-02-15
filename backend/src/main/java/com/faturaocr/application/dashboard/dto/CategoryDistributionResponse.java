package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CategoryDistributionResponse {
    private UUID categoryId;
    private String categoryName;
    private String categoryColor;
    private int invoiceCount;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
}

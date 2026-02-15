package com.faturaocr.interfaces.rest.template.dto;

import com.faturaocr.domain.template.valueobject.LearnedData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SupplierTemplateResponse {
    private Long id;
    private UUID companyId;
    private String supplierTaxNumber;
    private String supplierName;
    private int sampleCount;
    private LearnedData learnedData;
    private UUID defaultCategoryId;
    private boolean active;
    private LocalDateTime lastInvoiceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

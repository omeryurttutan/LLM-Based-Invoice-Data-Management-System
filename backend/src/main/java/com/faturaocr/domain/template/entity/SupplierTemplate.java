package com.faturaocr.domain.template.entity;

import com.faturaocr.domain.template.valueobject.LearnedData;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SupplierTemplate {
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

    public SupplierTemplate() {
        this.active = true;
        this.sampleCount = 0;
        this.learnedData = new LearnedData();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

package com.faturaocr.interfaces.rest.template.dto;

import com.faturaocr.domain.template.valueobject.LearnedData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Supplier learning template")
public class SupplierTemplateResponse {
    @Schema(description = "Template ID", example = "1")
    private Long id;

    @Schema(description = "Company ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID companyId;

    @Schema(description = "Supplier tax number", example = "1234567890")
    private String supplierTaxNumber;

    @Schema(description = "Supplier name", example = "Tech Solutions Ltd. Şti.")
    private String supplierName;

    @Schema(description = "Number of invoices learned from", example = "5")
    private int sampleCount;

    @Schema(description = "Learned extraction patterns")
    private LearnedData learnedData;

    @Schema(description = "Default category ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID defaultCategoryId;

    @Schema(description = "Is template active", example = "true")
    private boolean active;

    @Schema(description = "Date of last invoice processed")
    private LocalDateTime lastInvoiceDate;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}

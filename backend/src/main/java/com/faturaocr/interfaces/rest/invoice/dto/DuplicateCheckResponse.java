package com.faturaocr.interfaces.rest.invoice.dto;

import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import lombok.Builder;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Result of duplicate check operation")
public class DuplicateCheckResponse {
    @Schema(description = "Operation success status", example = "true")
    private boolean success;

    @Schema(description = "Detailed duplicate check result")
    private DuplicateCheckResult data;

    public static DuplicateCheckResponse success(DuplicateCheckResult result) {
        return DuplicateCheckResponse.builder()
                .success(true)
                .data(result)
                .build();
    }
}

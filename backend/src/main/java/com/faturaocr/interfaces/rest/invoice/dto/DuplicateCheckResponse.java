package com.faturaocr.interfaces.rest.invoice.dto;

import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuplicateCheckResponse {
    private boolean success;
    private DuplicateCheckResult data;

    public static DuplicateCheckResponse success(DuplicateCheckResult result) {
        return DuplicateCheckResponse.builder()
                .success(true)
                .data(result)
                .build();
    }
}

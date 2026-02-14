package com.faturaocr.application.invoice.dto;

import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DuplicateCheckResult {
    private boolean hasDuplicates;
    private List<DuplicateMatch> duplicates;
    private DuplicateConfidence highestConfidence;

    public static DuplicateCheckResult noDuplicates() {
        return DuplicateCheckResult.builder()
                .hasDuplicates(false)
                .duplicates(List.of())
                .highestConfidence(DuplicateConfidence.NONE)
                .build();
    }
}

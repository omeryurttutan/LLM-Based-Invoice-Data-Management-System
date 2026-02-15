package com.faturaocr.domain.template.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldAccuracy implements Serializable {
    private int correctCount;
    private int correctedCount;
    private int totalCount;

    public void incrementCorrect() {
        this.correctCount++;
        this.totalCount++;
    }

    public void incrementCorrected() {
        this.correctedCount++;
        this.totalCount++;
    }

    public double getAccuracy() {
        if (totalCount == 0)
            return 0.0;
        return (double) correctCount / totalCount * 100.0;
    }
}

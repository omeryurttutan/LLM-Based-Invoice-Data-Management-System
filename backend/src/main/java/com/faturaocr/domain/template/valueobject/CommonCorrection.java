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
public class CommonCorrection implements Serializable {
    private String field;
    private Object originalValue;
    private Object correctedTo;
    private int frequency;

    public void incrementFrequency() {
        this.frequency++;
    }
}

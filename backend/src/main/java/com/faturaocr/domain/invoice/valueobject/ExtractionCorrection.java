package com.faturaocr.domain.invoice.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionCorrection implements Serializable {
    private String field;

    @com.fasterxml.jackson.annotation.JsonProperty("original_value")
    private Object originalValue;

    @com.fasterxml.jackson.annotation.JsonProperty("corrected_value")
    private Object correctedValue;
}

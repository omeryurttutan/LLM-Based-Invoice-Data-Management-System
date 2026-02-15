package com.faturaocr.domain.template.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmountRange implements Serializable {
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal average;
}

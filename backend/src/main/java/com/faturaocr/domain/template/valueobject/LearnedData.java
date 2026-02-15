package com.faturaocr.domain.template.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnedData implements Serializable {

    @Builder.Default
    @JsonProperty("category_distribution")
    private Map<String, Integer> categoryDistribution = new HashMap<>();

    @Builder.Default
    @JsonProperty("field_accuracy")
    private Map<String, FieldAccuracy> fieldAccuracy = new HashMap<>();

    @Builder.Default
    @JsonProperty("common_corrections")
    private List<CommonCorrection> commonCorrections = new ArrayList<>();

    @JsonProperty("typical_line_item_count")
    private Double typicalLineItemCount;

    @JsonProperty("typical_amount_range")
    private AmountRange typicalAmountRange;

    @Builder.Default
    @JsonProperty("typical_tax_rates")
    private List<BigDecimal> typicalTaxRates = new ArrayList<>();
}

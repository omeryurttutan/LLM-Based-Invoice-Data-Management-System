package com.faturaocr.domain.rule.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition implements Serializable {
    private String field;
    private ConditionOperator operator;
    private Object value;
}

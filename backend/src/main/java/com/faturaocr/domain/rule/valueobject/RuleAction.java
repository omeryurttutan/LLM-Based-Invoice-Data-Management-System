package com.faturaocr.domain.rule.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleAction implements Serializable {
    private ActionType type;
    private Map<String, Object> params;
}

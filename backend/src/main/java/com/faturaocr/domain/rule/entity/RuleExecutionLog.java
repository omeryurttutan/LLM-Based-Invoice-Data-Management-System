package com.faturaocr.domain.rule.entity;

import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RuleExecutionLog {
    private Long id;
    private Long ruleId;
    private UUID invoiceId;
    private UUID companyId;
    private TriggerPoint triggerPoint;
    private Object conditionsMatched;
    private Object actionsApplied;
    private String executionResult; // SUCCESS, FAILED
    private String errorMessage;
    private LocalDateTime executedAt;

    public RuleExecutionLog() {
        this.executedAt = LocalDateTime.now();
    }
}

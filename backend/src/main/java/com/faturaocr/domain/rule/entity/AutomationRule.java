package com.faturaocr.domain.rule.entity;

import com.faturaocr.domain.rule.valueobject.RuleAction;
import com.faturaocr.domain.rule.valueobject.RuleCondition;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AutomationRule {
    private Long id;
    private UUID companyId;
    private String name;
    private String description;
    private List<RuleCondition> conditions;
    private List<RuleAction> actions;
    private String conditionLogic; // AND, OR
    private int priority;
    private boolean active;
    private TriggerPoint triggerPoint;
    private int executionCount;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdByUserId;

    public AutomationRule() {
        this.active = true;
        this.conditionLogic = "AND";
        this.priority = 100;
        this.executionCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

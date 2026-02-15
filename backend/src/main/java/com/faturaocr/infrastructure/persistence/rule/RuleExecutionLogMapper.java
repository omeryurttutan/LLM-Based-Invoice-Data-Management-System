package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.entity.RuleExecutionLog;
import org.springframework.stereotype.Component;

@Component
public class RuleExecutionLogMapper {

    public RuleExecutionLog toDomain(RuleExecutionLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        RuleExecutionLog domain = new RuleExecutionLog();
        domain.setId(entity.getId());
        domain.setRuleId(entity.getRuleId());
        domain.setInvoiceId(entity.getInvoiceId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setTriggerPoint(entity.getTriggerPoint());
        domain.setConditionsMatched(entity.getConditionsMatched());
        domain.setActionsApplied(entity.getActionsApplied());
        domain.setExecutionResult(entity.getExecutionResult());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setExecutedAt(entity.getExecutedAt());
        return domain;
    }

    public RuleExecutionLogJpaEntity toJpa(RuleExecutionLog domain) {
        if (domain == null) {
            return null;
        }
        RuleExecutionLogJpaEntity entity = new RuleExecutionLogJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setRuleId(domain.getRuleId());
        entity.setInvoiceId(domain.getInvoiceId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setTriggerPoint(domain.getTriggerPoint());
        entity.setConditionsMatched(domain.getConditionsMatched());
        entity.setActionsApplied(domain.getActionsApplied());
        entity.setExecutionResult(domain.getExecutionResult());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setExecutedAt(domain.getExecutedAt());
        return entity;
    }
}

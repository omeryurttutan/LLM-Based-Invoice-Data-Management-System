package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.entity.AutomationRule;
import org.springframework.stereotype.Component;

@Component
public class AutomationRuleMapper {

    public AutomationRule toDomain(AutomationRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AutomationRule domain = new AutomationRule();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setConditions(entity.getConditions());
        domain.setActions(entity.getActions());
        domain.setConditionLogic(entity.getConditionLogic());
        domain.setPriority(entity.getPriority());
        domain.setActive(entity.isActive());
        domain.setTriggerPoint(entity.getTriggerPoint());
        domain.setExecutionCount(entity.getExecutionCount());
        domain.setLastExecutedAt(entity.getLastExecutedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setCreatedByUserId(entity.getCreatedByUserId());
        return domain;
    }

    public AutomationRuleJpaEntity toJpa(AutomationRule domain) {
        if (domain == null) {
            return null;
        }
        AutomationRuleJpaEntity entity = new AutomationRuleJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setCompanyId(domain.getCompanyId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setConditions(domain.getConditions());
        entity.setActions(domain.getActions());
        entity.setConditionLogic(domain.getConditionLogic());
        entity.setPriority(domain.getPriority());
        entity.setActive(domain.isActive());
        entity.setTriggerPoint(domain.getTriggerPoint());
        entity.setExecutionCount(domain.getExecutionCount());
        entity.setLastExecutedAt(domain.getLastExecutedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedByUserId(domain.getCreatedByUserId());
        return entity;
    }
}

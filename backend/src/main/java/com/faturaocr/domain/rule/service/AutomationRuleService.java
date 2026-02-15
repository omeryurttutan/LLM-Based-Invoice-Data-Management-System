package com.faturaocr.domain.rule.service;

import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.port.AutomationRuleRepository;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutomationRuleService {

    private final AutomationRuleRepository repository;

    @Transactional
    public AutomationRule createRule(AutomationRule rule) {
        validateRule(rule);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setExecutionCount(0);
        return repository.save(rule);
    }

    @Transactional
    public AutomationRule updateRule(Long id, UUID companyId, AutomationRule updatedRule) {
        AutomationRule existingRule = getRule(id, companyId);
        validateRule(updatedRule);

        existingRule.setName(updatedRule.getName());
        existingRule.setDescription(updatedRule.getDescription());
        existingRule.setConditions(updatedRule.getConditions());
        existingRule.setActions(updatedRule.getActions());
        existingRule.setConditionLogic(updatedRule.getConditionLogic());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setActive(updatedRule.isActive());
        existingRule.setTriggerPoint(updatedRule.getTriggerPoint());
        existingRule.setUpdatedAt(LocalDateTime.now());

        return repository.save(existingRule);
    }

    public AutomationRule getRule(Long id, UUID companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Automation rule not found"));
    }

    public Page<AutomationRule> listRules(UUID companyId, Pageable pageable) {
        return repository.findAllByCompanyId(companyId, pageable);
    }

    @Transactional
    public void deleteRule(Long id, UUID companyId) {
        AutomationRule rule = getRule(id, companyId);
        repository.delete(rule);
    }

    public List<AutomationRule> getActiveRules(UUID companyId, TriggerPoint triggerPoint) {
        return repository.findByCompanyIdAndTriggerPointAndActive(companyId, triggerPoint, true);
    }

    private void validateRule(AutomationRule rule) {
        if (rule.getName() == null || rule.getName().isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be empty");
        }
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            throw new IllegalArgumentException("Rule must have at least one condition");
        }
        if (rule.getActions() == null || rule.getActions().isEmpty()) {
            throw new IllegalArgumentException("Rule must have at least one action");
        }
        if (rule.getTriggerPoint() == null) {
            throw new IllegalArgumentException("Rule must have a trigger point");
        }
    }

    @Transactional
    public void updateExecutionStats(AutomationRule rule) {
        rule.setExecutionCount(rule.getExecutionCount() + 1);
        rule.setLastExecutedAt(LocalDateTime.now());
        repository.save(rule);
    }
}

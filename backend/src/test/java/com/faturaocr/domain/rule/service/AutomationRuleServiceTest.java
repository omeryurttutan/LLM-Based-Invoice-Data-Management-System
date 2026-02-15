package com.faturaocr.domain.rule.service;

import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.port.AutomationRuleRepository;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationRuleServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;

    @InjectMocks
    private AutomationRuleService ruleService;

    @Test
    @DisplayName("Should create rule")
    void shouldCreateRule() {
        // Given
        AutomationRule rule = new AutomationRule();
        rule.setName("Test Rule");
        rule.setConditions(List.of(new com.faturaocr.domain.rule.valueobject.RuleCondition())); // Add required
                                                                                                // condition
        rule.setActions(List.of(new com.faturaocr.domain.rule.valueobject.RuleAction())); // Add required action
        rule.setTriggerPoint(com.faturaocr.domain.rule.valueobject.TriggerPoint.ON_MANUAL_CREATE); // Add valid trigger
                                                                                                   // point

        when(ruleRepository.save(any(AutomationRule.class))).thenReturn(rule);

        // When
        AutomationRule created = ruleService.createRule(rule);

        // Then
        assertThat(created.getName()).isEqualTo("Test Rule");
        verify(ruleRepository).save(any(AutomationRule.class));
    }

    @Test
    @DisplayName("Should list rules for company")
    void shouldListRulesForCompany() {
        // Given
        UUID companyId = TestFixtures.COMPANY_ID;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<AutomationRule> page = new org.springframework.data.domain.PageImpl<>(
                List.of(new AutomationRule()));

        when(ruleRepository.findAllByCompanyId(companyId, pageable)).thenReturn(page);

        // When
        org.springframework.data.domain.Page<AutomationRule> rules = ruleService.listRules(companyId, pageable);

        // Then
        assertThat(rules).hasSize(1);
    }
}

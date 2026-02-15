package com.faturaocr.domain.rule.service;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.entity.RuleExecutionLog;
import com.faturaocr.domain.rule.port.RuleExecutionLogRepository;
import com.faturaocr.domain.rule.valueobject.ActionType;
import com.faturaocr.domain.rule.valueobject.ConditionOperator;
import com.faturaocr.domain.rule.valueobject.RuleAction;
import com.faturaocr.domain.rule.valueobject.RuleCondition;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private AutomationRuleService ruleService;

    @Mock
    private RuleExecutionLogRepository logRepository;

    @InjectMocks
    private RuleEngine ruleEngine;

    private Invoice invoice;
    private AutomationRule rule;

    @BeforeEach
    void setUp() {
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCompanyId(UUID.randomUUID());
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setSupplierName("Test Supplier");

        rule = new AutomationRule();
        rule.setId(1L);
        rule.setCompanyId(invoice.getCompanyId());
        rule.setTriggerPoint(TriggerPoint.AFTER_EXTRACTION);
        rule.setConditionLogic("AND");
        rule.setPriority(1);
        rule.setActive(true);
    }

    @Test
    void evaluateAndExecute_shouldExecuteAction_whenConditionMatches() {
        // Condition: Total Amount > 50
        RuleCondition condition = new RuleCondition();
        condition.setField("total_amount");
        condition.setOperator(ConditionOperator.GREATER_THAN);
        condition.setValue("50");
        rule.setConditions(Collections.singletonList(condition));

        // Action: Set Status to VERIFIED (Note: simplified for test, usually strict
        // checks)
        // Let's use ADD_NOTE which is safer
        RuleAction action = new RuleAction();
        action.setType(ActionType.ADD_NOTE);
        Map<String, Object> params = new HashMap<>();
        params.put("note", "Rule Applied");
        action.setParams(params);
        rule.setActions(Collections.singletonList(action));

        when(ruleService.getActiveRules(invoice.getCompanyId(), TriggerPoint.AFTER_EXTRACTION))
                .thenReturn(Collections.singletonList(rule));

        ruleEngine.evaluateAndExecute(TriggerPoint.AFTER_EXTRACTION, invoice);

        assertEquals("Rule Applied", invoice.getNotes());
        verify(logRepository).save(any(RuleExecutionLog.class));
        verify(ruleService).updateExecutionStats(rule);
    }

    @Test
    void evaluateAndExecute_shouldNotExecute_whenConditionFails() {
        // Condition: Total Amount > 200 (Invoice has 100)
        RuleCondition condition = new RuleCondition();
        condition.setField("total_amount");
        condition.setOperator(ConditionOperator.GREATER_THAN);
        condition.setValue("200");
        rule.setConditions(Collections.singletonList(condition));

        RuleAction action = new RuleAction();
        action.setType(ActionType.ADD_NOTE);
        action.setParams(Collections.singletonMap("note", "Should not run"));
        rule.setActions(Collections.singletonList(action));

        when(ruleService.getActiveRules(invoice.getCompanyId(), TriggerPoint.AFTER_EXTRACTION))
                .thenReturn(Collections.singletonList(rule));

        ruleEngine.evaluateAndExecute(TriggerPoint.AFTER_EXTRACTION, invoice);

        assertEquals(null, invoice.getNotes());
        // Should NOT save log for success? Or log failure?
        // Implementation only logs matching rules.
        // wait, executeActions is called only if evaluateConditions returns true.
        // So no logRepository.save called.
        // verify(logRepository, never()).save(any());
    }
}

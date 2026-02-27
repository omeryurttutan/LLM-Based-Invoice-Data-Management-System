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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngine {

    private final AutomationRuleService ruleService;
    private final RuleExecutionLogRepository logRepository;

    @Transactional
    public void evaluateAndExecute(TriggerPoint triggerPoint, Invoice invoice) {
        List<AutomationRule> rules = new java.util.ArrayList<>(
                ruleService.getActiveRules(invoice.getCompanyId(), triggerPoint));

        // Sort by priority (higher priority first typically, or lower? Assuming lower =
        // higher priority like 1 is first)
        // Adjust based on requirement. Let's assume 'priority' field: higher number =
        // higher priority? Or execution order?
        // Let's sort ascending (1 executing before 10).
        rules.sort((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()));

        for (AutomationRule rule : rules) {
            try {
                if (evaluateConditions(rule, invoice)) {
                    executeActions(rule, invoice);
                    logExecution(rule, invoice, "SUCCESS", null);
                    ruleService.updateExecutionStats(rule); // Update stats
                }
            } catch (Exception e) {
                log.error("Error executing rule {}: {}", rule.getId(), e.getMessage(), e);
                logExecution(rule, invoice, "FAILED", e.getMessage());
            }
        }
    }

    private boolean evaluateConditions(AutomationRule rule, Invoice invoice) {
        boolean isAndLogic = "AND".equalsIgnoreCase(rule.getConditionLogic());

        for (RuleCondition condition : rule.getConditions()) {
            boolean result = checkCondition(condition, invoice);
            if (isAndLogic && !result) {
                return false; // Fail fast for AND
            }
            if (!isAndLogic && result) {
                return true; // Succeed fast for OR
            }
        }

        return isAndLogic; // If all passed for AND, true. If none passed for OR, false.
    }

    private boolean checkCondition(RuleCondition condition, Invoice invoice) {
        Object fieldValue = getFieldValue(invoice, condition.getField());
        Object conditionValue = condition.getValue();
        ConditionOperator operator = condition.getOperator();

        return compare(fieldValue, conditionValue, operator);
    }

    private Object getFieldValue(Invoice invoice, String field) {
        // Reflection or simple switch-case mapping
        // Using switch case for safety and simplicity
        switch (field) {
            case "supplier_name":
                return invoice.getSupplierName();
            case "supplier_tax_number":
                return invoice.getSupplierTaxNumber();
            case "total_amount":
                return invoice.getTotalAmount();
            case "tax_amount":
                return invoice.getTaxAmount();
            case "invoice_date":
                return invoice.getInvoiceDate();
            case "currency":
                return invoice.getCurrency() != null ? invoice.getCurrency().toString() : null;
            case "category_id":
                return invoice.getCategoryId() != null ? invoice.getCategoryId().toString() : null;
            case "notes":
                return invoice.getNotes();
            default:
                return null;
        }
    }

    private boolean compare(Object actual, Object expected, ConditionOperator operator) {
        if (operator == ConditionOperator.IS_NULL)
            return actual == null;
        if (operator == ConditionOperator.IS_NOT_NULL)
            return actual != null;

        if (actual == null && expected != null)
            return false;
        // if expected is null and not Checked IS_NULL, usually false unless equals null

        // Convert expected to match actual type if possible?
        // Assuming values in JSONB are Strings or Numbers.
        // Need type handling.

        // Simplified Logic:
        String actualStr = String.valueOf(actual);
        String expectedStr = String.valueOf(expected);

        switch (operator) {
            case EQUALS:
                return actualStr.equals(expectedStr); // Weak typing
            case NOT_EQUALS:
                return !actualStr.equals(expectedStr);
            case CONTAINS:
                return actualStr.contains(expectedStr);
            case NOT_CONTAINS:
                return !actualStr.contains(expectedStr);
            case GREATER_THAN:
                return compareNumbers(actual, expected) > 0;
            case LESS_THAN:
                return compareNumbers(actual, expected) < 0;
            // ... other operators
            default:
                return false;
        }
    }

    private int compareNumbers(Object n1, Object n2) {
        // Safe conversion to BigDecimal for comparison
        BigDecimal b1 = toBigDecimal(n1);
        BigDecimal b2 = toBigDecimal(n2);
        return b1.compareTo(b2);
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal)
            return (BigDecimal) o;
        if (o instanceof Number)
            return new BigDecimal(o.toString());
        if (o instanceof String)
            return new BigDecimal((String) o);
        return BigDecimal.ZERO;
    }

    private void executeActions(AutomationRule rule, Invoice invoice) {
        for (RuleAction action : rule.getActions()) {
            switch (action.getType()) {
                case SET_CATEGORY:
                    String catId = (String) action.getParams().get("category_id");
                    if (catId != null)
                        invoice.setCategoryId(UUID.fromString(catId));
                    break;
                case SET_STATUS:
                    String status = (String) action.getParams().get("status");
                    if (status != null)
                        invoice.setStatus(InvoiceStatus.valueOf(status));
                    break;
                case ADD_NOTE:
                    String note = (String) action.getParams().get("note");
                    if (note != null) {
                        String current = invoice.getNotes();
                        invoice.setNotes(current == null ? note : current + "\n" + note);
                    }
                    break;
                case FLAG_FOR_REVIEW:
                    invoice.setStatus(InvoiceStatus.REVIEW_REQUIRED);
                    String reviewNote = (String) action.getParams().get("note");
                    if (reviewNote != null) {
                        String current = invoice.getNotes();
                        invoice.setNotes(current == null ? reviewNote : current + "\n[Flagged]: " + reviewNote);
                    }
                    break;
                case SEND_NOTIFICATION:
                    // Notification logic is separate or needs NotificationService injection
                    // For now, logged or ignored as implementation complexity for notification
                    // service injection in RuleEngine might be high
                    // Adding simple log note
                    String notifMsg = (String) action.getParams().get("message");
                    if (notifMsg != null) {
                        // potential todo: inject NotificationService
                        log.info("Rule Notification: {}", notifMsg);
                    }
                    break;
                case SET_PRIORITY:
                    // Priority field not on Invoice. Log warning.
                    log.warn("SET_PRIORITY action skipped: 'priority' field missing on Invoice entity");
                    break;
            }
        }
    }

    private void logExecution(AutomationRule rule, Invoice invoice, String result, String error) {
        RuleExecutionLog log = new RuleExecutionLog();
        log.setRuleId(rule.getId());
        log.setInvoiceId(invoice.getId());
        log.setCompanyId(invoice.getCompanyId());
        log.setTriggerPoint(rule.getTriggerPoint());
        log.setConditionsMatched(rule.getConditions()); // Actually track which ones matched?
        log.setActionsApplied(rule.getActions());
        log.setExecutionResult(result);
        log.setErrorMessage(error);
        logRepository.save(log);
    }
}

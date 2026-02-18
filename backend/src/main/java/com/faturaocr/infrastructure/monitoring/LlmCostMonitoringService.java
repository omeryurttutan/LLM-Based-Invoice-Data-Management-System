package com.faturaocr.infrastructure.monitoring;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;
import com.faturaocr.domain.monitoring.port.LlmApiUsageRepository;
import com.faturaocr.infrastructure.alerting.AlertService;
import com.faturaocr.infrastructure.alerting.AlertSeverity;
import com.faturaocr.infrastructure.alerting.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmCostMonitoringService {

    private final LlmApiUsageRepository usageRepository;
    private final AlertService alertService;

    @Value("${app.llm.budget.monthly-limit-usd:50.0}")
    private BigDecimal monthlyLimitUsd;

    @Value("${app.llm.budget.daily-limit-usd:5.0}")
    private BigDecimal dailyLimitUsd;

    @Value("${app.llm.budget.alert-threshold-percent:80}")
    private int alertThresholdPercent;

    @Transactional
    public void recordUsage(LlmApiUsage usage) {
        usageRepository.save(usage);

        checkBudget(usage.getCompanyId());
    }

    private void checkBudget(UUID companyId) {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthlyCost = getMonthlyUsage(companyId, currentMonth);

        BigDecimal threshold = monthlyLimitUsd.multiply(BigDecimal.valueOf(alertThresholdPercent))
                .divide(BigDecimal.valueOf(100));

        if (monthlyCost.compareTo(monthlyLimitUsd) >= 0) {
            alertService.sendAlert(
                    AlertType.LLM_BUDGET_EXCEEDED,
                    AlertSeverity.CRITICAL,
                    "Monthly LLM Budget Exceeded",
                    String.format("Company %s has exceeded the monthly budget. Current: $%s, Limit: $%s", companyId,
                            monthlyCost, monthlyLimitUsd));
        } else if (monthlyCost.compareTo(threshold) >= 0) {
            alertService.sendAlert(
                    AlertType.LLM_BUDGET_WARNING,
                    AlertSeverity.WARN,
                    "Monthly LLM Budget Threshold Reached",
                    String.format("Company %s has reached %d%% of the monthly budget. Current: $%s, Limit: $%s",
                            companyId, alertThresholdPercent, monthlyCost, monthlyLimitUsd));
        }

        BigDecimal dailyCost = getDailyUsage(companyId, LocalDate.now());
        if (dailyCost.compareTo(dailyLimitUsd) >= 0) {
            alertService.sendAlert(
                    AlertType.LLM_DAILY_LIMIT,
                    AlertSeverity.HIGH,
                    "Daily LLM Limit Exceeded",
                    String.format("Company %s has exceeded the daily limit. Current: $%s, Limit: $%s", companyId,
                            dailyCost, dailyLimitUsd));
        }
    }

    public BigDecimal getMonthlyUsage(UUID companyId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<LlmApiUsage> usages = usageRepository.findByCompanyIdAndCreatedAtBetween(companyId, start.atStartOfDay(),
                end.atTime(23, 59, 59));

        return usages.stream()
                .map(LlmApiUsage::getEstimatedCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getDailyUsage(UUID companyId, LocalDate date) {
        List<LlmApiUsage> usages = usageRepository.findByCompanyIdAndCreatedAtBetween(companyId, date.atStartOfDay(),
                date.atTime(23, 59, 59));

        return usages.stream()
                .map(LlmApiUsage::getEstimatedCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

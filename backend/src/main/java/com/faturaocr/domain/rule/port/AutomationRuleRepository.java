package com.faturaocr.domain.rule.port;

import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomationRuleRepository {
    AutomationRule save(AutomationRule rule);

    Optional<AutomationRule> findByIdAndCompanyId(Long id, UUID companyId);

    Page<AutomationRule> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<AutomationRule> findByCompanyIdAndTriggerPointAndActive(UUID companyId, TriggerPoint triggerPoint,
            boolean active);

    void delete(AutomationRule rule);
}

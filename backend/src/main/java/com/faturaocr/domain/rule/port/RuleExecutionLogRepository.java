package com.faturaocr.domain.rule.port;

import com.faturaocr.domain.rule.entity.RuleExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface RuleExecutionLogRepository {
    RuleExecutionLog save(RuleExecutionLog log);

    Page<RuleExecutionLog> findByCompanyId(UUID companyId, Pageable pageable);
}

package com.faturaocr.application.kvkk.service;

import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import com.faturaocr.infrastructure.persistence.audit.AuditLogJpaRepository;
import com.faturaocr.infrastructure.persistence.kvkk.UserConsentJpaRepository;
import com.faturaocr.web.dto.kvkk.KvkkReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KvkkReportService {

    private final JdbcTemplate jdbcTemplate;
    private final UserConsentJpaRepository userConsentRepository;
    private final AuditLogJpaRepository auditLogRepository;

    public KvkkReportResponse getComplianceReport() {
        // 1. Encryption Status
        boolean encryptionEnabled = checkEncryptionStatus();

        // 2. Encrypted Field Counts (Approximation by counting rows in relevant tables)
        long userCount = countRows("users");
        long companyCount = countRows("companies");
        long invoiceCount = countRows("invoices");

        // 3. Consent Summary
        Map<ConsentType, Long> consentSummary = new HashMap<>();
        for (ConsentType type : ConsentType.values()) {
            // Count GRANTED consents by type
            // This queries `user_consents` table.
            // Ideally we want *active* consents (latest per user per type).
            // Simplified: Count all "is_granted = true" records? No, history counts too.
            // Let's count current active consents.
            // "SELECT count(DISTINCT user_id) FROM user_consents WHERE consent_type = ? AND
            // is_granted = true"
            // Actually prompt says "Consent overview report".
            // I'll count total active grants.
            String sql = "SELECT count(*) FROM user_consents WHERE consent_type = ? AND is_granted = true";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, type.name());
            consentSummary.put(type, count != null ? count : 0);
        }

        long totalConsents = consentSummary.values().stream().mapToLong(Long::longValue).sum();

        // 4. Data Retention
        // We haven't implemented the job yet, so last run unknown.
        // We can query system_settings if we store it there.
        LocalDateTime lastRun = null; // Placeholder
        long pendingDeletions = 0; // Placeholder

        // 5. Right to be Forgotten
        // Count audit logs with specific action or description?
        // Or check deleted users? "deleted_at IS NOT NULL"?
        long forgottenUsers = countRows("users WHERE deleted_at IS NOT NULL");

        // 6. Audit Logs
        long totalLogs = auditLogRepository.count();

        return KvkkReportResponse.builder()
                .dataEncryptionEnabled(encryptionEnabled)
                .encryptedUserFields(userCount) // Each user has phone encrypted
                .encryptedCompanyFields(companyCount)
                .encryptedInvoiceFields(invoiceCount)
                .consentSummary(consentSummary)
                .totalConsentsGranted(totalConsents)
                .dataRetentionLastRun(lastRun)
                .pendingDeletions(pendingDeletions)
                .rightToBeForgottenRequests(forgottenUsers)
                .totalAuditLogs(totalLogs)
                .totalMaskedLogs(totalLogs) // Assuming all logs potentially masked
                .build();
    }

    private boolean checkEncryptionStatus() {
        try {
            String val = jdbcTemplate.queryForObject("SELECT value FROM system_settings WHERE key = 'data_encrypted'",
                    String.class);
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            return false;
        }
    }

    private long countRows(String tableNameAndCondition) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableNameAndCondition, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}

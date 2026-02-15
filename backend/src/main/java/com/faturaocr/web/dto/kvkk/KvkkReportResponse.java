package com.faturaocr.web.dto.kvkk;

import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class KvkkReportResponse {
    private boolean dataEncryptionEnabled;
    private long encryptedUserFields; // Estimated
    private long encryptedCompanyFields;
    private long encryptedInvoiceFields;

    private Map<ConsentType, Long> consentSummary; // Count per type
    private long totalConsentsGranted;

    private LocalDateTime dataRetentionLastRun;
    private long pendingDeletions;

    private long rightToBeForgottenRequests; // Completed requests

    private long totalAuditLogs;
    private long totalMaskedLogs; // Hard to count, maybe just total logs
}

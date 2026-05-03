package com.faturaocr.application.kvkk.job;

import com.faturaocr.infrastructure.persistence.audit.AuditLogJpaRepository;
import com.faturaocr.infrastructure.persistence.company.CompanyJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.user.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetentionJobTest {

    @Mock
    private InvoiceJpaRepository invoiceRepository;
    @Mock
    private CompanyJpaRepository companyRepository;
    @Mock
    private UserJpaRepository userRepository;
    @Mock
    private AuditLogJpaRepository auditLogRepository;

    @InjectMocks
    private RetentionJob retentionJob;

    @Test
    @DisplayName("Should do nothing when disabled")
    void shouldDoNothingWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(retentionJob, "enabled", false);

        // When
        retentionJob.runRetentionPolicy();

        // Then
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("Should run retention policy when enabled")
    void shouldRunRetentionPolicyWhenEnabled() {
        // Given
        ReflectionTestUtils.setField(retentionJob, "enabled", true);
        ReflectionTestUtils.setField(retentionJob, "invoiceRetentionYears", 10);
        ReflectionTestUtils.setField(retentionJob, "auditLogRetentionYears", 2);

        // When
        retentionJob.runRetentionPolicy();

        // Then
        // Currently the job only logs, but structure is there.
        // If logic is added to call repository, we verify it here.
        // For now, valid execution implies no exceptions.
    }
}

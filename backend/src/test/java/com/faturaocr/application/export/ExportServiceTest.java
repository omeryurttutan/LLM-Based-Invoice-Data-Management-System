package com.faturaocr.application.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.infrastructure.persistence.category.CategoryJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceMapper;
import com.faturaocr.infrastructure.persistence.user.UserJpaRepository;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private InvoiceJpaRepository invoiceRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); // Use real ObjectMapper

    @Mock
    private InvoiceExporter xlsxExporter;

    @Mock
    private InvoiceExporter csvExporter;

    // We need to inject the list of exporters
    // InjectMocks won't inject List automatically if generic?
    // Let's set it manually in setUp
    private ExportService exportService;

    private MockedStatic<CompanyContextHolder> companyContextMock;
    private final UUID COMPANY_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        exportService = new ExportService(
                List.of(xlsxExporter, csvExporter),
                invoiceRepository,
                invoiceMapper,
                userRepository,
                categoryRepository,
                auditLogRepository,
                objectMapper);

        // Setup exporters
        lenient().when(xlsxExporter.getFormat()).thenReturn(ExportFormat.XLSX);
        lenient().when(csvExporter.getFormat()).thenReturn(ExportFormat.CSV);

        // Mock Static Contexts
        companyContextMock = mockStatic(CompanyContextHolder.class);
        companyContextMock.when(CompanyContextHolder::getCompanyId).thenReturn(COMPANY_ID);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(USER_ID, "admin@test.com", COMPANY_ID, "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null,
                Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        companyContextMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportInvoices_ShouldUseCorrectExporter_WhenFormatProvided() throws IOException {
        // Arrange
        Specification<InvoiceJpaEntity> spec = (Specification<InvoiceJpaEntity>) mock(Specification.class);
        OutputStream outputStream = mock(OutputStream.class);

        when(invoiceRepository.count(spec)).thenReturn(50L);
        // Mock page iterator calls inside export?
        // Actually export calls exporter.export(iterable, stream)
        // We just need to verify exporter.export is called.

        // Act
        exportService.exportInvoices(ExportFormat.XLSX, spec, false, outputStream);

        // Assert
        verify(xlsxExporter).export(any(), eq(outputStream));
        verify(csvExporter, never()).export(any(), any());

        // Verify Audit Log
        verify(auditLogRepository).save(argThat(log -> log.getActionType() == AuditActionType.EXPORT &&
                "INVOICE".equals(log.getEntityType()) &&
                "admin@test.com".equals(log.getUserEmail()) // Match email set in setUp
        ));
    }

    @Test
    void exportInvoices_ShouldThrowException_WhenCountExceedsLimit() {
        // Arrange
        Specification<InvoiceJpaEntity> spec = (Specification<InvoiceJpaEntity>) mock(Specification.class);
        when(invoiceRepository.count(spec)).thenReturn(50001L); // Limit is 50000

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> exportService.exportInvoices(ExportFormat.CSV, spec, false, mock(OutputStream.class)));

        verifyNoInteractions(csvExporter);
        verifyNoInteractions(auditLogRepository);
    }
}

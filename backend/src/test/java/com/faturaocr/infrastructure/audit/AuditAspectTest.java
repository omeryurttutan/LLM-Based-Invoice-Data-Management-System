package com.faturaocr.infrastructure.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.audit.annotation.Auditable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.faturaocr.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditEntityLoader entityLoader;

    private AuditSerializer serializer;
    private AuditAspect auditAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Auditable auditable;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "user@example.com";
    private final UUID testCompanyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        serializer = new AuditSerializer();
        auditAspect = new AuditAspect(auditLogRepository, entityLoader, serializer);

        // Mock security context
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(testUserId, testEmail, testCompanyId, "ADMIN");
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock audit log save
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // Mock joinPoint signature
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    void shouldSaveAuditLogForCreateAction() throws Throwable {
        // Arrange
        when(auditable.action()).thenReturn(AuditActionType.CREATE);
        when(auditable.entityType()).thenReturn("COMPANY");
        when(auditable.description()).thenReturn("");

        // CREATE: no old state loaded, entity ID from result
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        TestResponse result = new TestResponse(UUID.randomUUID());
        when(joinPoint.proceed()).thenReturn(result);

        // Act
        Object returnValue = auditAspect.audit(joinPoint, auditable);

        // Assert
        assertSame(result, returnValue);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(AuditActionType.CREATE, saved.getActionType());
        assertEquals("COMPANY", saved.getEntityType());
        assertEquals(testUserId, saved.getUserId());
        assertEquals(testEmail, saved.getUserEmail());
        assertNull(saved.getOldValue()); // No old state for CREATE
        assertNotNull(saved.getNewValue()); // New state captured
    }

    @Test
    void shouldLoadOldStateForUpdateAction() throws Throwable {
        // Arrange
        UUID entityId = UUID.randomUUID();
        when(auditable.action()).thenReturn(AuditActionType.UPDATE);
        when(auditable.entityType()).thenReturn("COMPANY");
        when(auditable.description()).thenReturn("");
        when(joinPoint.getArgs()).thenReturn(new Object[] { entityId });

        Company oldCompany = new Company("OldName", "1234567890");
        when(entityLoader.loadEntity("COMPANY", entityId)).thenReturn(oldCompany);

        TestResponse result = new TestResponse(entityId);
        when(joinPoint.proceed()).thenReturn(result);

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(entityLoader).loadEntity("COMPANY", entityId);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(AuditActionType.UPDATE, saved.getActionType());
        assertNotNull(saved.getOldValue());
        assertNotNull(saved.getNewValue());
        assertEquals(entityId, saved.getEntityId());
    }

    @Test
    void shouldNotSaveAuditLogOnException() throws Throwable {
        // Arrange
        when(auditable.action()).thenReturn(AuditActionType.DELETE);
        when(auditable.entityType()).thenReturn("COMPANY");
        when(auditable.description()).thenReturn("");
        when(joinPoint.getArgs()).thenReturn(new Object[]{UUID.randomUUID()});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> auditAspect.audit(joinPoint, auditable));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void shouldExtractEntityIdFromArgs() throws Throwable {
        // Arrange
        UUID entityId = UUID.randomUUID();
        when(auditable.action()).thenReturn(AuditActionType.DELETE);
        when(auditable.entityType()).thenReturn("COMPANY");
        when(auditable.description()).thenReturn("");
        when(joinPoint.getArgs()).thenReturn(new Object[] { entityId });
        when(entityLoader.loadEntity("COMPANY", entityId)).thenReturn(null);
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(entityId, captor.getValue().getEntityId());
    }

    /**
     * Helper class simulating a response with getId().
     */
    static class TestResponse {
        private final UUID id;

        TestResponse(UUID id) {
            this.id = id;
        }

        public UUID getId() {
            return id;
        }
    }
}

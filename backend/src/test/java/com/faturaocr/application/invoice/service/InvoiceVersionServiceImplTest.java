package com.faturaocr.application.invoice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.domain.invoice.dto.InvoiceVersionDto;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceVersion;
import com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource;
import com.faturaocr.domain.invoice.repository.InvoiceVersionRepository;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceVersionServiceImplTest {

    @Mock
    private InvoiceVersionRepository versionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InvoiceVersionServiceImpl versionService;

    private Invoice invoice;
    private User user;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCompanyId(companyId);
        invoice.setInvoiceNumber("INV-001");
        invoice.setTotalAmount(BigDecimal.TEN);

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .fullName("Test User")
                .companyId(companyId)
                .passwordHash("hash")
                .build();
    }

    @Test
    void createSnapshot_ShouldSaveVersion_WhenCalled() {
        // Arrange
        when(versionRepository.findMaxVersionNumberByInvoiceId(invoice.getId())).thenReturn(Optional.of(0));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));

        JsonNode mockJson = mock(JsonNode.class);
        when(objectMapper.valueToTree(any())).thenReturn(mockJson);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            // Act
            versionService.createSnapshot(invoice, new ArrayList<>(), ChangeSource.MANUAL_EDIT, "Test Change");

            // Assert
            verify(versionRepository).save(any(InvoiceVersion.class));
        }
    }

    @Test
    void getVersions_ShouldReturnList_WhenVersionsExist() {
        // Arrange
        InvoiceVersion version = InvoiceVersion.builder()
                .id(UUID.randomUUID())
                .versionNumber(1)
                .changeSource(ChangeSource.MANUAL_EDIT)
                .changedBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(versionRepository.findByInvoiceIdOrderByVersionNumberDesc(invoice.getId()))
                .thenReturn(List.of(version));

        // Act
        List<InvoiceVersionDto.Summary> result = versionService.getVersions(invoice.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getVersionNumber());
        assertEquals("test@example.com", result.get(0).getChangedBy().getEmail());
    }

    @Test
    void cleanupOldVersions_ShouldDeleteOldest_WhenLimitExceeded() {
        // Arrange
        List<InvoiceVersion> versions = new ArrayList<>();
        for (int i = 1; i <= 55; i++) {
            versions.add(InvoiceVersion.builder().versionNumber(i).build());
        }

        // Mock findMaxVersionNumber
        when(versionRepository.findMaxVersionNumberByInvoiceId(invoice.getId())).thenReturn(Optional.of(54));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        // Mock cleanup query
        // cleanup is called inside createSnapshot, which calls findVersionsByInvoiceId
        when(versionRepository.findVersionsByInvoiceId(invoice.getId())).thenReturn(versions);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            // Act
            versionService.createSnapshot(invoice, new ArrayList<>(), ChangeSource.MANUAL_EDIT, "Test");

            // Assert
            verify(versionRepository).deleteAll(anyList());
        }
    }
}

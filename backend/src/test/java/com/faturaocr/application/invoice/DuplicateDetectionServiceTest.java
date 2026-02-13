package com.faturaocr.application.invoice;

import com.faturaocr.application.invoice.dto.DuplicateCheckRequest;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.DuplicateMatch;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DuplicateDetectionServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private UUID companyId;
    private DuplicateCheckRequest baseRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        companyId = UUID.randomUUID();
        baseRequest = DuplicateCheckRequest.builder()
                .invoiceNumber("INV-001")
                .invoiceDate(LocalDate.now())
                .totalAmount(new BigDecimal("100.00"))
                .supplierName("Test Supplier")
                .supplierTaxNumber("1234567890")
                .companyId(companyId)
                .build();
    }

    @Test
    void checkForDuplicates_NoDuplicates_ReturnsSuccess() {
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted(anyString(), any(UUID.class)))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findBySupplierTaxNumberAndDateAndAmountAndCompanyId(anyString(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceRepository.findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(anyString(), any(), any(),
                any(), any(), any()))
                .thenReturn(Collections.emptyList());

        DuplicateCheckResult result = duplicateDetectionService.checkForDuplicates(baseRequest);

        assertFalse(result.isHasDuplicates());
        assertEquals(DuplicateConfidence.NONE, result.getHighestConfidence());
        assertTrue(result.getDuplicates().isEmpty());
    }

    @Test
    void checkForDuplicates_Level1ExactMatch_ReturnsHighConfidence() {
        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(UUID.randomUUID());
        existingInvoice.setInvoiceNumber("INV-001");
        existingInvoice.setCompanyId(companyId);
        existingInvoice.setStatus(InvoiceStatus.VERIFIED);

        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted("INV-001", companyId))
                .thenReturn(Optional.of(existingInvoice));

        DuplicateCheckResult result = duplicateDetectionService.checkForDuplicates(baseRequest);

        assertTrue(result.isHasDuplicates());
        assertEquals(DuplicateConfidence.HIGH, result.getHighestConfidence());
        assertEquals(1, result.getDuplicates().size());
        DuplicateMatch match = result.getDuplicates().get(0);
        assertEquals(DuplicateConfidence.HIGH, match.getConfidence());
        assertEquals("INV-001", match.getInvoiceNumber());
    }

    @Test
    void checkForDuplicates_Level2StrongMatch_ReturnsMediumConfidence() {
        // Level 1 yields empty
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted(anyString(), any()))
                .thenReturn(Optional.empty());

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(UUID.randomUUID());
        existingInvoice.setInvoiceNumber("INV-DIFFERENT"); // Different number matches Level 2 logic
        existingInvoice.setSupplierTaxNumber("1234567890");
        existingInvoice.setInvoiceDate(baseRequest.getInvoiceDate());
        existingInvoice.setTotalAmount(baseRequest.getTotalAmount());
        existingInvoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findBySupplierTaxNumberAndDateAndAmountAndCompanyId(
                eq("1234567890"), eq(baseRequest.getInvoiceDate()), eq(baseRequest.getTotalAmount()), eq(companyId)))
                .thenReturn(List.of(existingInvoice));

        DuplicateCheckResult result = duplicateDetectionService.checkForDuplicates(baseRequest);

        assertTrue(result.isHasDuplicates());
        assertEquals(DuplicateConfidence.MEDIUM, result.getHighestConfidence());
        assertEquals(1, result.getDuplicates().size());
        assertEquals(DuplicateConfidence.MEDIUM, result.getDuplicates().get(0).getConfidence());
        assertEquals("INV-DIFFERENT", result.getDuplicates().get(0).getInvoiceNumber());
    }

    @Test
    void checkForDuplicates_Level3FuzzyMatch_ReturnsLowConfidence() {
        // Level 1 and 2 yield empty
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted(anyString(), any()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findBySupplierTaxNumberAndDateAndAmountAndCompanyId(anyString(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(UUID.randomUUID());
        existingInvoice.setInvoiceNumber("INV-FUZZY");
        existingInvoice.setSupplierName("Test Supplier Inc."); // Slightly different name
        existingInvoice.setInvoiceDate(baseRequest.getInvoiceDate());
        existingInvoice.setTotalAmount(new BigDecimal("100.50")); // Within 1% tolerance
        existingInvoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(
                anyString(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(existingInvoice));

        DuplicateCheckResult result = duplicateDetectionService.checkForDuplicates(baseRequest);

        assertTrue(result.isHasDuplicates());
        assertEquals(DuplicateConfidence.LOW, result.getHighestConfidence());
        assertEquals(1, result.getDuplicates().size());
        assertEquals(DuplicateConfidence.LOW, result.getDuplicates().get(0).getConfidence());
    }

    @Test
    void checkForDuplicates_ExcludeSelf_ReturnsNoDuplicates() {
        UUID excludeId = UUID.randomUUID();
        baseRequest.setExcludeInvoiceId(excludeId);

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(excludeId);
        existingInvoice.setInvoiceNumber("INV-001");

        // Mock returns the SAME invoice that should be excluded
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted("INV-001", companyId))
                .thenReturn(Optional.of(existingInvoice));

        // Ensure other levels are empty
        when(invoiceRepository.findBySupplierTaxNumberAndDateAndAmountAndCompanyId(anyString(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceRepository.findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(anyString(), any(), any(),
                any(), any(), any()))
                .thenReturn(Collections.emptyList());

        DuplicateCheckResult result = duplicateDetectionService.checkForDuplicates(baseRequest);

        assertFalse(result.isHasDuplicates()); // Should be false because we excluded the only match
    }
}

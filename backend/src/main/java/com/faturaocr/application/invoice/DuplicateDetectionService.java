package com.faturaocr.application.invoice;

import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.application.invoice.dto.DuplicateCheckRequest;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.DuplicateMatch;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standalone service for detecting potential duplicate invoices.
 * Uses three-level matching strategy with different confidence levels.
 * Designed to be reusable from manual creation, LLM pipeline, e-invoice import,
 * etc.
 */
@ApplicationService
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Check for potential duplicates against existing invoices.
     *
     * @param request the duplicate check request containing invoice details
     * @return result with any matching duplicates and their confidence levels
     */
    public DuplicateCheckResult checkForDuplicates(DuplicateCheckRequest request) {
        Map<UUID, DuplicateMatch> matchMap = new LinkedHashMap<>();

        // Level 1: Exact invoice number match (HIGH confidence)
        checkLevel1ExactMatch(request, matchMap);

        // Level 2: Strong match — supplier tax + date + amount (MEDIUM confidence)
        checkLevel2StrongMatch(request, matchMap);

        // Level 3: Fuzzy match — name + date + ~amount (LOW confidence)
        checkLevel3FuzzyMatch(request, matchMap);

        List<DuplicateMatch> duplicates = new ArrayList<>(matchMap.values());
        if (duplicates.isEmpty()) {
            return DuplicateCheckResult.noDuplicates();
        }

        // Determine highest confidence
        DuplicateConfidence highest = duplicates.stream()
                .map(DuplicateMatch::getConfidence)
                .min(Comparator.comparingInt(DuplicateConfidence::ordinal))
                .orElse(DuplicateConfidence.NONE);

        return DuplicateCheckResult.builder()
                .hasDuplicates(true)
                .duplicates(duplicates)
                .highestConfidence(highest)
                .build();
    }

    /**
     * Level 1: Exact invoice number match within the same company.
     * Confidence: HIGH
     */
    private void checkLevel1ExactMatch(DuplicateCheckRequest request, Map<UUID, DuplicateMatch> matchMap) {
        if (!StringUtils.hasText(request.getInvoiceNumber())) {
            return;
        }

        invoiceRepository.findByInvoiceNumberAndCompanyIdAndNotDeleted(
                request.getInvoiceNumber(), request.getCompanyId()).ifPresent(invoice -> {
                    if (shouldInclude(invoice.getId(), request.getExcludeInvoiceId())) {
                        matchMap.put(invoice.getId(), toMatch(invoice, DuplicateConfidence.HIGH,
                                "Exact invoice number match within your company"));
                    }
                });
    }

    /**
     * Level 2: Same supplier tax number + same date + same total amount.
     * Confidence: MEDIUM
     */
    private void checkLevel2StrongMatch(DuplicateCheckRequest request, Map<UUID, DuplicateMatch> matchMap) {
        if (!StringUtils.hasText(request.getSupplierTaxNumber()) ||
                request.getInvoiceDate() == null ||
                request.getTotalAmount() == null) {
            return;
        }

        List<Invoice> matches = invoiceRepository.findBySupplierTaxNumberAndDateAndAmountAndCompanyId(
                request.getSupplierTaxNumber(), request.getInvoiceDate(),
                request.getTotalAmount(), request.getCompanyId());

        for (Invoice invoice : matches) {
            if (shouldInclude(invoice.getId(), request.getExcludeInvoiceId()) &&
                    !matchMap.containsKey(invoice.getId())) {
                matchMap.put(invoice.getId(), toMatch(invoice, DuplicateConfidence.MEDIUM,
                        "Same supplier tax number, date, and amount"));
            }
        }
    }

    /**
     * Level 3: Fuzzy match — same supplier name (case-insensitive) + same date +
     * amount within ±1%.
     * Confidence: LOW
     */
    private void checkLevel3FuzzyMatch(DuplicateCheckRequest request, Map<UUID, DuplicateMatch> matchMap) {
        if (!StringUtils.hasText(request.getSupplierName()) ||
                request.getInvoiceDate() == null ||
                request.getTotalAmount() == null) {
            return;
        }

        BigDecimal totalAmount = request.getTotalAmount();
        BigDecimal tolerance = totalAmount.abs().multiply(new BigDecimal("0.01"));
        if (tolerance.compareTo(BigDecimal.ONE) < 0) {
            tolerance = BigDecimal.ONE;
        }
        BigDecimal minAmount = totalAmount.subtract(tolerance);
        BigDecimal maxAmount = totalAmount.add(tolerance);

        List<Invoice> matches = invoiceRepository
                .findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(
                        request.getSupplierName(), request.getInvoiceDate(),
                        minAmount, maxAmount, request.getCompanyId(),
                        request.getExcludeInvoiceId());

        for (Invoice invoice : matches) {
            if (shouldInclude(invoice.getId(), request.getExcludeInvoiceId()) &&
                    !matchMap.containsKey(invoice.getId())) {
                matchMap.put(invoice.getId(), toMatch(invoice, DuplicateConfidence.LOW,
                        "Similar supplier name, same date, and similar amount (±1%)"));
            }
        }
    }

    private boolean shouldInclude(UUID invoiceId, UUID excludeId) {
        return excludeId == null || !excludeId.equals(invoiceId);
    }

    private DuplicateMatch toMatch(Invoice invoice, DuplicateConfidence confidence, String reason) {
        return DuplicateMatch.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .supplierName(invoice.getSupplierName())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .confidence(confidence)
                .matchReason(reason)
                .build();
    }
}

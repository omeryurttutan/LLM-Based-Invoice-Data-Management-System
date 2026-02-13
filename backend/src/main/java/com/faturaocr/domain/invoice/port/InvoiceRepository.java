package com.faturaocr.domain.invoice.port;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Invoice> findAllByCompanyId(UUID companyId, Pageable pageable);

    Page<Invoice> findAllByCompanyIdAndStatus(UUID companyId, InvoiceStatus status, Pageable pageable);

    boolean existsByInvoiceNumberAndCompanyId(String invoiceNumber, UUID companyId);

    void softDelete(UUID id);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, InvoiceStatus status);

    long countByCategoryId(UUID categoryId);

    // Duplicate detection queries
    Optional<Invoice> findByInvoiceNumberAndCompanyIdAndNotDeleted(String invoiceNumber, UUID companyId);

    List<Invoice> findBySupplierTaxNumberAndDateAndAmountAndCompanyId(
            String supplierTaxNumber, LocalDate date, BigDecimal amount, UUID companyId);

    List<Invoice> findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(
            String supplierName, LocalDate date, BigDecimal minAmount, BigDecimal maxAmount,
            UUID companyId, UUID excludeId);
}

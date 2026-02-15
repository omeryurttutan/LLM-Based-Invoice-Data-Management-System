package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository invoiceJpaRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = invoiceMapper.toJpa(invoice);
        InvoiceJpaEntity savedEntity = invoiceJpaRepository.save(entity);
        return invoiceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return invoiceJpaRepository.findById(id)
                .map(invoiceMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndCompanyId(UUID id, UUID companyId) {
        return invoiceJpaRepository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .map(invoiceMapper::toDomain);
    }

    @Override
    public Page<Invoice> findAllByCompanyId(UUID companyId, Pageable pageable) {
        return invoiceJpaRepository.findAllByCompanyIdAndIsDeletedFalse(companyId, pageable)
                .map(invoiceMapper::toDomain);
    }

    @Override
    public Page<Invoice> findAllByCompanyIdAndStatus(UUID companyId, InvoiceStatus status, Pageable pageable) {
        return invoiceJpaRepository.findAllByCompanyIdAndStatusAndIsDeletedFalse(companyId, status, pageable)
                .map(invoiceMapper::toDomain);
    }

    @Override
    public boolean existsByInvoiceNumberAndCompanyId(String invoiceNumber, UUID companyId) {
        return invoiceJpaRepository.existsByInvoiceNumberAndCompanyId(invoiceNumber, companyId);
    }

    @Override
    public void softDelete(UUID id) {
        invoiceJpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(java.time.LocalDateTime.now());
            invoiceJpaRepository.save(entity);
        });
    }

    @Override
    public long countByCompanyId(UUID companyId) {
        return invoiceJpaRepository.countByCompanyIdAndIsDeletedFalse(companyId);
    }

    @Override
    public long countByCompanyIdAndStatus(UUID companyId, InvoiceStatus status) {
        return invoiceJpaRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, status);
    }

    @Override
    public long countByCategoryId(UUID categoryId) {
        return invoiceJpaRepository.countByCategoryIdAndIsDeletedFalse(categoryId);
    }

    // ===== Duplicate Detection =====

    @Override
    public Optional<Invoice> findByInvoiceNumberAndCompanyIdAndNotDeleted(String invoiceNumber, UUID companyId) {
        return invoiceJpaRepository.findByInvoiceNumberAndCompanyIdAndIsDeletedFalse(invoiceNumber, companyId)
                .map(invoiceMapper::toDomain);
    }

    @Override
    public List<Invoice> findBySupplierTaxNumberAndDateAndAmountAndCompanyId(
            String supplierTaxNumber, LocalDate date, BigDecimal amount, UUID companyId) {
        return invoiceJpaRepository
                .findBySupplierTaxNumberAndInvoiceDateAndTotalAmountAndCompanyIdAndIsDeletedFalse(
                        supplierTaxNumber, date, amount, companyId)
                .stream()
                .map(invoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findPotentialDuplicatesBySupplierNameAndDateAndAmountRange(
            String supplierName, LocalDate date, BigDecimal minAmount, BigDecimal maxAmount,
            UUID companyId, UUID excludeId) {
        return invoiceJpaRepository
                .findPotentialDuplicatesBySupplierAndDateAndAmountRange(
                        supplierName, date, minAmount, maxAmount, companyId, excludeId)
                .stream()
                .map(invoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findByBatchId(UUID batchId) {
        return invoiceJpaRepository.findByBatchId(batchId)
                .stream()
                .map(invoiceMapper::toDomain)
                .collect(Collectors.toList());
    }
}

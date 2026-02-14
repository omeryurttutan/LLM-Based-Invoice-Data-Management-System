package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    Optional<InvoiceJpaEntity> findByIdAndCompanyIdAndIsDeletedFalse(UUID id, UUID companyId);

    Page<InvoiceJpaEntity> findAllByCompanyIdAndIsDeletedFalse(UUID companyId, Pageable pageable);

    Page<InvoiceJpaEntity> findAllByCompanyIdAndStatusAndIsDeletedFalse(UUID companyId, InvoiceStatus status,
            Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InvoiceJpaEntity i WHERE i.invoiceNumber = :invoiceNumber AND i.companyId = :companyId AND i.isDeleted = false")
    boolean existsByInvoiceNumberAndCompanyId(String invoiceNumber, UUID companyId);

    long countByCompanyIdAndIsDeletedFalse(UUID companyId);

    long countByCompanyIdAndStatusAndIsDeletedFalse(UUID companyId, InvoiceStatus status);

    long countByCategoryIdAndIsDeletedFalse(UUID categoryId);

    // ===== Duplicate Detection Queries =====

    // Level 1: Exact invoice number match
    Optional<InvoiceJpaEntity> findByInvoiceNumberAndCompanyIdAndIsDeletedFalse(
            String invoiceNumber, UUID companyId);

    // Level 2: Strong match — supplier tax number + date + amount
    List<InvoiceJpaEntity> findBySupplierTaxNumberAndInvoiceDateAndTotalAmountAndCompanyIdAndIsDeletedFalse(
            String supplierTaxNumber, LocalDate invoiceDate, BigDecimal totalAmount, UUID companyId);

    // Level 3: Fuzzy match — supplier name (case-insensitive) + date + amount range
    @Query("SELECT i FROM InvoiceJpaEntity i WHERE i.companyId = :companyId AND i.isDeleted = false " +
            "AND i.invoiceDate = :invoiceDate " +
            "AND i.totalAmount BETWEEN :minAmount AND :maxAmount " +
            "AND LOWER(TRIM(i.supplierName)) = LOWER(TRIM(:supplierName)) " +
            "AND (:excludeId IS NULL OR i.id <> :excludeId)")
    List<InvoiceJpaEntity> findPotentialDuplicatesBySupplierAndDateAndAmountRange(
            @Param("supplierName") String supplierName,
            @Param("invoiceDate") LocalDate invoiceDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("companyId") UUID companyId,
            @Param("excludeId") UUID excludeId);
}

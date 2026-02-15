package com.faturaocr.domain.invoice.repository;

import com.faturaocr.domain.invoice.entity.InvoiceVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceVersionRepository extends JpaRepository<InvoiceVersion, UUID> {

    List<InvoiceVersion> findByInvoiceIdOrderByVersionNumberDesc(UUID invoiceId);

    Optional<InvoiceVersion> findByInvoiceIdAndVersionNumber(UUID invoiceId, Integer versionNumber);

    @Query("SELECT MAX(v.versionNumber) FROM InvoiceVersion v WHERE v.invoice.id = :invoiceId")
    Optional<Integer> findMaxVersionNumberByInvoiceId(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT v FROM InvoiceVersion v WHERE v.invoice.id = :invoiceId ORDER BY v.versionNumber ASC")
    List<InvoiceVersion> findVersionsByInvoiceId(@Param("invoiceId") UUID invoiceId);
}

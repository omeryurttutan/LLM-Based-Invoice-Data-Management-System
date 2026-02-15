package com.faturaocr.domain.template.port;

import com.faturaocr.domain.template.entity.SupplierTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierTemplateRepository {
    Optional<SupplierTemplate> findByCompanyIdAndSupplierTaxNumber(UUID companyId, String supplierTaxNumber);

    SupplierTemplate save(SupplierTemplate template);

    Page<SupplierTemplate> findAllByCompanyId(UUID companyId, Pageable pageable);

    Optional<SupplierTemplate> findByIdAndCompanyId(Long id, UUID companyId);

    void delete(SupplierTemplate template);
}

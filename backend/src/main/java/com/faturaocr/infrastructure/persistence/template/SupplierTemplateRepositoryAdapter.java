package com.faturaocr.infrastructure.persistence.template;

import com.faturaocr.domain.template.entity.SupplierTemplate;
import com.faturaocr.domain.template.port.SupplierTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupplierTemplateRepositoryAdapter implements SupplierTemplateRepository {

    private final SupplierTemplateJpaRepository jpaRepository;
    private final SupplierTemplateMapper mapper;

    @Override
    public Optional<SupplierTemplate> findByCompanyIdAndSupplierTaxNumber(UUID companyId, String supplierTaxNumber) {
        return jpaRepository.findByCompanyIdAndSupplierTaxNumber(companyId, supplierTaxNumber)
                .map(mapper::toDomain);
    }

    @Override
    public SupplierTemplate save(SupplierTemplate template) {
        SupplierTemplateJpaEntity entity = mapper.toJpa(template);
        SupplierTemplateJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Page<SupplierTemplate> findAllByCompanyId(UUID companyId, Pageable pageable) {
        return jpaRepository.findAllByCompanyId(companyId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SupplierTemplate> findByIdAndCompanyId(Long id, UUID companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(SupplierTemplate template) {
        if (template.getId() != null) {
            jpaRepository.deleteById(template.getId());
        }
    }
}

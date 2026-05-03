package com.faturaocr.infrastructure.persistence.template;

import com.faturaocr.domain.template.entity.SupplierTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupplierTemplateMapper {

    public SupplierTemplate toDomain(SupplierTemplateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        SupplierTemplate domain = new SupplierTemplate();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setSupplierTaxNumber(entity.getSupplierTaxNumber());
        domain.setSupplierName(entity.getSupplierName());
        domain.setSampleCount(entity.getSampleCount());
        domain.setLearnedData(entity.getLearnedData());
        domain.setDefaultCategoryId(entity.getDefaultCategoryId());
        domain.setActive(entity.isActive());
        domain.setLastInvoiceDate(entity.getLastInvoiceDate());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public SupplierTemplateJpaEntity toJpa(SupplierTemplate domain) {
        if (domain == null) {
            return null;
        }
        SupplierTemplateJpaEntity entity = new SupplierTemplateJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setCompanyId(domain.getCompanyId());
        entity.setSupplierTaxNumber(domain.getSupplierTaxNumber());
        entity.setSupplierName(domain.getSupplierName());
        entity.setSampleCount(domain.getSampleCount());
        entity.setLearnedData(domain.getLearnedData());
        entity.setDefaultCategoryId(domain.getDefaultCategoryId());
        entity.setActive(domain.isActive());
        entity.setLastInvoiceDate(domain.getLastInvoiceDate());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

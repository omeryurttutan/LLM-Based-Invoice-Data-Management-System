package com.faturaocr.infrastructure.persistence.company;

import com.faturaocr.domain.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public Company toDomain(CompanyJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Company.builder()
                .id(entity.getId())
                .name(entity.getName())
                .taxNumber(entity.getTaxNumber())
                .taxOffice(entity.getTaxOffice())
                .address(entity.getAddress())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .postalCode(entity.getPostalCode())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .website(entity.getWebsite())
                .defaultCurrency(entity.getDefaultCurrency())
                .invoicePrefix(entity.getInvoicePrefix())
                .isActive(entity.isActive())
                .subscriptionStatus(entity.getSubscriptionStatus())
                .trialEndsAt(entity.getTrialEndsAt())
                .planId(entity.getPlanId())
                .maxUsers(entity.getMaxUsers())
                .maxInvoices(entity.getMaxInvoices())
                .dailyInvoiceLimit(entity.getDailyInvoiceLimit())
                .usedInvoiceCount(entity.getUsedInvoiceCount())
                .dailyInvoiceCount(entity.getDailyInvoiceCount())
                .dailyCountDate(entity.getDailyCountDate())
                .suspendedAt(entity.getSuspendedAt())
                .suspensionReason(entity.getSuspensionReason())
                .build();
    }

    public CompanyJpaEntity toJpaEntity(Company domain) {
        if (domain == null) {
            return null;
        }

        CompanyJpaEntity entity = new CompanyJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setTaxNumber(domain.getTaxNumber());
        entity.setTaxOffice(domain.getTaxOffice());
        entity.setAddress(domain.getAddress());
        entity.setCity(domain.getCity());
        entity.setDistrict(domain.getDistrict());
        entity.setPostalCode(domain.getPostalCode());
        entity.setPhone(domain.getPhone());
        entity.setEmail(domain.getEmail());
        entity.setWebsite(domain.getWebsite());
        entity.setDefaultCurrency(domain.getDefaultCurrency());
        entity.setInvoicePrefix(domain.getInvoicePrefix());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setSubscriptionStatus(domain.getSubscriptionStatus());
        entity.setTrialEndsAt(domain.getTrialEndsAt());
        entity.setPlanId(domain.getPlanId());
        entity.setMaxUsers(domain.getMaxUsers());
        entity.setMaxInvoices(domain.getMaxInvoices());
        entity.setDailyInvoiceLimit(domain.getDailyInvoiceLimit());
        entity.setUsedInvoiceCount(domain.getUsedInvoiceCount());
        entity.setDailyInvoiceCount(domain.getDailyInvoiceCount());
        entity.setDailyCountDate(domain.getDailyCountDate());
        entity.setSuspendedAt(domain.getSuspendedAt());
        entity.setSuspensionReason(domain.getSuspensionReason());

        return entity;
    }
}


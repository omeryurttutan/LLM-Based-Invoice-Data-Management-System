package com.faturaocr.infrastructure.persistence.kvkk;

import com.faturaocr.domain.kvkk.entity.UserConsent;
import org.springframework.stereotype.Component;

@Component
public class UserConsentMapper {

    public UserConsent toDomain(UserConsentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        UserConsent domain = new UserConsent();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setConsentType(entity.getConsentType());
        domain.setConsentVersion(entity.getConsentVersion());
        domain.setGranted(entity.isGranted());
        domain.setIpAddress(entity.getIpAddress());
        domain.setUserAgent(entity.getUserAgent());
        domain.setGrantedAt(entity.getGrantedAt());
        domain.setRevokedAt(entity.getRevokedAt());
        domain.setMetadata(entity.getMetadata());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setDeleted(entity.isDeleted());
        domain.setDeletedAt(entity.getDeletedAt());
        return domain;
    }

    public UserConsentJpaEntity toJpaEntity(UserConsent domain) {
        if (domain == null) {
            return null;
        }
        UserConsentJpaEntity entity = new UserConsentJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setUserId(domain.getUserId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setConsentType(domain.getConsentType());
        entity.setConsentVersion(domain.getConsentVersion());
        entity.setGranted(domain.isGranted());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setGrantedAt(domain.getGrantedAt());
        entity.setRevokedAt(domain.getRevokedAt());
        entity.setMetadata(domain.getMetadata());
        entity.setDeleted(domain.isDeleted());
        return entity;
    }
}

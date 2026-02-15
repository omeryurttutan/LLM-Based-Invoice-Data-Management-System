package com.faturaocr.infrastructure.persistence.kvkk;

import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter
@Setter
public class UserConsentJpaEntity extends BaseJpaEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "consent_version", nullable = false)
    private String consentVersion;

    @Column(name = "is_granted", nullable = false)
    private boolean isGranted;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}

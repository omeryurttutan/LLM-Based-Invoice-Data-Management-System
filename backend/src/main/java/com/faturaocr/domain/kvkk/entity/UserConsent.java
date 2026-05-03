package com.faturaocr.domain.kvkk.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserConsent extends BaseEntity {

    private UUID userId;
    private UUID companyId;
    private ConsentType consentType;
    private String consentVersion;
    private boolean isGranted;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
    private String metadata; // JSON string

    public UserConsent() {
        super();
        this.grantedAt = LocalDateTime.now();
    }

    public static UserConsent create(UUID userId, UUID companyId, ConsentType type, String version, String ip,
            String userAgent) {
        UserConsent consent = new UserConsent();
        consent.userId = userId;
        consent.companyId = companyId;
        consent.consentType = type;
        consent.consentVersion = version;
        consent.isGranted = true;
        consent.ipAddress = ip;
        consent.userAgent = userAgent;
        return consent;
    }

    public void revoke() {
        this.isGranted = false;
        this.revokedAt = LocalDateTime.now();
        markAsUpdated();
    }
}

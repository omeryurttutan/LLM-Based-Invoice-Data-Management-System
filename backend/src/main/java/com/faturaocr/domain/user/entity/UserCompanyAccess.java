package com.faturaocr.domain.user.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.user.valueobject.Role;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCompanyAccess extends BaseEntity {
    private UUID userId;
    private UUID companyId;
    private String companyName;
    private Role role;
    private boolean isActive;

    @Builder
    public UserCompanyAccess(UUID id, UUID userId, UUID companyId, String companyName, Role role, boolean isActive) {
        super(id != null ? id : UUID.randomUUID());
        this.userId = userId;
        this.companyId = companyId;
        this.companyName = companyName;
        this.role = role != null ? role : Role.ACCOUNTANT;
        this.isActive = isActive;
    }
}

package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import com.faturaocr.infrastructure.persistence.company.CompanyJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity for user_company_access table mapping M:N relations.
 */
@Entity
@Table(name = "user_company_access")
@lombok.Getter
@lombok.Setter
public class UserCompanyAccessJpaEntity extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserJpaEntity.RoleJpa role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}

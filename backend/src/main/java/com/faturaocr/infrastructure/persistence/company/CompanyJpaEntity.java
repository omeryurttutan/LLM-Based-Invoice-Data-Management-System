package com.faturaocr.infrastructure.persistence.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class CompanyJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @Column(name = "tax_office")
    private String taxOffice;

    private String address;
    private String city;
    private String district;

    @Column(name = "postal_code")
    private String postalCode;

    private String phone;
    private String email;
    private String website;

    @Column(name = "default_currency")
    private String defaultCurrency;

    @Column(name = "invoice_prefix")
    private String invoicePrefix;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

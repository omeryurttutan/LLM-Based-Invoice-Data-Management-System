package com.faturaocr.domain.company.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Company extends BaseEntity {

    private String name;
    @AuditMask(AuditMask.MaskType.PARTIAL)
    private String taxNumber;
    private String taxOffice;
    @AuditMask(AuditMask.MaskType.FULL)
    private String address;
    private String city;
    private String district;
    private String postalCode;
    @AuditMask(AuditMask.MaskType.PHONE)
    private String phone;
    private String email;
    private String website;
    private String defaultCurrency;
    private String invoicePrefix;
    private boolean isActive;

    @Builder
    public Company(UUID id, String name, String taxNumber, String taxOffice,
            String address, String city, String district, String postalCode,
            String phone, String email, String website,
            String defaultCurrency, String invoicePrefix, boolean isActive) {
        super(id != null ? id : UUID.randomUUID());
        this.name = name;
        this.taxNumber = taxNumber;
        this.taxOffice = taxOffice;
        this.address = address;
        this.city = city;
        this.district = district;
        this.postalCode = postalCode;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "TRY";
        this.invoicePrefix = invoicePrefix;
        this.isActive = isActive;
    }

    // Constructor for creating new company
    public Company(String name, String taxNumber) {
        super();
        this.name = name;
        this.taxNumber = taxNumber;
        this.defaultCurrency = "TRY";
        this.isActive = true;
    }

    public void updateInfo(String name, String taxOffice, String address, String city,
            String district, String postalCode, String phone,
            String email, String website, String defaultCurrency,
            String invoicePrefix) {
        this.name = name;
        this.taxOffice = taxOffice;
        this.address = address;
        this.city = city;
        this.district = district;
        this.postalCode = postalCode;
        this.phone = phone;
        this.email = email;
        this.website = website;
        if (defaultCurrency != null) {
            this.defaultCurrency = defaultCurrency;
        }
        this.invoicePrefix = invoicePrefix;
        markAsUpdated();
    }

    public void activate() {
        this.isActive = true;
        markAsUpdated();
    }

    public void deactivate() {
        this.isActive = false;
        markAsUpdated();
    }
}

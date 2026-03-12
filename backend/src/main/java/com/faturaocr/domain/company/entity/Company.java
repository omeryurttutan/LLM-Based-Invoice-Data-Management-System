package com.faturaocr.domain.company.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.common.util.TaxNumberValidator;
import com.faturaocr.domain.audit.annotation.AuditMask;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // --- SaaS Subscription & Quota Fields ---
    private String subscriptionStatus; // TRIAL, ACTIVE, SUSPENDED, CANCELLED
    private LocalDateTime trialEndsAt;
    private String planId;
    private int maxUsers;
    private int maxInvoices;
    private int dailyInvoiceLimit;
    private int usedInvoiceCount;
    private int dailyInvoiceCount;
    private LocalDate dailyCountDate;
    private LocalDateTime suspendedAt;
    private String suspensionReason;

    @Builder
    public Company(UUID id, String name, String taxNumber, String taxOffice,
            String address, String city, String district, String postalCode,
            String phone, String email, String website,
            String defaultCurrency, String invoicePrefix, boolean isActive,
            String subscriptionStatus, LocalDateTime trialEndsAt, String planId,
            int maxUsers, int maxInvoices, int dailyInvoiceLimit,
            int usedInvoiceCount, int dailyInvoiceCount, LocalDate dailyCountDate,
            LocalDateTime suspendedAt, String suspensionReason) {
        super(id != null ? id : UUID.randomUUID());
        validateTaxNumber(taxNumber);
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
        this.subscriptionStatus = subscriptionStatus != null ? subscriptionStatus : "TRIAL";
        this.trialEndsAt = trialEndsAt;
        this.planId = planId != null ? planId : "FREE_TRIAL";
        this.maxUsers = maxUsers > 0 ? maxUsers : 2;
        this.maxInvoices = maxInvoices > 0 ? maxInvoices : 350;
        this.dailyInvoiceLimit = dailyInvoiceLimit > 0 ? dailyInvoiceLimit : 50;
        this.usedInvoiceCount = usedInvoiceCount;
        this.dailyInvoiceCount = dailyInvoiceCount;
        this.dailyCountDate = dailyCountDate;
        this.suspendedAt = suspendedAt;
        this.suspensionReason = suspensionReason;
    }

    // Constructor for creating new company (with TRIAL defaults)
    public Company(String name, String taxNumber) {
        super();
        validateTaxNumber(taxNumber);
        this.name = name;
        this.taxNumber = taxNumber;
        this.defaultCurrency = "TRY";
        this.isActive = true;
        this.subscriptionStatus = "TRIAL";
        this.trialEndsAt = LocalDateTime.now().plusDays(7);
        this.planId = "FREE_TRIAL";
        this.maxUsers = 2;
        this.maxInvoices = 350;
        this.dailyInvoiceLimit = 50;
        this.usedInvoiceCount = 0;
        this.dailyInvoiceCount = 0;
        this.dailyCountDate = LocalDate.now();
    }

    private void validateTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.length() != 10) {
            throw new IllegalArgumentException("Tax number must be exactly 10 digits");
        }
        if (!taxNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Tax number must contain only digits");
        }
        if (!TaxNumberValidator.isValidVKN(taxNumber)) {
            throw new IllegalArgumentException("Invalid tax number (VKN checksum failed)");
        }
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
        this.subscriptionStatus = "ACTIVE";
        this.suspendedAt = null;
        this.suspensionReason = null;
        markAsUpdated();
    }

    public void deactivate() {
        this.isActive = false;
        markAsUpdated();
    }

    public void suspend(String reason) {
        this.subscriptionStatus = "SUSPENDED";
        this.suspendedAt = LocalDateTime.now();
        this.suspensionReason = reason;
        markAsUpdated();
    }

    public boolean isTrial() {
        return "TRIAL".equals(this.subscriptionStatus);
    }

    public boolean isSuspended() {
        return "SUSPENDED".equals(this.subscriptionStatus);
    }

    public boolean isTrialExpired() {
        return isTrial() && trialEndsAt != null && LocalDateTime.now().isAfter(trialEndsAt);
    }

    public void incrementInvoiceCount() {
        LocalDate today = LocalDate.now();
        if (this.dailyCountDate == null || !this.dailyCountDate.equals(today)) {
            this.dailyCountDate = today;
            this.dailyInvoiceCount = 0;
        }
        this.dailyInvoiceCount++;
        this.usedInvoiceCount++;
        markAsUpdated();
    }

    public boolean hasReachedDailyLimit() {
        LocalDate today = LocalDate.now();
        if (this.dailyCountDate == null || !this.dailyCountDate.equals(today)) {
            return false; // New day, counter will reset
        }
        return this.dailyInvoiceCount >= this.dailyInvoiceLimit;
    }

    public boolean hasReachedTotalLimit() {
        return this.usedInvoiceCount >= this.maxInvoices;
    }
}


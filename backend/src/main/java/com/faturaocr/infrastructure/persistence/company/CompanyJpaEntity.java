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

import com.faturaocr.infrastructure.security.encryption.EncryptedStringConverter;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Convert;

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
    @Convert(converter = EncryptedStringConverter.class)
    private String taxNumber;

    @Column(name = "tax_number_hash")
    private String taxNumberHash;

    @Column(name = "tax_office")
    private String taxOffice;

    @Convert(converter = EncryptedStringConverter.class)
    private String address;
    private String city;
    private String district;

    @Column(name = "postal_code")
    private String postalCode;

    @Convert(converter = EncryptedStringConverter.class)
    private String phone;
    private String email;
    private String website;

    @Column(name = "default_currency")
    private String defaultCurrency;

    @Column(name = "invoice_prefix")
    private String invoicePrefix;

    @Column(name = "is_active")
    private boolean isActive;

    // --- SaaS Subscription & Quota ---
    @Column(name = "subscription_status")
    private String subscriptionStatus;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "max_users")
    private int maxUsers;

    @Column(name = "max_invoices")
    private int maxInvoices;

    @Column(name = "daily_invoice_limit")
    private int dailyInvoiceLimit;

    @Column(name = "used_invoice_count")
    private int usedInvoiceCount;

    @Column(name = "daily_invoice_count")
    private int dailyInvoiceCount;

    @Column(name = "daily_count_date")
    private java.time.LocalDate dailyCountDate;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspension_reason")
    private String suspensionReason;

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

    @PrePersist
    @PreUpdate
    public void hashSensitiveFields() {
        if (this.taxNumber != null) {
            this.taxNumberHash = hashString(this.taxNumber);
        }
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

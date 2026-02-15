package com.faturaocr.infrastructure.persistence.template;

import com.faturaocr.infrastructure.security.encryption.EncryptedStringConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.faturaocr.domain.template.valueobject.LearnedData;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_templates")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SupplierTemplateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "supplier_tax_number", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String supplierTaxNumber;

    @Column(name = "supplier_tax_number_hash")
    private String supplierTaxNumberHash;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Type(JsonType.class)
    @Column(name = "learned_data", columnDefinition = "jsonb", nullable = false)
    private LearnedData learnedData;

    @Column(name = "default_category_id")
    private UUID defaultCategoryId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_invoice_date")
    private LocalDateTime lastInvoiceDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void hashSensitiveFields() {
        if (this.supplierTaxNumber != null) {
            this.supplierTaxNumberHash = hashString(this.supplierTaxNumber);
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

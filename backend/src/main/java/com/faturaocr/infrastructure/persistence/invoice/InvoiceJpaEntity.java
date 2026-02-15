package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.faturaocr.infrastructure.security.encryption.EncryptedStringConverter;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Convert;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceJpaEntity extends BaseJpaEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_tax_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String supplierTaxNumber;

    @Column(name = "supplier_tax_number_hash")
    private String supplierTaxNumberHash;

    @Column(name = "supplier_tax_office")
    private String supplierTaxOffice;

    @Column(name = "supplier_address")
    private String supplierAddress;

    @Column(name = "supplier_phone")
    private String supplierPhone;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;

    @Column(name = "exchange_rate")
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_provider")
    private LlmProvider llmProvider;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "processing_duration_ms")
    private Integer processingDurationMs;

    @Column(name = "original_file_path")
    private String originalFilePath;

    @Column(name = "stored_file_path")
    private String storedFilePath;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "original_file_size")
    private Integer originalFileSize;

    @Column(name = "original_file_type")
    private String originalFileType;

    @Column(name = "e_invoice_uuid")
    private String eInvoiceUuid;

    @Column(name = "e_invoice_ettn")
    private String eInvoiceEttn;

    @Column(name = "notes")
    private String notes;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Type(JsonType.class)
    @Column(name = "extraction_corrections", columnDefinition = "jsonb")
    private List<ExtractionCorrection> extractionCorrections;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemJpaEntity> items = new ArrayList<>();

    public void addItem(InvoiceItemJpaEntity item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItemJpaEntity item) {
        items.remove(item);
        item.setInvoice(null);
    }

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

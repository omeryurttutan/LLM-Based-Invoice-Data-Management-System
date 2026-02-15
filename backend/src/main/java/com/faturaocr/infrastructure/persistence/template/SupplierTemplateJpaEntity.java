package com.faturaocr.infrastructure.persistence.template;

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
    private String supplierTaxNumber;

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
}

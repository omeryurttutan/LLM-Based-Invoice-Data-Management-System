package com.faturaocr.domain.invoice.entity;

import com.faturaocr.domain.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Type(JsonType.class)
    @Column(name = "snapshot_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode snapshotData;

    @Type(JsonType.class)
    @Column(name = "items_snapshot", columnDefinition = "jsonb", nullable = false)
    private JsonNode itemsSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_source", nullable = false)
    private ChangeSource changeSource;

    @Column(name = "change_summary")
    private String changeSummary;

    @Type(JsonType.class)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private JsonNode changedFields;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "company_id")
    private UUID companyId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum ChangeSource {
        MANUAL_EDIT,
        LLM_EXTRACTION,
        LLM_RE_EXTRACTION,
        VERIFICATION,
        STATUS_CHANGE,
        REVERT,
        BULK_UPDATE
    }
}

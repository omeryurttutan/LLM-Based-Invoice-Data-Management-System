package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rule_execution_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RuleExecutionLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_point", nullable = false)
    private TriggerPoint triggerPoint;

    @Type(JsonType.class)
    @Column(name = "conditions_matched", columnDefinition = "jsonb", nullable = false)
    private Object conditionsMatched;

    @Type(JsonType.class)
    @Column(name = "actions_applied", columnDefinition = "jsonb", nullable = false)
    private Object actionsApplied;

    @Column(name = "execution_result", nullable = false)
    private String executionResult;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }
}

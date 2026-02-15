package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.valueobject.RuleAction;
import com.faturaocr.domain.rule.valueobject.RuleCondition;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "automation_rules")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AutomationRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Type(JsonType.class)
    @Column(name = "conditions", columnDefinition = "jsonb", nullable = false)
    private List<RuleCondition> conditions;

    @Type(JsonType.class)
    @Column(name = "actions", columnDefinition = "jsonb", nullable = false)
    private List<RuleAction> actions;

    @Column(name = "condition_logic", nullable = false)
    private String conditionLogic;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_point", nullable = false)
    private TriggerPoint triggerPoint;

    @Column(name = "execution_count", nullable = false)
    private int executionCount;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
}

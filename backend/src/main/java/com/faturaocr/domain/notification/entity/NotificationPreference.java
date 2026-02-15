package com.faturaocr.domain.notification.entity;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference extends BaseJpaEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // Map<NotificationType, Map<Channel, Boolean>>
    // e.g. "EXTRACTION_COMPLETED": { "in_app": true, "email": false }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> preferences;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

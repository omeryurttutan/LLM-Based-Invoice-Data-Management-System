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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription extends BaseJpaEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "endpoint", nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, columnDefinition = "TEXT")
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, columnDefinition = "TEXT")
    private String authKey;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}

package com.faturaocr.domain.notification.port;

import com.faturaocr.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

    Page<Notification> findAllByUserIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndIsReadFalse(UUID userId);

    void markAllAsRead(UUID userId, Instant readAt);

    int deleteReadNotificationsOlderThan(Instant cutoffDate);

    Notification save(Notification notification);

    void delete(Notification notification);

    Optional<Notification> findById(UUID id);
}

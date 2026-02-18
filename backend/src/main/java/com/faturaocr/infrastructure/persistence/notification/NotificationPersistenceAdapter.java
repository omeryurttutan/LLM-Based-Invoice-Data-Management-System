package com.faturaocr.infrastructure.persistence.notification;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Page<Notification> findAllByUserId(UUID userId, Pageable pageable) {
        return notificationJpaRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public Page<Notification> findAllByUserIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable) {
        return notificationJpaRepository.findAllByUserIdAndIsRead(userId, isRead, pageable);
    }

    @Override
    public Optional<Notification> findByIdAndUserId(UUID id, UUID userId) {
        return notificationJpaRepository.findByIdAndUserId(id, userId);
    }

    @Override
    public long countByUserIdAndIsReadFalse(UUID userId) {
        return notificationJpaRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAllAsRead(UUID userId, Instant readAt) {
        notificationJpaRepository.markAllAsRead(userId, readAt);
    }

    @Override
    public int deleteReadNotificationsOlderThan(Instant cutoffDate) {
        return notificationJpaRepository.deleteReadNotificationsOlderThan(cutoffDate);
    }

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public void delete(Notification notification) {
        notificationJpaRepository.delete(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return notificationJpaRepository.findById(id);
    }
}

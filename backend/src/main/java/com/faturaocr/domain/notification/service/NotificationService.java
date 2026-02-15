package com.faturaocr.domain.notification.service;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.domain.notification.service.channel.NotificationChannel;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;
    private final NotificationPreferenceService preferenceService;

    @Transactional
    public void notify(UUID userId, UUID companyId, NotificationType type, String title, String message,
            NotificationReferenceType referenceType, UUID referenceId, Map<String, Object> metadata) {
        notify(userId, companyId, type, title, message, type.getDefaultSeverity(), referenceType, referenceId,
                metadata);
    }

    // Convenience overload
    @Transactional
    public void notify(UUID userId, UUID companyId, NotificationType type, String title, String message,
            NotificationSeverity severity, NotificationReferenceType referenceType, UUID referenceId,
            Map<String, Object> metadata) {

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setCompanyId(companyId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSeverity(severity != null ? severity : type.getDefaultSeverity());
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        if (metadata != null) {
            notification.setMetadata(metadata);
        }

        // Always save to DB first (acting as the persistence layer for "in_app"
        // effectively, but good for history)
        Notification saved = notificationRepository.save(notification);

        // Dispatch to channels
        dispatchToChannels(userId, saved);
    }

    private void dispatchToChannels(UUID userId, Notification notification) {
        for (NotificationChannel channel : notificationChannels) {
            if (channel.supports(notification.getType())) {
                boolean isEnabled = preferenceService.isChannelEnabled(userId, notification.getType(),
                        channel.getChannelName());
                if (isEnabled) {
                    try {
                        channel.send(notification, userId);
                    } catch (Exception e) {
                        log.error("Failed to send notification via channel {}", channel.getChannelName(), e);
                    }
                }
            }
        }
    }

    @Transactional
    public void notifyCompany(UUID companyId, NotificationType type, String title, String message,
            NotificationReferenceType referenceType, UUID referenceId, Map<String, Object> metadata) {
        List<User> companyUsers = userRepository.findAllByCompanyId(companyId);

        for (User user : companyUsers) {
            notify(user.getId(), companyId, type, title, message, type.getDefaultSeverity(), referenceType, referenceId,
                    metadata);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UUID userId, boolean isRead, Pageable pageable) {
        return notificationRepository.findAllByUserIdAndIsRead(userId, isRead, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getAllUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found or access denied"));

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found or access denied"));
        notificationRepository.delete(notification);
    }
}

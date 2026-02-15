package com.faturaocr.domain.notification.service.channel;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationType;

import java.util.UUID;

public interface NotificationChannel {
    String getChannelName(); // "in_app", "email", "push"

    boolean supports(NotificationType type);

    void send(Notification notification, UUID userId);
}

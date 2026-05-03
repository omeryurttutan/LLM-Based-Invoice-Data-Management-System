package com.faturaocr.domain.notification.service.channel;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationChannel implements NotificationChannel {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public String getChannelName() {
        return "in_app";
    }

    @Override
    public boolean supports(NotificationType type) {
        return true; // In-app supports all types by default
    }

    @Override
    public void send(Notification notification, UUID userId) {
        if (userId == null || notification == null) {
            log.warn("Cannot push notification: userId or notification is null");
            return;
        }
        try {
            // /user/{userId}/queue/notifications
            String destUser = userId.toString();
            Map<String, Object> payload = mapToDto(notification);
            messagingTemplate.convertAndSendToUser(
                    destUser,
                    "/queue/notifications",
                    payload);
            log.debug("Sent in-app notification to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to push notification via WebSocket to user {}", userId, e);
        }
    }

    private Map<String, Object> mapToDto(Notification n) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", n.getId());
        dto.put("type", n.getType());
        dto.put("title", n.getTitle());
        dto.put("message", n.getMessage());
        dto.put("severity", n.getSeverity());
        dto.put("referenceType", n.getReferenceType());
        dto.put("referenceId", n.getReferenceId());
        dto.put("metadata", n.getMetadata());
        dto.put("createdAt", n.getCreatedAt());
        dto.put("isRead", n.isRead());
        return dto;
    }
}

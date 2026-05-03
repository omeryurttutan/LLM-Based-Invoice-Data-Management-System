package com.faturaocr.domain.notification.service.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.entity.PushSubscription;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.infrastructure.persistence.notification.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationChannel implements NotificationChannel {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    @Value("${frontend.url:http://localhost:3001}")
    private String frontendUrl;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (Exception e) {
            log.error("Failed to initialize PushService", e);
        }
    }

    @Override
    public String getChannelName() {
        return "push";
    }

    @Override
    public boolean supports(NotificationType type) {
        return true;
    }

    @Override
    @Async
    public void send(Notification notification, UUID userId) {
        if (pushService == null) {
            log.warn("PushService not initialized, skipping push notification");
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(userId);
        if (subscriptions.isEmpty()) {
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                // Build payload
                Map<String, Object> payloadData = new HashMap<>();
                payloadData.put("title", notification.getTitle());
                payloadData.put("body", notification.getMessage());
                payloadData.put("icon", "/icon-192x192.png");
                payloadData.put("data", Map.of("url", getActionUrl(notification)));

                String payloadJson = objectMapper.writeValueAsString(payloadData);

                // Construct Web Push Subscription object

                nl.martijndwars.webpush.Notification pushNotification = new nl.martijndwars.webpush.Notification(
                        sub.getEndpoint(),
                        sub.getP256dhKey(),
                        sub.getAuthKey(),
                        payloadJson.getBytes());

                pushService.send(pushNotification);
                log.debug("Sent push notification to endpoint {}", sub.getEndpoint());

            } catch (Exception e) {
                // Check if 410 Gone, remove subscription
                // For now just log
                log.warn("Failed to send push notification to subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    private String getActionUrl(Notification notification) {
        if (notification.getReferenceType() != null && notification.getReferenceId() != null) {
            switch (notification.getReferenceType()) {
                case INVOICE:
                    return frontendUrl + "/invoices/" + notification.getReferenceId();
                case BATCH:
                    return frontendUrl + "/invoices?batchId=" + notification.getReferenceId();
                default:
                    return frontendUrl + "/notifications";
            }
        }
        return frontendUrl + "/notifications";
    }
}

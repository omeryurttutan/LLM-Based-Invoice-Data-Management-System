package com.faturaocr.domain.notification.service;

import com.faturaocr.domain.notification.entity.PushSubscription;
import com.faturaocr.infrastructure.persistence.notification.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Transactional
    public void subscribe(UUID userId, String endpoint, String p256dh, String auth, String userAgent) {
        // Check if exists to update or create new
        repository.findByEndpoint(endpoint).ifPresentOrElse(
                sub -> {
                    sub.setUserId(userId);
                    sub.setP256dhKey(p256dh);
                    sub.setAuthKey(auth);
                    sub.setUserAgent(userAgent);
                    sub.setLastUsedAt(LocalDateTime.now());
                    repository.save(sub);
                },
                () -> {
                    repository.save(PushSubscription.builder()
                            .userId(userId)
                            .endpoint(endpoint)
                            .p256dhKey(p256dh)
                            .authKey(auth)
                            .userAgent(userAgent)
                            .lastUsedAt(LocalDateTime.now())
                            .build());
                });
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        repository.deleteByEndpoint(endpoint);
    }

    public String getPublicKey() {
        return vapidPublicKey;
    }
}

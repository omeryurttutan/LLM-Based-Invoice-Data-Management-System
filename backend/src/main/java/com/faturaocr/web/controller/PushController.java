package com.faturaocr.web.controller;

import com.faturaocr.domain.notification.service.PushSubscriptionService;
import com.faturaocr.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications", description = "Manage web push subscriptions")
public class PushController {

    private final PushSubscriptionService subscriptionService;

    @GetMapping("/vapid-public-key")
    @Operation(summary = "Get VAPID Public Key")
    public ResponseEntity<String> getVapidPublicKey() {
        return ResponseEntity.ok(subscriptionService.getPublicKey());
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to push notifications")
    public ResponseEntity<Void> subscribe(@RequestBody SubscriptionRequest request) {
        subscriptionService.subscribe(
                SecurityUtils.getCurrentUserId(),
                request.getEndpoint(),
                request.getKeys().getP256dh(),
                request.getKeys().getAuth(),
                request.getUserAgent());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from push notifications")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        subscriptionService.unsubscribe(endpoint);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class SubscriptionRequest {
        private String endpoint;
        private Keys keys;
        private String userAgent;

        @Data
        public static class Keys {
            private String p256dh;
            private String auth;
        }
    }
}

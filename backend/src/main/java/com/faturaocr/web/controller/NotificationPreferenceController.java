package com.faturaocr.web.controller;

import com.faturaocr.domain.notification.service.NotificationPreferenceService;
import com.faturaocr.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Manage user notification preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get user preferences")
    public ResponseEntity<Map<String, Object>> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    @Operation(summary = "Update user preferences")
    public ResponseEntity<Void> updatePreferences(@RequestBody Map<String, Object> preferences) {
        preferenceService.updatePreferences(SecurityUtils.getCurrentUserId(), preferences);
        return ResponseEntity.ok().build();
    }
}

package com.faturaocr.domain.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.domain.notification.entity.NotificationPreference;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.infrastructure.persistence.notification.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreference::getPreferences)
                .orElseGet(this::getDefaultPreferences);
    }

    @Transactional
    public void updatePreferences(UUID userId, Map<String, Object> newPreferences) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .preferences(getDefaultPreferences())
                        .build());

        // Merge with defaults to ensure all keys present? Or just replace?
        // Let's replace for now, but usually merging is safer for new keys.
        // Or better, let's merge newPreferences into current/default.
        Map<String, Object> current = preference.getPreferences();
        if (current == null) {
            current = getDefaultPreferences();
        }

        // Simple merge: put all new entries into current
        current.putAll(newPreferences);

        preference.setPreferences(current);
        preference.setUpdatedAt(LocalDateTime.now());

        preferenceRepository.save(preference);
    }

    public boolean isChannelEnabled(UUID userId, NotificationType type, String channelName) {
        Map<String, Object> prefs = getPreferences(userId);

        // prefs structure: "EXTRACTION_COMPLETED": { "in_app": true, "email": false }
        // as Map
        Object typePrefsObj = prefs.get(type.name());

        if (typePrefsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> typePrefs = (Map<String, Boolean>) typePrefsObj;
            return typePrefs.getOrDefault(channelName, getDefaultChannelState(type, channelName));
        }

        return getDefaultChannelState(type, channelName);
    }

    private boolean getDefaultChannelState(NotificationType type, String channelName) {
        // Default logic if no preference found
        // In-app is usually always true unless explicitly disabled (and we warn against
        // it)
        if ("in_app".equals(channelName))
            return true;

        // Email/Push defaults per type
        switch (type) {
            case EXTRACTION_COMPLETED:
            case BATCH_COMPLETED:
            case BATCH_PARTIALLY_COMPLETED:
            case EXTRACTION_FAILED:
            case ALL_PROVIDERS_DOWN:
                return true; // Urgent/Important events
            default:
                return false; // Less important events
        }
    }

    private Map<String, Object> getDefaultPreferences() {
        Map<String, Object> defaults = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            Map<String, Boolean> channels = new HashMap<>();
            channels.put("in_app", true); // Always on by default
            channels.put("email", getDefaultChannelState(type, "email"));
            channels.put("push", getDefaultChannelState(type, "push"));
            defaults.put(type.name(), channels);
        }
        return defaults;
    }
}

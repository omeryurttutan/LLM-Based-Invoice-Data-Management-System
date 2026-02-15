package com.faturaocr.application.notification.service;

import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final NotificationRepository notificationRepository;

    /**
     * Delete notifications older than 30 days.
     * Runs every day at 03:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications...");
        Instant threshold = Instant.now().minusSeconds(30L * 24 * 60 * 60);

        try {
            int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(threshold);
            log.info("Cleanup task executed. Deleted {} notifications older than: {}", deletedCount, threshold);
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications", e);
        }
    }
}

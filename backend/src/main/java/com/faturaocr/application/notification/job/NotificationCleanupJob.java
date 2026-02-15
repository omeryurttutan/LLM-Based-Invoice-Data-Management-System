package com.faturaocr.application.notification.job;

import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    @Value("${notification.cleanup-days:90}")
    private int cleanupDays;

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting notification cleanup job...");

        Instant cutoffDate = Instant.now().minus(cleanupDays, ChronoUnit.DAYS);
        int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(cutoffDate);

        log.info("Notification cleanup completed. Deleted {} old read notifications.", deletedCount);
    }
}

package com.faturaocr.infrastructure.scheduler;

import com.faturaocr.infrastructure.persistence.company.CompanyJpaEntity;
import com.faturaocr.infrastructure.persistence.company.CompanyJpaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that runs every night at 00:00.
 * Suspends TRIAL companies whose trial period has expired.
 */
@Component
public class SubscriptionCheckScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCheckScheduler.class);

    private final CompanyJpaRepository companyJpaRepository;

    public SubscriptionCheckScheduler(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    /**
     * Runs every day at midnight.
     * Finds all TRIAL companies whose trial_ends_at < now() and suspends them.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void suspendExpiredTrials() {
        LOGGER.info("Running subscription check scheduler...");

        List<CompanyJpaEntity> expiredTrials = companyJpaRepository
                .findAllBySubscriptionStatusAndTrialEndsAtBefore("TRIAL", LocalDateTime.now());

        if (expiredTrials.isEmpty()) {
            LOGGER.info("No expired trials found.");
            return;
        }

        for (CompanyJpaEntity company : expiredTrials) {
            company.setSubscriptionStatus("SUSPENDED");
            company.setSuspendedAt(LocalDateTime.now());
            company.setSuspensionReason("Trial period expired");
            companyJpaRepository.save(company);
            LOGGER.info("Suspended company: {} (ID: {}). Trial ended at: {}",
                    company.getName(), company.getId(), company.getTrialEndsAt());
        }

        LOGGER.info("Subscription check complete. Suspended {} companies.", expiredTrials.size());
    }
}

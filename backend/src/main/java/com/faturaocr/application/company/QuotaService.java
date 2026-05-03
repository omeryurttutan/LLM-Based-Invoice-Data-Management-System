package com.faturaocr.application.company;

import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.port.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for checking and managing company quotas (invoices, users).
 */
@Service
public class QuotaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaService.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public QuotaService(CompanyRepository companyRepository, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if the company can process another invoice (daily + total limits).
     * Throws DomainException if quota exceeded.
     */
    public void checkInvoiceQuota(UUID companyId) {
        Company company = getCompany(companyId);

        // Check subscription status
        if (company.isSuspended()) {
            throw new DomainException("SUBSCRIPTION_SUSPENDED",
                    "Aboneliğiniz askıya alınmıştır. Devam etmek için ödeme yapınız.");
        }

        // Check if trial has expired
        if (company.isTrialExpired()) {
            company.suspend("Trial period expired");
            companyRepository.save(company);
            throw new DomainException("TRIAL_EXPIRED",
                    "Deneme süreniz sona ermiştir. Devam etmek için ödeme yapınız.");
        }

        // Check total invoice limit
        if (company.hasReachedTotalLimit()) {
            throw new DomainException("INVOICE_QUOTA_EXCEEDED",
                    "Toplam fatura işleme limitinize ulaştınız (" + company.getMaxInvoices()
                            + "). Paketinizi yükseltin.");
        }

        // Check daily invoice limit
        if (company.hasReachedDailyLimit()) {
            throw new DomainException("DAILY_INVOICE_LIMIT",
                    "Günlük fatura işleme limitinize ulaştınız (" + company.getDailyInvoiceLimit()
                            + "). Yarın tekrar deneyin veya paketinizi yükseltin.");
        }
    }

    /**
     * Increment the invoice count after successful processing.
     */
    public void incrementInvoiceCount(UUID companyId) {
        Company company = getCompany(companyId);
        company.incrementInvoiceCount();
        companyRepository.save(company);
        LOGGER.info("Invoice count incremented for company {}. Total: {}, Daily: {}",
                companyId, company.getUsedInvoiceCount(), company.getDailyInvoiceCount());
    }

    /**
     * Check if the company can add another user.
     * Throws DomainException if user quota exceeded.
     */
    public void checkUserQuota(UUID companyId) {
        Company company = getCompany(companyId);
        long currentUserCount = userRepository.countActiveByCompanyId(companyId);

        if (currentUserCount >= company.getMaxUsers()) {
            throw new DomainException("USER_QUOTA_EXCEEDED",
                    "Kullanıcı limitinize ulaştınız (" + company.getMaxUsers()
                            + "). Paketinizi yükseltin.");
        }
    }

    /**
     * Get quota information for display to the user (frontend).
     */
    public QuotaInfo getQuotaInfo(UUID companyId) {
        Company company = getCompany(companyId);
        long currentUserCount = userRepository.countActiveByCompanyId(companyId);

        return new QuotaInfo(
                company.getUsedInvoiceCount(),
                company.getMaxInvoices(),
                company.getDailyInvoiceCount(),
                company.getDailyInvoiceLimit(),
                (int) currentUserCount,
                company.getMaxUsers(),
                company.getSubscriptionStatus(),
                company.getTrialEndsAt(),
                company.getPlanId());
    }

    private Company getCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new DomainException("COMPANY_NOT_FOUND", "Company not found"));
    }

    /**
     * Record for quota information returned to frontend.
     */
    public record QuotaInfo(
            int usedInvoices,
            int maxInvoices,
            int dailyUsedInvoices,
            int dailyMaxInvoices,
            int usedUsers,
            int maxUsers,
            String subscriptionStatus,
            java.time.LocalDateTime trialEndsAt,
            String planId) {

        public int remainingInvoices() {
            return Math.max(0, maxInvoices - usedInvoices);
        }

        public int dailyRemainingInvoices() {
            return Math.max(0, dailyMaxInvoices - dailyUsedInvoices);
        }
    }
}

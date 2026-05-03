package com.faturaocr.application.kvkk.service;

import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.port.UserConsentRepository;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository consentRepository;

    @Transactional
    public void recordConsent(UUID userId, UUID companyId, ConsentType type, String version, boolean isGranted,
            String ip, String userAgent) {
        // Check if there is a latest consent
        Optional<UserConsent> latestOpt = consentRepository.findLatestConsent(userId, type);

        if (latestOpt.isPresent()) {
            UserConsent latest = latestOpt.get();
            // If same version and same status, maybe skip?
            // Better to record explicitly if user action happened.
            // But if user REVOKES, we should check if it was granted.

            if (!isGranted && latest.isGranted()) {
                // If revoking, we can just mark new record as revoked?
                // The prompt says: "revoked_at TIMESTAMP NULL | When consent was revoked (if
                // applicable)"
                // And "Create via Flyway... is_granted BOOLEAN".
                // So each action is a NEW record.
                // But logic: "Consent can be revoked at any time."

                // If we create a NEW record with isGranted=false, that represents the current
                // state.
            }
        }

        UserConsent consent = UserConsent.create(userId, companyId, type, version, ip, userAgent);
        if (!isGranted) {
            consent.setGranted(false);
            consent.setRevokedAt(LocalDateTime.now());
        }
        consentRepository.save(consent);
    }

    public Map<ConsentType, Boolean> getCurrentConsents(UUID userId) {
        // Return status for all types
        // Ideally we should cache this or optimize query.
        // For now, naive approach: iterate all types and fetch latest.
        // Better: Fetch all for user, group by type, take latest.

        List<UserConsent> allConsents = consentRepository.findAllByUserId(userId);

        return Helper.latestConsents(allConsents);
    }

    public List<UserConsent> getConsentHistory(UUID userId) {
        return consentRepository.findAllByUserId(userId);
    }

    // Helper class or method
    private static class Helper {
        static Map<ConsentType, Boolean> latestConsents(List<UserConsent> allConsents) {
            // Group by type
            Map<ConsentType, Optional<UserConsent>> latestByType = allConsents.stream()
                    .collect(Collectors.groupingBy(
                            UserConsent::getConsentType,
                            Collectors.maxBy((c1, c2) -> c1.getGrantedAt().compareTo(c2.getGrantedAt()))));

            // Map to boolean
            // If no record, default to FALSE (unless required? but technically false)
            // But required ones are checked elsewhere.

            return java.util.Arrays.stream(ConsentType.values())
                    .collect(Collectors.toMap(
                            type -> type,
                            type -> latestByType.getOrDefault(type, Optional.empty())
                                    .map(UserConsent::isGranted)
                                    .orElse(false)));
        }
    }
}

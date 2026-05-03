package com.faturaocr.domain.kvkk.port;

import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository {
    UserConsent save(UserConsent consent);

    Optional<UserConsent> findLatestConsent(UUID userId, ConsentType type);

    List<UserConsent> findAllByUserId(UUID userId);

    void deleteByUserId(UUID userId); // For right to be forgotten
}

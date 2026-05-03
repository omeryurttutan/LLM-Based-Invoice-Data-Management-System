package com.faturaocr.application.kvkk.service;

import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.port.UserConsentRepository;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private UserConsentRepository consentRepository;

    @InjectMocks
    private ConsentService consentService;

    @Test
    @DisplayName("Should record consent")
    void shouldRecordConsent() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ConsentType type = ConsentType.MARKETING;
        String version = "1.0";
        boolean isGranted = true;
        String ip = "127.0.0.1";
        String userAgent = "Mozilla/5.0";

        when(consentRepository.findLatestConsent(userId, type)).thenReturn(Optional.empty());

        // When
        consentService.recordConsent(userId, companyId, type, version, isGranted, ip, userAgent);

        // Then
        verify(consentRepository).save(any(UserConsent.class));
    }

    @Test
    @DisplayName("Should get current consents")
    void shouldGetCurrentConsents() {
        // Given
        UUID userId = UUID.randomUUID();
        UserConsent consent = UserConsent.create(userId, UUID.randomUUID(), ConsentType.MARKETING, "1.0", "ip", "ua");
        when(consentRepository.findAllByUserId(userId)).thenReturn(List.of(consent));

        // When
        Map<ConsentType, Boolean> result = consentService.getCurrentConsents(userId);

        // Then
        assertThat(result).containsEntry(ConsentType.MARKETING, true);
        assertThat(result.get(ConsentType.DATA_PROCESSING)).isFalse(); // dependent on enum values but likely default
                                                                       // false
    }
}

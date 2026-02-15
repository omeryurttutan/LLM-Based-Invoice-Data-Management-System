package com.faturaocr.infrastructure.persistence.kvkk;

import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.port.UserConsentRepository;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserConsentRepositoryAdapter implements UserConsentRepository {

    private final UserConsentJpaRepository jpaRepository;
    private final UserConsentMapper mapper;

    @Override
    public UserConsent save(UserConsent consent) {
        UserConsentJpaEntity entity = mapper.toJpaEntity(consent);
        UserConsentJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserConsent> findLatestConsent(UUID userId, ConsentType type) {
        return jpaRepository.findLatestConsent(userId, type)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserConsent> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserIdOrderByGrantedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}

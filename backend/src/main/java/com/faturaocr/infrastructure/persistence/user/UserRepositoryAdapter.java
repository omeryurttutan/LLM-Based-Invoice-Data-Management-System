package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing UserRepository port.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    @SuppressWarnings("null")
    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserJpaEntity entity = toJpaEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        if (saved == null) {
            throw new RuntimeException("Failed to save user: saved entity is null");
        }
        return toDomainEntity(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        if (email == null || email.getValue() == null) {
            return Optional.empty();
        }
        return jpaRepository.findByEmail(email.getValue()).map(this::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmailAndCompanyId(Email email, UUID companyId) {
        if (email == null || email.getValue() == null || companyId == null) {
            return Optional.empty();
        }
        return jpaRepository.findByEmailAndCompanyId(email.getValue(), companyId)
                .map(this::toDomainEntity);
    }

    @Override
    public boolean existsByEmail(Email email) {
        if (email == null || email.getValue() == null) {
            return false;
        }
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByEmailAndCompanyId(Email email, UUID companyId) {
        if (email == null || email.getValue() == null || companyId == null) {
            return false;
        }
        return jpaRepository.existsByEmailAndCompanyId(email.getValue(), companyId);
    }

    @Override
    public Page<User> findAllByCompanyId(UUID companyId, Pageable pageable) {
        return jpaRepository.findAllByCompanyId(companyId, pageable)
                .map(this::toDomainEntity);
    }

    @Override
    public java.util.List<User> findAllByCompanyId(UUID companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream()
                .map(this::toDomainEntity)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            jpaRepository.deleteById(id);
        }
    }

    @Override
    public long countByCompanyIdAndRole(UUID companyId, Role role) {
        return jpaRepository.countByCompanyIdAndRole(companyId, UserJpaEntity.RoleJpa.valueOf(role.name()));
    }

    @Override
    public long countActiveByCompanyId(UUID companyId) {
        return jpaRepository.countByCompanyIdAndIsActiveTrue(companyId);
    }

    @Override
    public Optional<User> findByEmailValue(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return jpaRepository.findByEmail(email).map(this::toDomainEntity);
    }

    // Mapping methods
    private UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        if (user.getId() != null) {
            entity.setId(user.getId());
        }
        entity.setCompanyId(user.getCompanyId());
        entity.setEmail(user.getEmailValue());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setRole(UserJpaEntity.RoleJpa.valueOf(user.getRole().name()));
        entity.setActive(user.isActive());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setEmailVerifiedAt(user.getEmailVerifiedAt());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setPasswordChangedAt(user.getPasswordChangedAt());

        // Handling base entity fields if needed (createdAt, updatedAt, isDeleted)
        // Assuming base entity handles them or passed through if manually setting
        // But BaseJpaEntity handles auditing. If we want to preserve dates from domain,
        // we might need to set them.
        // However, usually we rely on JPA auditing for modification dates.
        // Let's assume JPA handles it.
        entity.setDeleted(user.isDeleted());

        if (user.getCompanyAccesses() != null) {
            java.util.List<UserCompanyAccessJpaEntity> accessJpaEntities = user.getCompanyAccesses().stream()
                    .map(access -> {
                        UserCompanyAccessJpaEntity accessEntity = new UserCompanyAccessJpaEntity();
                        if (access.getId() != null) {
                            accessEntity.setId(access.getId());
                        }
                        accessEntity.setUser(entity);

                        com.faturaocr.infrastructure.persistence.company.CompanyJpaEntity companyRef = new com.faturaocr.infrastructure.persistence.company.CompanyJpaEntity();
                        companyRef.setId(access.getCompanyId());
                        companyRef.setName(access.getCompanyName());
                        accessEntity.setCompany(companyRef);

                        accessEntity.setRole(UserJpaEntity.RoleJpa.valueOf(access.getRole().name()));
                        accessEntity.setActive(access.isActive());
                        return accessEntity;
                    }).collect(java.util.stream.Collectors.toList());
            entity.setCompanyAccesses(accessJpaEntities);
        }

        return entity;
    }

    private User toDomainEntity(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .avatarUrl(entity.getAvatarUrl())
                .role(Role.valueOf(entity.getRole().name()))
                .isActive(entity.isActive())
                .emailVerified(entity.isEmailVerified())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lockedUntil(entity.getLockedUntil())
                .lastLoginAt(entity.getLastLoginAt())
                .companyAccesses(
                        entity.getCompanyAccesses() != null
                                ? entity.getCompanyAccesses().stream()
                                        .map(a -> com.faturaocr.domain.user.entity.UserCompanyAccess.builder()
                                                .id(a.getId())
                                                .userId(entity.getId())
                                                .companyId(a.getCompany().getId())
                                                .companyName(a.getCompany().getName())
                                                .role(Role.valueOf(a.getRole().name()))
                                                .isActive(a.isActive())
                                                .build())
                                        .collect(java.util.stream.Collectors.toList())
                                : new java.util.ArrayList<>())
                .build();
    }
}

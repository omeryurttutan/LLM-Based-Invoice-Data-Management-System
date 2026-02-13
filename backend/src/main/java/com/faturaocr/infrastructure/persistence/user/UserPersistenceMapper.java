package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.UserRole;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User domain entity and UserJpaEntity.
 */
@Component
public class UserPersistenceMapper {

    public UserJpaEntity toJpaEntity(User user) {
        if (user == null) {
            return null;
        }

        UserJpaEntity jpaEntity = new UserJpaEntity();
        jpaEntity.setId(user.getId());
        jpaEntity.setFirstName(user.getFirstName());
        jpaEntity.setLastName(user.getLastName());
        jpaEntity.setEmail(user.getEmail().getValue());
        jpaEntity.setPasswordHash(user.getPasswordHash());
        jpaEntity.setRole(toJpaRole(user.getRole()));

        // Map base entity fields
        jpaEntity.setCreatedAt(user.getCreatedAt());
        jpaEntity.setUpdatedAt(user.getUpdatedAt());
        jpaEntity.setDeleted(user.isDeleted());
        jpaEntity.setDeletedAt(user.getDeletedAt());

        return jpaEntity;
    }

    public User toDomainEntity(UserJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return User.reconstitute(
                jpaEntity.getId(),
                jpaEntity.getFirstName(),
                jpaEntity.getLastName(),
                Email.of(jpaEntity.getEmail()),
                jpaEntity.getPasswordHash(),
                toDomainRole(jpaEntity.getRole()),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt(),
                jpaEntity.isDeleted(),
                jpaEntity.getDeletedAt());
    }

    private UserJpaEntity.UserJpaRole toJpaRole(UserRole role) {
        return UserJpaEntity.UserJpaRole.valueOf(role.name());
    }

    private UserRole toDomainRole(UserJpaEntity.UserJpaRole jpaRole) {
        return UserRole.valueOf(jpaRole.name());
    }
}

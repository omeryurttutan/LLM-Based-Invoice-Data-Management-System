package com.faturaocr.domain.user.port;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for User persistence.
 * Implemented by infrastructure layer adapters.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    void deleteById(UUID id);
}

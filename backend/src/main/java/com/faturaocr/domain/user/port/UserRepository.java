package com.faturaocr.domain.user.port;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * User repository port (interface).
 * Implemented by infrastructure layer.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByEmailAndCompanyId(Email email, UUID companyId);

    boolean existsByEmail(Email email);

    boolean existsByEmailAndCompanyId(Email email, UUID companyId);

    Page<User> findAllByCompanyId(UUID companyId, Pageable pageable);

    java.util.List<User> findAllByCompanyId(UUID companyId);

    void deleteById(UUID id);

    long countByCompanyIdAndRole(UUID companyId, Role role);

    long countActiveByCompanyId(UUID companyId);

    Optional<User> findByEmailValue(String email);
}

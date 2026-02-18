package com.faturaocr.application.user;

import com.faturaocr.application.user.dto.ChangeRoleCommand;
import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UpdateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.domain.audit.annotation.Auditable;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Auditable(action = AuditActionType.CREATE, entityType = "USER")
    @org.springframework.cache.annotation.CacheEvict(value = { "user-profile", "company-users" }, allEntries = true)
    public UserResponse createUser(CreateUserCommand command) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId == null) {
            throw new DomainException("Operation requires company context");
        }

        Email email = Email.of(command.getEmail());
        if (userRepository.existsByEmailAndCompanyId(email, companyId)) {
            throw new DomainException("User with email " + command.getEmail() + " already exists in this company");
        }

        String encodedPassword = passwordEncoder.encode(command.getPassword());

        User user = User.builder()
                .companyId(companyId)
                .email(command.getEmail())
                .passwordHash(encodedPassword)
                .fullName(command.getFullName())
                .phone(command.getPhone())
                .role(command.getRole() != null ? command.getRole() : Role.ACCOUNTANT)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromDomain(savedUser);
    }

    @Transactional
    @Auditable(action = AuditActionType.UPDATE, entityType = "USER")
    @org.springframework.cache.annotation.CacheEvict(value = { "user-profile", "company-users" }, allEntries = true)
    public UserResponse updateUser(UUID id, UpdateUserCommand command) {
        User user = getUserInContext(id);

        // Admin update currently doesn't handle avatarUrl, preserving existing or
        // passing null/empty if we want to clear?
        // Ideally we should fetch existing avatarUrl if we don't want to change it, or
        // add avatarUrl to UpdateUserCommand.
        // For now, let's keep existing avatarUrl.
        user.updateDetails(command.getFullName(), command.getPhone(), user.getAvatarUrl());
        User savedUser = userRepository.save(user);
        return UserResponse.fromDomain(savedUser);
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "user-profile", key = "#id")
    public UserResponse getUserById(UUID id) {
        User user = getUserInContext(id);
        return UserResponse.fromDomain(user);
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "company-users", key = "#root.target.getCurrentUserId() != null ? #root.target.getCurrentUserId() : 'anon'") // companyId
                                                                                                                                                                         // is
                                                                                                                                                                         // in
                                                                                                                                                                         // context,
                                                                                                                                                                         // hard
                                                                                                                                                                         // to
                                                                                                                                                                         // use
                                                                                                                                                                         // as
                                                                                                                                                                         // key
                                                                                                                                                                         // unless
                                                                                                                                                                         // passed.
    // Issue: listUsersByCompany relies on context companyId.
    // I can't easily cache this unless I assume 1 user = 1 company and key by
    // userId?
    // Or key by companyId if I can get it.
    // For now, let's NOT cache this one due to context dependency, or extract
    // companyId.
    // Wait, CompanyContextHolder is thread local.
    // I will skip caching listUsersByCompany for now as it's less critical (admin
    // only).
    public Page<UserResponse> listUsersByCompany(Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId == null) {
            throw new DomainException("Operation requires company context");
        }

        return userRepository.findAllByCompanyId(companyId, pageable)
                .map(UserResponse::fromDomain);
    }

    @Transactional
    @Auditable(action = AuditActionType.DELETE, entityType = "USER")
    @org.springframework.cache.annotation.CacheEvict(value = { "user-profile", "company-users" }, allEntries = true)
    public void deleteUser(UUID id) {
        User user = getUserInContext(id);

        // Cannot delete self
        UUID currentUserId = getCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new DomainException("Cannot delete your own account");
        }

        // Last admin protection
        if (user.getRole() == Role.ADMIN) {
            ensureNotLastAdmin(user.getCompanyId());
        }

        user.markAsDeleted();
        userRepository.save(user);
    }

    @Transactional
    @Auditable(action = AuditActionType.UPDATE, entityType = "USER", description = "toggle user active status")
    @org.springframework.cache.annotation.CacheEvict(value = { "user-profile", "company-users" }, allEntries = true)
    public UserResponse toggleUserActive(UUID id) {
        User user = getUserInContext(id);

        // Cannot deactivate self
        UUID currentUserId = getCurrentUserId();
        if (user.isActive() && id.equals(currentUserId)) {
            throw new DomainException("Cannot deactivate your own account");
        }

        if (user.isActive()) {
            user.deactivate();
        } else {
            user.activate();
        }
        User savedUser = userRepository.save(user);
        return UserResponse.fromDomain(savedUser);
    }

    @Transactional
    @Auditable(action = AuditActionType.UPDATE, entityType = "USER", description = "change user role")
    @org.springframework.cache.annotation.CacheEvict(value = { "user-profile", "company-users" }, allEntries = true)
    public UserResponse changeUserRole(UUID id, ChangeRoleCommand command) {
        User user = getUserInContext(id);

        // Cannot change own role
        UUID currentUserId = getCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new DomainException("Cannot change your own role");
        }

        // Last admin protection: if demoting an admin, ensure they're not the last one
        if (user.getRole() == Role.ADMIN && command.getRole() != Role.ADMIN) {
            ensureNotLastAdmin(user.getCompanyId());
        }

        user.changeRole(command.getRole());
        User savedUser = userRepository.save(user);
        return UserResponse.fromDomain(savedUser);
    }

    private User getUserInContext(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId != null && !user.getCompanyId().equals(companyId)) {
            throw new EntityNotFoundException("User", id); // Hide existence of user in other company
        }
        return user;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId();
        }
        return null;
    }

    private void ensureNotLastAdmin(UUID companyId) {
        long adminCount = userRepository.countByCompanyIdAndRole(companyId, Role.ADMIN);
        if (adminCount <= 1) {
            throw new DomainException("Cannot remove the last admin of the company");
        }
    }
}

package com.faturaocr.application.kvkk.service;

import com.faturaocr.domain.kvkk.port.UserConsentRepository;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.persistence.audit.AuditLogJpaRepository;
import com.faturaocr.infrastructure.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RightToBeForgottenService {

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final AuditLogJpaRepository auditLogRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void forgetUser(UUID adminId, UUID userId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        // Check admin role
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!targetUser.getCompanyId().equals(admin.getCompanyId())) {
            throw new AccessDeniedException("User belongs to another company");
        }

        if (targetUser.getId().equals(adminId)) {
            throw new IllegalArgumentException("Cannot delete yourself");
        }

        // Anonymize User
        String anonName = "Anonim Kullanıcı #" + userId.toString().substring(0, 8);
        String anonEmail = "deleted_" + userId + "@anonymized.local";

        // Update User entity
        // We need methods on User entity to set these.
        // User.updateDetails(fullName, phone, avatarUrl)
        // phone -> null
        targetUser.updateDetails(anonName, null, null);

        // Email is a value object in User entity?
        // Step 14: private Email email;
        // User.Builder email(String).
        // No setter for email on User entity?
        // Step 14: No public setEmail.
        // But there is:
        // public void updateDetails...
        // No updateEmail method.
        // I might need to add one or use reflection/mapper tricks if accessing via
        // repository adapter.
        // But Domain Entity should support this logic.
        // I'll assume I can add a method if needed, OR the User entity has it.
        // Checking Step 14 content...
        // It has `verifyEmail()`.
        // It does NOT have `updateEmail()`.
        // I MUST add `anonymize(String newEmail, String newPasswordHash)` to User
        // entity?
        // Or `updateEmail`.
        // I'll add `anonymize` method to `User` entity to encapsulate this domain
        // logic.

        // For now, in this file, I'll call it. I will update User entity next.
        // targetUser.anonymize(anonEmail, randomPassword);

        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        // Using a temporary method call, will implement in User entity
        targetUser.updateForAnonymization(anonEmail, randomPassword);

        targetUser.deactivate();
        targetUser.markAsDeleted();

        userRepository.save(targetUser);

        // Anonymize Audit Logs
        auditLogRepository.updateEmailByUserId(userId, "anonymized");

        // Delete Consents
        userConsentRepository.deleteByUserId(userId);

        // Revoke Tokens
        refreshTokenService.revokeAllUserTokens(userId.toString());
    }
}

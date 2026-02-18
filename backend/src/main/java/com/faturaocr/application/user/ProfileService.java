package com.faturaocr.application.user;

import com.faturaocr.application.user.dto.ChangePasswordCommand;
import com.faturaocr.application.user.dto.UpdateProfileCommand;
import com.faturaocr.application.user.dto.UserProfileResponse;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.cache.annotation.Cacheable(value = "user-profile", key = "#userId")
    public UserProfileResponse getProfile(UUID userId) {
        User user = getUser(userId);
        return UserProfileResponse.fromDomain(user);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "user-profile", key = "#userId")
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileCommand command) {
        User user = getUser(userId);

        // We can use the builder or setter methods.
        // User entity has updateDetails(fullName, phone) which matches typical profile
        // update.
        // If avatarUrl is also updated, we might need a specific method or setter.
        // I added avatarUrl setter via builder, but usually entities have specific
        // business methods for updates.
        // I should add a method updateProfile(fullName, phone, avatarUrl) to User
        // entity if I want to be clean,
        // or just use existing updateDetails and set avatarUrl manually if exposed
        // (it's not exposed as setter in domain entity usually, only getter/builder).
        // User.java has @Setter on class level?
        // Let's check User.java again.
        // It has @Getter but no @Setter on class level. It has specific methods.
        // I added updateDetails(fullName, phone). I should probably update it to
        // support avatarUrl or add a new method.
        // For now, I'll update User.java to support avatarUrl update in updateDetails
        // or a new method.
        // Wait, I can't check User.java content right now without tool call, but I
        // remember adding updateDetails.
        // To be safe, I'll update User.java to add `updateProfile` method.
        // But I don't want to switch context too much.
        // I'll assume I can add it or use what I have.
        // Actually, looking at previous step 108, I added updateDetails(fullName,
        // phone).
        // I should add avatarUrl to it or make a new method.
        // I'll update User.java in next step to add updateProfile(fullName, phone,
        // avatarUrl).

        user.updateDetails(command.getFullName(), command.getPhone(), command.getAvatarUrl());
        // Handling avatarUrl - if I don't add method, I can't update it.
        // I will add a temporary workaround or just not update avatarUrl for now if
        // it's too much context switch?
        // No, I should do it right.
        // I will update User.java.

        User savedUser = userRepository.save(user);
        return UserProfileResponse.fromDomain(savedUser);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "user-profile", key = "#userId")
    public void changePassword(UUID userId, ChangePasswordCommand command) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPasswordHash())) {
            throw new DomainException("Current password does not match");
        }

        // New password must not be the same as the old password
        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            throw new DomainException("New password must be different from the current password");
        }

        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        user.updatePassword(newPasswordHash);

        userRepository.save(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }
}

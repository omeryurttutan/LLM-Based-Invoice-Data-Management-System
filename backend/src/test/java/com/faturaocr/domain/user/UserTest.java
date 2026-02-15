package com.faturaocr.domain.user;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Should create user with builder")
    void shouldCreateUserWithBuilder() {
        // When
        User user = TestDataBuilder.aUser()
                .withEmail("test@example.com")
                .withRole(Role.ADMIN)
                .build();

        // Then
        assertThat(user.getEmailValue()).isEqualTo("test@example.com");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("Should handle locking logic")
    void shouldHandleLockingLogic() {
        // Given
        User user = TestDataBuilder.aUser().build();

        // When
        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();

        // Then
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);

        // Lock
        user.lock(15);
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());

        // Reset (Login success)
        user.recordSuccessfulLogin();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Should verify email")
    void shouldVerifyEmail() {
        // Given
        User user = TestDataBuilder.aUser().emailVerified(false).build();

        // When
        user.verifyEmail();

        // Then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should change role")
    void shouldChangeRole() {
        // Given
        User user = TestDataBuilder.aUser().withRole(Role.ACCOUNTANT).build();

        // When
        user.changeRole(Role.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }
}

package com.faturaocr.application.auth;

import com.faturaocr.application.auth.dto.AuthResponse;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.application.auth.dto.RefreshTokenCommand;
import com.faturaocr.application.auth.dto.RegisterCommand;
import com.faturaocr.application.auth.service.AuthenticationService;
import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import com.faturaocr.infrastructure.security.LoginAttemptService;
import com.faturaocr.infrastructure.security.RefreshTokenService;
import com.faturaocr.testutil.TestDataBuilder;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        RegisterCommand command = new RegisterCommand(
                TestFixtures.COMPANY_ID,
                null,
                null,
                "new@example.com",
                "password123",
                "New User",
                "555-1234");

        when(userRepository.existsByEmailAndCompanyId(any(Email.class), eq(TestFixtures.COMPANY_ID)))
                .thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            return TestDataBuilder.aUser()
                    .withId(UUID.randomUUID())
                    .withEmail(user.getEmailValue())
                    .build();
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refreshToken");

        // When
        AuthResponse response = authenticationService.register(command);

        // Then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() {
        // Given
        LoginCommand command = new LoginCommand("test@example.com", "password");
        User user = TestFixtures.defaultUser();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refreshToken");

        // When
        AuthResponse response = authenticationService.login(command);

        // Then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        verify(loginAttemptService).loginSucceeded(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should throw exception when login with invalid password")
    void shouldThrowExceptionWhenLoginWithInvalidPassword() {
        // Given
        LoginCommand command = new LoginCommand("test@example.com", "wrongpass");
        User user = TestFixtures.defaultUser();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When
        Throwable thrown = catchThrowable(() -> authenticationService.login(command));

        // Then
        assertThat(thrown).isInstanceOf(DomainException.class)
                .hasMessage("Invalid email or password");
        verify(loginAttemptService).loginFailed(anyString());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should block login when account is locked")
    void shouldBlockLoginWhenAccountIsLocked() {
        // Given
        LoginCommand command = new LoginCommand("test@example.com", "password");

        // Mock Redis blocking
        when(loginAttemptService.isBlocked(anyString())).thenReturn(true);
        when(loginAttemptService.getRemainingBlockMinutes(anyString())).thenReturn(15L);

        // When
        Throwable thrown = catchThrowable(() -> authenticationService.login(command));

        // Then
        assertThat(thrown).isInstanceOf(DomainException.class)
                .hasMessageContaining("geçici olarak kilitlendi");
        verify(userRepository, never()).findByEmail(any(Email.class));
    }
}

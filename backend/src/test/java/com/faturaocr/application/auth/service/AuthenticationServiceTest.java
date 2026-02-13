package com.faturaocr.application.auth.service;

import com.faturaocr.application.auth.dto.AuthResponse;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.application.auth.dto.RegisterCommand;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import com.faturaocr.infrastructure.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtTokenProvider,
                refreshTokenService, auditLogRepository);
    }

    @Test
    void shouldRegisterNewUser() {
        // Arrange
        RegisterCommand command = new RegisterCommand(UUID.randomUUID(), "new@example.com", "Password123!", "New User",
                "1234567890");
        when(userRepository.existsByEmailAndCompanyId(any(), any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            User saved = User.builder()
                    .id(UUID.randomUUID())
                    .companyId(u.getCompanyId())
                    .email(u.getEmail())
                    .passwordHash(u.getPasswordHash())
                    .fullName(u.getFullName())
                    .role(u.getRole())
                    .isActive(u.isActive())
                    .build();
            return saved;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);

        // Act
        AuthResponse response = authenticationService.register(command);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        LoginCommand command = new LoginCommand("test@example.com", "Password123!");
        User user = User.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashedPwd")
                .fullName("Test User")
                .role(Role.ACCOUNTANT)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        // Act
        AuthResponse response = authenticationService.login(command);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
    }

    @Test
    void shouldThrowExceptionOnInvalidLogin() {
        // Arrange
        LoginCommand command = new LoginCommand("test@example.com", "WrongPassword");
        User user = User.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashedPwd")
                .fullName("Test User")
                .role(Role.ACCOUNTANT)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(DomainException.class, () -> authenticationService.login(command));
    }
}

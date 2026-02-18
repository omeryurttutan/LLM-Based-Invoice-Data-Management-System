package com.faturaocr.application.auth.service;

import com.faturaocr.application.auth.dto.AuthResponse;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.application.auth.dto.RefreshTokenCommand;
import com.faturaocr.application.auth.dto.RegisterCommand;
import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.audit.AuditRequestContext;
import com.faturaocr.infrastructure.common.util.SanitizationUtils;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import com.faturaocr.infrastructure.security.LoginAttemptService;
import com.faturaocr.infrastructure.security.RefreshTokenService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Authentication application service.
 */
@ApplicationService
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogRepository auditLogRepository;
    private final LoginAttemptService loginAttemptService;

    @org.springframework.beans.factory.annotation.Value("${security.brute-force.max-attempts:5}")
    private int maxLoginAttempts;

    @org.springframework.beans.factory.annotation.Value("${security.brute-force.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            AuditLogRepository auditLogRepository,
            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.auditLogRepository = auditLogRepository;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Register a new user.
     */
    public AuthResponse register(RegisterCommand command) {
        LOGGER.info("Registering new user with email: {}", command.email());

        Email email = Email.of(command.email());

        // Check if email already exists in the same company
        if (userRepository.existsByEmailAndCompanyId(email, command.companyId())) {
            throw new DomainException("AUTH_EMAIL_EXISTS", "Email already registered in this company");
        }

        // Create user
        String sanitizedFullName = SanitizationUtils.sanitizeHtml(command.fullName());

        User user = User.builder()
                .companyId(command.companyId())
                .email(email)
                .passwordHash(passwordEncoder.encode(command.password()))
                .fullName(sanitizedFullName)
                .phone(command.phone())
                .role(Role.ACCOUNTANT) // Default role for new registrations
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        LOGGER.info("User registered successfully with ID: {}", savedUser.getId());

        // Generate tokens
        return generateAuthResponse(savedUser);
    }

    /**
     * Authenticate user and return tokens.
     */
    public AuthResponse login(LoginCommand command) {
        LOGGER.info("Login attempt for email: {}", command.email());

        // Check block status from Redis first
        if (loginAttemptService.isBlocked(command.email())) {
            long remaining = loginAttemptService.getRemainingBlockMinutes(command.email());
            LOGGER.warn("Login failed: account is locked for email: {} (Redis)", command.email());
            throw new DomainException("AUTH_ACCOUNT_LOCKED",
                    "Hesabınız çok fazla başarısız giriş denemesi nedeniyle geçici olarak kilitlendi. " + remaining
                            + " dakika sonra tekrar deneyin.");
        }

        Email email = Email.of(command.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    LOGGER.warn("Login failed: user not found for email: {}", command.email());
                    loginAttemptService.loginFailed(command.email());
                    return new DomainException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
                });

        // Check if account is locked (DB fallback)
        if (user.isLocked()) {
            LOGGER.warn("Login failed: account is locked for email: {}", command.email());
            throw new DomainException("AUTH_ACCOUNT_LOCKED",
                    "Account is locked due to too many failed attempts. Please try again later.");
        }

        // Check if account is active
        if (!user.isActive()) {
            LOGGER.warn("Login failed: account is inactive for email: {}", command.email());
            throw new DomainException("AUTH_ACCOUNT_INACTIVE", "Account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            loginAttemptService.loginFailed(command.email());
            handleFailedLogin(user); // Keep DB logic for persistent history/locking if needed
            throw new DomainException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Successful login
        loginAttemptService.loginSucceeded(command.email());
        user.recordSuccessfulLogin();
        userRepository.save(user);

        // Audit: login event
        saveAuditLog(user, AuditActionType.LOGIN, "User logged in successfully");

        LOGGER.info("User logged in successfully: {}", user.getId());

        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token.
     */
    public AuthResponse refresh(RefreshTokenCommand command) {
        LOGGER.debug("Token refresh requested");

        // Validate refresh token
        if (!refreshTokenService.validateRefreshToken(command.refreshToken())) {
            throw new DomainException("AUTH_INVALID_TOKEN", "Invalid or expired refresh token");
        }

        // Get user ID from refresh token
        String userId = refreshTokenService.getUserIdFromToken(command.refreshToken());

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new DomainException("AUTH_USER_NOT_FOUND", "User not found"));

        if (!user.isActive()) {
            throw new DomainException("AUTH_ACCOUNT_INACTIVE", "Account is inactive");
        }

        // Revoke old refresh token and generate new tokens
        refreshTokenService.revokeRefreshToken(command.refreshToken());

        LOGGER.info("Token refreshed for user: {}", user.getId());

        return generateAuthResponse(user);
    }

    /**
     * Logout user by revoking refresh token.
     */
    public void logout(String refreshToken) {
        LOGGER.debug("Logout requested");
        // Extract user info before revoking token
        String userId = null;
        try {
            userId = refreshTokenService.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            LOGGER.debug("Could not extract user from refresh token for audit: {}", e.getMessage());
        }
        refreshTokenService.revokeRefreshToken(refreshToken);

        // Audit: logout event
        if (userId != null) {
            try {
                User user = userRepository.findById(java.util.UUID.fromString(userId)).orElse(null);
                if (user != null) {
                    saveAuditLog(user, AuditActionType.LOGOUT, "User logged out");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to save logout audit log: {}", e.getMessage());
            }
        }
        LOGGER.info("User logged out successfully");
    }

    /**
     * Handle failed login attempt.
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lock(lockDurationMinutes);
            LOGGER.warn("Account locked due to {} failed attempts: {}",
                    maxLoginAttempts, user.getEmailValue());
        }

        userRepository.save(user);
    }

    /**
     * Generate authentication response with tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration();

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmailValue(),
                user.getFullName(),
                user.getRole().name(),
                user.getCompanyId());

        return AuthResponse.of(accessToken, refreshToken, expiresIn, userInfo);
    }

    /**
     * Save an audit log entry for auth events.
     */
    private void saveAuditLog(User user, AuditActionType action, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .userEmail(user.getEmailValue())
                    .actionType(action)
                    .entityType("USER")
                    .entityId(user.getId())
                    .companyId(user.getCompanyId())
                    .ipAddress(AuditRequestContext.getIpAddress())
                    .userAgent(AuditRequestContext.getUserAgent())
                    .requestId(AuditRequestContext.getRequestId())
                    .description(description)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            LOGGER.warn("Failed to save audit log for {}: {}", action, e.getMessage());
        }
    }
}

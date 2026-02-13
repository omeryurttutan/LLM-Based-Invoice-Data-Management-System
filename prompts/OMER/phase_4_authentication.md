# PHASE 4: USER REGISTRATION AND LOGIN (AUTHENTICATION)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Security**: JWT-based stateless authentication with Redis token management

### Current State
**Phase 0, 1, 2, and 3 have been completed:**
- ✅ Docker Compose environment with PostgreSQL, Redis, RabbitMQ
- ✅ CI/CD Pipeline with GitHub Actions
- ✅ Hexagonal Architecture layer structure
- ✅ Database schema with users, companies, refresh_tokens tables
- ✅ Default admin user created (admin@demo.com / Admin123!)
- ✅ Flyway migrations working

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Implement a complete JWT-based stateless authentication system using Spring Security 6.x. Users should be able to register, login, refresh tokens, and logout. The system must include brute-force protection, secure password hashing, and Redis-based token management.

---

## AUTHENTICATION FLOW

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           AUTHENTICATION FLOW                                 │
└──────────────────────────────────────────────────────────────────────────────┘

1. REGISTRATION
   ┌─────────┐     POST /auth/register      ┌─────────────┐
   │  User   │ ──────────────────────────▶  │   Backend   │
   │         │  {email, password, name}     │             │
   │         │ ◀──────────────────────────  │ Hash pw     │
   └─────────┘     {user created}           │ Save to DB  │
                                            └─────────────┘

2. LOGIN
   ┌─────────┐     POST /auth/login         ┌─────────────┐
   │  User   │ ──────────────────────────▶  │   Backend   │
   │         │  {email, password}           │             │
   │         │ ◀──────────────────────────  │ Verify pw   │
   └─────────┘  {accessToken, refreshToken} │ Generate JWT│
                                            │ Store in Redis
                                            └─────────────┘

3. AUTHENTICATED REQUEST
   ┌─────────┐    GET /api/v1/invoices      ┌─────────────┐
   │  User   │ ──────────────────────────▶  │   Backend   │
   │         │  Authorization: Bearer {AT}  │             │
   │         │ ◀──────────────────────────  │ Verify JWT  │
   └─────────┘     {data}                   │ Check claims│
                                            └─────────────┘

4. TOKEN REFRESH
   ┌─────────┐    POST /auth/refresh        ┌─────────────┐
   │  User   │ ──────────────────────────▶  │   Backend   │
   │         │  {refreshToken}              │             │
   │         │ ◀──────────────────────────  │ Validate RT │
   └─────────┘  {new accessToken}           │ Issue new AT│
                                            └─────────────┘

5. LOGOUT
   ┌─────────┐    POST /auth/logout         ┌─────────────┐
   │  User   │ ──────────────────────────▶  │   Backend   │
   │         │  {refreshToken}              │             │
   │         │ ◀──────────────────────────  │ Blacklist RT│
   └─────────┘     {success}                │ in Redis    │
                                            └─────────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Dependencies (pom.xml)

**Purpose**: Add required security dependencies.

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT Library (jjwt) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### 2. JWT Configuration Properties

**File**: `application.yml` (add to existing)

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-for-jwt-token-generation-min-32-chars}
  access-token:
    expiration: 900000        # 15 minutes in milliseconds
  refresh-token:
    expiration: 604800000     # 7 days in milliseconds

# Security Configuration
security:
  password:
    bcrypt-strength: 12
  brute-force:
    max-attempts: 5
    lock-duration-minutes: 30
```

**File**: `JwtProperties.java`

```java
package com.faturaocr.infrastructure.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secret;
    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();
    
    // Getters and Setters
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    
    public AccessToken getAccessToken() { return accessToken; }
    public void setAccessToken(AccessToken accessToken) { this.accessToken = accessToken; }
    
    public RefreshToken getRefreshToken() { return refreshToken; }
    public void setRefreshToken(RefreshToken refreshToken) { this.refreshToken = refreshToken; }
    
    public static class AccessToken {
        private long expiration = 900000; // 15 minutes
        
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }
    
    public static class RefreshToken {
        private long expiration = 604800000; // 7 days
        
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }
}
```

---

### 3. Domain Layer - User Entity

**File**: `domain/user/entity/User.java`

```java
package com.faturaocr.domain.user.entity;

import com.faturaocr.domain.common.entity.BaseEntity;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User domain entity.
 */
public class User extends BaseEntity {
    
    private UUID companyId;
    private Email email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private Role role;
    private boolean isActive;
    private boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordChangedAt;
    
    // Private constructor for builder pattern
    private User() {
        super();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Domain methods
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        markAsUpdated();
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        markAsUpdated();
    }
    
    public void lock(int durationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        markAsUpdated();
    }
    
    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        resetFailedLoginAttempts();
    }
    
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        markAsUpdated();
    }
    
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
        markAsUpdated();
    }
    
    public void deactivate() {
        this.isActive = false;
        markAsUpdated();
    }
    
    public void activate() {
        this.isActive = true;
        markAsUpdated();
    }
    
    // Getters
    public UUID getCompanyId() { return companyId; }
    public Email getEmail() { return email; }
    public String getEmailValue() { return email.getValue(); }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public boolean isActive() { return isActive; }
    public boolean isEmailVerified() { return emailVerified; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    
    // Builder
    public static class Builder {
        private final User user = new User();
        
        public Builder id(UUID id) {
            user.id = id;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            user.companyId = companyId;
            return this;
        }
        
        public Builder email(String email) {
            user.email = Email.of(email);
            return this;
        }
        
        public Builder email(Email email) {
            user.email = email;
            return this;
        }
        
        public Builder passwordHash(String passwordHash) {
            user.passwordHash = passwordHash;
            return this;
        }
        
        public Builder fullName(String fullName) {
            user.fullName = fullName;
            return this;
        }
        
        public Builder phone(String phone) {
            user.phone = phone;
            return this;
        }
        
        public Builder role(Role role) {
            user.role = role;
            return this;
        }
        
        public Builder isActive(boolean isActive) {
            user.isActive = isActive;
            return this;
        }
        
        public Builder emailVerified(boolean emailVerified) {
            user.emailVerified = emailVerified;
            return this;
        }
        
        public Builder failedLoginAttempts(int attempts) {
            user.failedLoginAttempts = attempts;
            return this;
        }
        
        public Builder lockedUntil(LocalDateTime lockedUntil) {
            user.lockedUntil = lockedUntil;
            return this;
        }
        
        public Builder lastLoginAt(LocalDateTime lastLoginAt) {
            user.lastLoginAt = lastLoginAt;
            return this;
        }
        
        public User build() {
            // Validation
            if (user.companyId == null) {
                throw new IllegalStateException("Company ID is required");
            }
            if (user.email == null) {
                throw new IllegalStateException("Email is required");
            }
            if (user.passwordHash == null) {
                throw new IllegalStateException("Password hash is required");
            }
            if (user.fullName == null || user.fullName.isBlank()) {
                throw new IllegalStateException("Full name is required");
            }
            if (user.role == null) {
                user.role = Role.ACCOUNTANT; // Default role
            }
            return user;
        }
    }
}
```

---

### 4. Domain Layer - Value Objects

**File**: `domain/user/valueobject/Email.java`

```java
package com.faturaocr.domain.user.valueobject;

import com.faturaocr.domain.common.valueobject.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email value object with validation.
 */
public final class Email implements ValueObject {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );
    
    private final String value;
    
    private Email(String value) {
        this.value = value.toLowerCase().trim();
    }
    
    public static Email of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        String trimmed = value.toLowerCase().trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Email too long (max 255 characters)");
        }
        return new Email(trimmed);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

**File**: `domain/user/valueobject/Role.java`

```java
package com.faturaocr.domain.user.valueobject;

/**
 * User roles enum.
 */
public enum Role {
    ADMIN("Administrator with full system access"),
    MANAGER("Manager with invoice management and reporting"),
    ACCOUNTANT("Accountant with invoice CRUD operations"),
    INTERN("Intern with view and create only");
    
    private final String description;
    
    Role(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean hasHigherOrEqualPrivilegeThan(Role other) {
        return this.ordinal() <= other.ordinal();
    }
    
    public boolean canManageUsers() {
        return this == ADMIN;
    }
    
    public boolean canDeleteInvoices() {
        return this == ADMIN || this == MANAGER;
    }
    
    public boolean canEditInvoices() {
        return this != INTERN;
    }
    
    public boolean canViewReports() {
        return this == ADMIN || this == MANAGER;
    }
}
```

---

### 5. Domain Layer - User Repository Port

**File**: `domain/user/port/UserRepository.java`

```java
package com.faturaocr.domain.user.port;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;

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
    
    void deleteById(UUID id);
}
```

---

### 6. Application Layer - Auth DTOs

**File**: `application/auth/dto/RegisterCommand.java`

```java
package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.*;

/**
 * Command for user registration.
 */
public record RegisterCommand(
    
    @NotNull(message = "Company ID is required")
    java.util.UUID companyId,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be less than 255 characters")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    String password,
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName,
    
    @Size(max = 20, message = "Phone must be less than 20 characters")
    String phone

) implements Command {}
```

**File**: `application/auth/dto/LoginCommand.java`

```java
package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for user login.
 */
public record LoginCommand(
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password

) implements Command {}
```

**File**: `application/auth/dto/RefreshTokenCommand.java`

```java
package com.faturaocr.application.auth.dto;

import com.faturaocr.application.common.usecase.Command;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for token refresh.
 */
public record RefreshTokenCommand(
    
    @NotBlank(message = "Refresh token is required")
    String refreshToken

) implements Command {}
```

**File**: `application/auth/dto/AuthResponse.java`

```java
package com.faturaocr.application.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication response with tokens.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
    
    public record UserInfo(
        UUID id,
        String email,
        String fullName,
        String role,
        UUID companyId
    ) {}
}
```

---

### 7. Application Layer - Auth Service

**File**: `application/auth/service/AuthenticationService.java`

```java
package com.faturaocr.application.auth.service;

import com.faturaocr.application.auth.dto.*;
import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import com.faturaocr.infrastructure.security.RefreshTokenService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Authentication application service.
 */
@ApplicationService
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${security.brute-force.max-attempts:5}")
    private int maxLoginAttempts;
    
    @Value("${security.brute-force.lock-duration-minutes:30}")
    private int lockDurationMinutes;
    
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }
    
    /**
     * Register a new user.
     */
    public AuthResponse register(RegisterCommand command) {
        logger.info("Registering new user with email: {}", command.email());
        
        Email email = Email.of(command.email());
        
        // Check if email already exists in the same company
        if (userRepository.existsByEmailAndCompanyId(email, command.companyId())) {
            throw new DomainException("AUTH_EMAIL_EXISTS", "Email already registered in this company");
        }
        
        // Create user
        User user = User.builder()
            .companyId(command.companyId())
            .email(email)
            .passwordHash(passwordEncoder.encode(command.password()))
            .fullName(command.fullName())
            .phone(command.phone())
            .role(Role.ACCOUNTANT) // Default role for new registrations
            .isActive(true)
            .build();
        
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", savedUser.getId());
        
        // Generate tokens
        return generateAuthResponse(savedUser);
    }
    
    /**
     * Authenticate user and return tokens.
     */
    public AuthResponse login(LoginCommand command) {
        logger.info("Login attempt for email: {}", command.email());
        
        Email email = Email.of(command.email());
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                logger.warn("Login failed: user not found for email: {}", command.email());
                return new DomainException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
            });
        
        // Check if account is locked
        if (user.isLocked()) {
            logger.warn("Login failed: account is locked for email: {}", command.email());
            throw new DomainException("AUTH_ACCOUNT_LOCKED", 
                "Account is locked due to too many failed attempts. Please try again later.");
        }
        
        // Check if account is active
        if (!user.isActive()) {
            logger.warn("Login failed: account is inactive for email: {}", command.email());
            throw new DomainException("AUTH_ACCOUNT_INACTIVE", "Account is inactive");
        }
        
        // Verify password
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new DomainException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }
        
        // Successful login
        user.recordSuccessfulLogin();
        userRepository.save(user);
        
        logger.info("User logged in successfully: {}", user.getId());
        
        return generateAuthResponse(user);
    }
    
    /**
     * Refresh access token using refresh token.
     */
    public AuthResponse refresh(RefreshTokenCommand command) {
        logger.debug("Token refresh requested");
        
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
        
        logger.info("Token refreshed for user: {}", user.getId());
        
        return generateAuthResponse(user);
    }
    
    /**
     * Logout user by revoking refresh token.
     */
    public void logout(String refreshToken) {
        logger.debug("Logout requested");
        refreshTokenService.revokeRefreshToken(refreshToken);
        logger.info("User logged out successfully");
    }
    
    /**
     * Handle failed login attempt.
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();
        
        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lock(lockDurationMinutes);
            logger.warn("Account locked due to {} failed attempts: {}", 
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
            user.getCompanyId()
        );
        
        return AuthResponse.of(accessToken, refreshToken, expiresIn, userInfo);
    }
}
```

---

### 8. Infrastructure Layer - JWT Token Provider

**File**: `infrastructure/security/JwtTokenProvider.java`

```java
package com.faturaocr.infrastructure.security;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.infrastructure.common.config.JwtProperties;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token provider for generating and validating tokens.
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Generate access token for user.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessToken().getExpiration());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmailValue());
        claims.put("role", user.getRole().name());
        claims.put("companyId", user.getCompanyId().toString());
        claims.put("fullName", user.getFullName());
        
        return Jwts.builder()
            .claims(claims)
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }
    
    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        } catch (JwtException ex) {
            logger.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Get user ID from token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * Get all claims from token.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * Get access token expiration in seconds.
     */
    public long getAccessTokenExpiration() {
        return jwtProperties.getAccessToken().getExpiration() / 1000;
    }
}
```

---

### 9. Infrastructure Layer - Refresh Token Service

**File**: `infrastructure/security/RefreshTokenService.java`

```java
package com.faturaocr.infrastructure.security;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.infrastructure.common.config.JwtProperties;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Service for managing refresh tokens with Redis.
 */
@Service
public class RefreshTokenService {
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom;
    
    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Create a new refresh token for user.
     */
    public String createRefreshToken(User user) {
        // Generate random token
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Hash token for storage
        String tokenHash = hashToken(token);
        
        // Store in Redis: token_hash -> user_id
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        Duration expiration = Duration.ofMillis(jwtProperties.getRefreshToken().getExpiration());
        
        redisTemplate.opsForValue().set(key, user.getId().toString(), expiration);
        
        return token;
    }
    
    /**
     * Validate refresh token.
     */
    public boolean validateRefreshToken(String token) {
        String tokenHash = hashToken(token);
        
        // Check if token is blacklisted
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            return false;
        }
        
        // Check if token exists
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Get user ID from refresh token.
     */
    public String getUserIdFromToken(String token) {
        String tokenHash = hashToken(token);
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * Revoke refresh token (add to blacklist).
     */
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);
        
        // Remove from active tokens
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        redisTemplate.delete(key);
        
        // Add to blacklist (with same expiration as original token)
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        Duration expiration = Duration.ofMillis(jwtProperties.getRefreshToken().getExpiration());
        redisTemplate.opsForValue().set(blacklistKey, "revoked", expiration);
    }
    
    /**
     * Revoke all refresh tokens for a user.
     */
    public void revokeAllUserTokens(String userId) {
        // In a production system, you would need to maintain a user->tokens mapping
        // For simplicity, this implementation revokes individual tokens
        // Consider using a Set to store all token hashes for a user
    }
    
    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
```

---

### 10. Infrastructure Layer - Security Configuration

**File**: `infrastructure/common/config/SecurityConfig.java`

```java
package com.faturaocr.infrastructure.common.config;

import com.faturaocr.infrastructure.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (using JWT)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",  // Next.js dev
            "http://localhost:8080"   // Backend
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

### 11. Infrastructure Layer - JWT Authentication Filter

**File**: `infrastructure/security/JwtAuthenticationFilter.java`

```java
package com.faturaocr.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                
                UUID userId = UUID.fromString(claims.getSubject());
                String role = claims.get("role", String.class);
                String companyId = claims.get("companyId", String.class);
                String email = claims.get("email", String.class);
                
                // Create authentication principal
                AuthenticatedUser principal = new AuthenticatedUser(
                    userId,
                    email,
                    UUID.fromString(companyId),
                    role
                );
                
                // Create authentication token with role as authority
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
```

**File**: `infrastructure/security/AuthenticatedUser.java`

```java
package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Authenticated user principal.
 */
public record AuthenticatedUser(
    UUID userId,
    String email,
    UUID companyId,
    String role
) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
    
    public boolean isManager() {
        return "MANAGER".equals(role) || isAdmin();
    }
}
```

---

### 12. Interfaces Layer - Auth Controller

**File**: `interfaces/rest/auth/AuthController.java`

```java
package com.faturaocr.interfaces.rest.auth;

import com.faturaocr.application.auth.dto.*;
import com.faturaocr.application.auth.service.AuthenticationService;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.common.BaseController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController extends BaseController {
    
    private final AuthenticationService authenticationService;
    
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterCommand command) {
        
        AuthResponse response = authenticationService.register(command);
        return created(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginCommand command) {
        
        AuthResponse response = authenticationService.login(command);
        return ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenCommand command) {
        
        AuthResponse response = authenticationService.refresh(command);
        return ok(response);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenCommand command) {
        
        authenticationService.logout(command.refreshToken());
        return ok("Logged out successfully", null);
    }
}
```

---

### 13. Infrastructure Layer - User JPA Entity & Repository

**File**: `infrastructure/persistence/user/UserJpaEntity.java`

```java
package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for users table.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity extends BaseJpaEntity {
    
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleJpa role;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
    
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    // Getters and Setters
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public RoleJpa getRole() { return role; }
    public void setRole(RoleJpa role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    
    public enum RoleJpa {
        ADMIN, MANAGER, ACCOUNTANT, INTERN
    }
}
```

**File**: `infrastructure/persistence/user/UserJpaRepository.java`

```java
package com.faturaocr.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for users.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    
    Optional<UserJpaEntity> findByEmail(String email);
    
    Optional<UserJpaEntity> findByEmailAndCompanyId(String email, UUID companyId);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndCompanyId(String email, UUID companyId);
}
```

**File**: `infrastructure/persistence/user/UserRepositoryAdapter.java`

```java
package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing UserRepository port.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {
    
    private final UserJpaRepository jpaRepository;
    
    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public User save(User user) {
        UserJpaEntity entity = toJpaEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }
    
    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }
    
    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue()).map(this::toDomainEntity);
    }
    
    @Override
    public Optional<User> findByEmailAndCompanyId(Email email, UUID companyId) {
        return jpaRepository.findByEmailAndCompanyId(email.getValue(), companyId)
            .map(this::toDomainEntity);
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
    
    @Override
    public boolean existsByEmailAndCompanyId(Email email, UUID companyId) {
        return jpaRepository.existsByEmailAndCompanyId(email.getValue(), companyId);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    // Mapping methods
    private UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setCompanyId(user.getCompanyId());
        entity.setEmail(user.getEmailValue());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        entity.setRole(UserJpaEntity.RoleJpa.valueOf(user.getRole().name()));
        entity.setActive(user.isActive());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setEmailVerifiedAt(user.getEmailVerifiedAt());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setPasswordChangedAt(user.getPasswordChangedAt());
        return entity;
    }
    
    private User toDomainEntity(UserJpaEntity entity) {
        return User.builder()
            .id(entity.getId())
            .companyId(entity.getCompanyId())
            .email(entity.getEmail())
            .passwordHash(entity.getPasswordHash())
            .fullName(entity.getFullName())
            .phone(entity.getPhone())
            .role(Role.valueOf(entity.getRole().name()))
            .isActive(entity.isActive())
            .emailVerified(entity.isEmailVerified())
            .failedLoginAttempts(entity.getFailedLoginAttempts())
            .lockedUntil(entity.getLockedUntil())
            .lastLoginAt(entity.getLastLoginAt())
            .build();
    }
}
```

---

## TESTING REQUIREMENTS

### Test 1: Register New User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "email": "test@example.com",
    "password": "Test123!@#",
    "fullName": "Test User"
  }'

# Expected: 201 Created with accessToken, refreshToken, user info
```

### Test 2: Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "Admin123!"
  }'

# Expected: 200 OK with tokens
```

### Test 3: Access Protected Endpoint
```bash
# Get token from login response
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X GET http://localhost:8080/api/v1/invoices \
  -H "Authorization: Bearer $TOKEN"

# Expected: 200 OK (or 404 if no invoices)
```

### Test 4: Access Without Token
```bash
curl -X GET http://localhost:8080/api/v1/invoices

# Expected: 401 Unauthorized
```

### Test 5: Refresh Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "abc123..."
  }'

# Expected: 200 OK with new tokens
```

### Test 6: Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "abc123..."
  }'

# Expected: 200 OK
```

### Test 7: Brute Force Protection
```bash
# Try 5+ wrong passwords
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email": "admin@demo.com", "password": "wrong"}'
done

# 6th attempt should return "Account is locked"
```

### Test 8: Unit Tests
```bash
cd backend
mvn test -Dtest=AuthenticationServiceTest,JwtTokenProviderTest

# All tests should pass
```

---

## VERIFICATION CHECKLIST

After completing this phase, verify all items:

- [ ] JWT dependencies added to pom.xml
- [ ] JwtProperties configuration class created
- [ ] JWT secret configured in application.yml
- [ ] User domain entity with builder pattern
- [ ] Email and Role value objects
- [ ] UserRepository port (interface) in domain layer
- [ ] RegisterCommand with validation annotations
- [ ] LoginCommand with validation annotations
- [ ] RefreshTokenCommand with validation annotations
- [ ] AuthResponse record with user info
- [ ] AuthenticationService with register/login/refresh/logout
- [ ] JwtTokenProvider for token generation/validation
- [ ] RefreshTokenService with Redis storage
- [ ] SecurityConfig with filter chain
- [ ] JwtAuthenticationFilter
- [ ] AuthenticatedUser principal record
- [ ] AuthController with all endpoints
- [ ] UserJpaEntity for persistence
- [ ] UserJpaRepository interface
- [ ] UserRepositoryAdapter implementing domain port
- [ ] Password hashing with BCrypt strength 12
- [ ] Brute force protection (5 attempts, 30 min lock)
- [ ] Redis storing refresh tokens
- [ ] All unit tests pass
- [ ] Manual API tests pass
- [ ] CI pipeline passes

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_4_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (3-4 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created
Complete list organized by layer:
- Domain layer files
- Application layer files
- Infrastructure layer files
- Interfaces layer files

### 4. API Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/v1/auth/register | Register user | ✅ |
| POST | /api/v1/auth/login | Login | ✅ |
| POST | /api/v1/auth/refresh | Refresh token | ✅ |
| POST | /api/v1/auth/logout | Logout | ✅ |

### 5. Test Results
Include curl command outputs for all tests.

### 6. Security Configuration
- JWT expiration times
- BCrypt strength
- Brute force settings
- CORS configuration

### 7. Issues Encountered
Document any problems and solutions.

### 8. Next Steps
What needs to be done in Phase 5 (RBAC).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Environment Setup ✅
- **Phase 1**: CI/CD Pipeline Setup ✅
- **Phase 2**: Hexagonal Architecture ✅
- **Phase 3**: Database Schema (users table) ✅

### Required By (blocks these phases)
- **Phase 5**: RBAC (needs authentication working)
- **Phase 6**: Company/User API (needs auth)
- **Phase 7**: Invoice CRUD API (needs auth)
- All subsequent phases require authentication

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ User can register with valid credentials
2. ✅ User can login and receive JWT tokens
3. ✅ Access token validates correctly in protected endpoints
4. ✅ Refresh token generates new access token
5. ✅ Logout invalidates refresh token
6. ✅ Brute force protection locks account after 5 failed attempts
7. ✅ Passwords are hashed with BCrypt strength 12
8. ✅ Refresh tokens are stored in Redis
9. ✅ All unit tests pass
10. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **JWT Secret**: Must be at least 256 bits (32 characters) for HS256
2. **Never Log Passwords**: Even hashed passwords shouldn't be logged
3. **Token Storage**: Frontend should store access token in memory, refresh token in httpOnly cookie
4. **Redis Required**: Refresh tokens require Redis to be running
5. **Default Admin**: Use admin@demo.com / Admin123! for testing
6. **Validation**: All DTOs must have validation annotations

---

**Phase 4 Completion Target**: Complete authentication system ready for role-based access control

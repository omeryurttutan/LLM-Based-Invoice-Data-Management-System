# PHASE 2: HEXAGONAL ARCHITECTURE LAYER STRUCTURE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - Backend: Spring Boot 3.2+ (Java 17) with Hexagonal Architecture
  - Frontend: Next.js 14+ (TypeScript)
  - Extraction Service: Python 3.11+ FastAPI

### Current State
**Phase 0 and Phase 1 have been completed:**
- ✅ Docker Compose environment with all services
- ✅ PostgreSQL, Redis, RabbitMQ running
- ✅ Backend, Frontend, Extraction Service skeletons created
- ✅ CI/CD Pipeline with GitHub Actions configured
- ✅ Linting (Checkstyle, ESLint, Ruff) working
- ✅ All services start with `docker-compose up`

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Establish a clean, maintainable Hexagonal Architecture (also known as Ports and Adapters) structure for the Spring Boot backend. This architecture will enforce separation of concerns, enable testability, and ensure that business logic remains independent of external frameworks and infrastructure.

The architecture must follow these principles:
- **Domain-Centric**: Business logic at the core, independent of infrastructure
- **Dependency Inversion**: Inner layers don't depend on outer layers
- **Testability**: Each layer can be tested in isolation
- **Flexibility**: Easy to swap infrastructure components (database, messaging, etc.)

---

## HEXAGONAL ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────┐
│                     INTERFACES LAYER                            │
│         (REST Controllers, DTOs, Request/Response)              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                            │
│              (Use Cases, Application Services)                  │
│                    Orchestrates Domain                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                               │
│     (Entities, Value Objects, Domain Services, Ports)           │
│              Pure Business Logic - No Dependencies              │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                          │
│      (Repository Impl, External Services, Adapters)             │
│            Implements Ports defined in Domain                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Package Structure

**Purpose**: Create a clear, organized package structure that reflects the hexagonal architecture.

**Package Structure** (`backend/src/main/java/com/faturaocr/`):

```
com.faturaocr/
├── FaturaOcrApplication.java           # Main Spring Boot Application
│
├── domain/                              # DOMAIN LAYER (innermost)
│   ├── common/                          # Shared domain components
│   │   ├── entity/
│   │   │   └── BaseEntity.java         # Base entity with ID, timestamps
│   │   ├── valueobject/
│   │   │   └── Money.java              # Example value object
│   │   ├── event/
│   │   │   └── DomainEvent.java        # Domain event interface
│   │   └── exception/
│   │       └── DomainException.java    # Base domain exception
│   │
│   ├── user/                            # User aggregate
│   │   ├── entity/
│   │   ├── valueobject/
│   │   ├── port/                        # Ports (interfaces)
│   │   │   └── UserRepository.java     # Output port
│   │   └── service/                     # Domain services
│   │
│   ├── company/                         # Company aggregate
│   │   ├── entity/
│   │   ├── valueobject/
│   │   └── port/
│   │
│   └── invoice/                         # Invoice aggregate
│       ├── entity/
│       ├── valueobject/
│       ├── port/
│       └── service/
│
├── application/                         # APPLICATION LAYER
│   ├── common/
│   │   ├── usecase/
│   │   │   └── UseCase.java            # UseCase interface
│   │   └── service/
│   │       └── ApplicationService.java # Base application service
│   │
│   ├── user/
│   │   ├── usecase/
│   │   │   ├── CreateUserUseCase.java
│   │   │   └── GetUserUseCase.java
│   │   ├── dto/                         # Application DTOs
│   │   │   ├── CreateUserCommand.java
│   │   │   └── UserResponse.java
│   │   └── service/
│   │       └── UserApplicationService.java
│   │
│   ├── company/
│   │   └── ...
│   │
│   └── invoice/
│       └── ...
│
├── infrastructure/                      # INFRASTRUCTURE LAYER
│   ├── common/
│   │   └── config/
│   │       ├── JpaConfig.java
│   │       └── SecurityConfig.java
│   │
│   ├── persistence/                     # Database adapters
│   │   ├── user/
│   │   │   ├── UserJpaRepository.java  # Spring Data JPA
│   │   │   ├── UserRepositoryAdapter.java # Implements domain port
│   │   │   └── UserJpaEntity.java      # JPA entity (separate from domain)
│   │   ├── company/
│   │   └── invoice/
│   │
│   ├── messaging/                       # RabbitMQ adapters
│   │   └── RabbitMQAdapter.java
│   │
│   └── external/                        # External service adapters
│       └── extraction/
│           └── ExtractionServiceClient.java
│
└── interfaces/                          # INTERFACES LAYER (outermost)
    ├── rest/
    │   ├── common/
    │   │   ├── ApiResponse.java        # Standard API response wrapper
    │   │   ├── ErrorResponse.java      # Error response structure
    │   │   └── GlobalExceptionHandler.java
    │   │
    │   ├── user/
    │   │   ├── UserController.java
    │   │   └── dto/
    │   │       ├── UserRequest.java    # API request DTO
    │   │       └── UserApiResponse.java # API response DTO
    │   │
    │   ├── company/
    │   └── invoice/
    │
    └── websocket/                       # WebSocket handlers (future)
```

---

### 2. Domain Layer Components

**Purpose**: Create the core domain components that will be used across the application.

#### 2.1 Base Entity

**File**: `domain/common/entity/BaseEntity.java`

```java
package com.faturaocr.domain.common.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all domain entities.
 * Provides common fields and behavior for entity identity and auditing.
 */
public abstract class BaseEntity {
    
    protected UUID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected boolean isDeleted;
    protected LocalDateTime deletedAt;
    
    protected BaseEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }
    
    protected BaseEntity(UUID id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }
    
    public UUID getId() {
        return id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        markAsUpdated();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

#### 2.2 Value Object Base

**File**: `domain/common/valueobject/ValueObject.java`

```java
package com.faturaocr.domain.common.valueobject;

/**
 * Marker interface for Value Objects.
 * Value Objects are immutable and compared by their attributes, not identity.
 */
public interface ValueObject {
    // Value objects must implement equals() and hashCode()
    // based on their attributes, not identity
}
```

#### 2.3 Money Value Object (Example)

**File**: `domain/common/valueobject/Money.java`

```java
package com.faturaocr.domain.common.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing monetary amounts.
 * Immutable and compared by value.
 */
public final class Money implements ValueObject {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        return new Money(amount, currency);
    }
    
    public static Money tryLira(BigDecimal amount) {
        return of(amount, "TRY");
    }
    
    public static Money zero(String currencyCode) {
        return of(BigDecimal.ZERO, currencyCode);
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot perform operation on different currencies: " + 
                this.currency + " vs " + other.currency
            );
        }
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && 
               Objects.equals(currency, money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
```

#### 2.4 Domain Event Interface

**File**: `domain/common/event/DomainEvent.java`

```java
package com.faturaocr.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for all domain events.
 * Domain events represent something that happened in the domain.
 */
public interface DomainEvent {
    
    UUID getEventId();
    
    LocalDateTime getOccurredAt();
    
    String getEventType();
}
```

#### 2.5 Abstract Domain Event

**File**: `domain/common/event/AbstractDomainEvent.java`

```java
package com.faturaocr.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for domain events.
 */
public abstract class AbstractDomainEvent implements DomainEvent {
    
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    
    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
```

#### 2.6 Domain Exception

**File**: `domain/common/exception/DomainException.java`

```java
package com.faturaocr.domain.common.exception;

/**
 * Base exception for all domain-level exceptions.
 */
public class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    public DomainException(String message) {
        super(message);
        this.errorCode = "DOMAIN_ERROR";
    }
    
    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

#### 2.7 Entity Not Found Exception

**File**: `domain/common/exception/EntityNotFoundException.java`

```java
package com.faturaocr.domain.common.exception;

import java.util.UUID;

/**
 * Exception thrown when an entity is not found.
 */
public class EntityNotFoundException extends DomainException {
    
    public EntityNotFoundException(String entityName, UUID id) {
        super(
            "ENTITY_NOT_FOUND",
            String.format("%s with id %s not found", entityName, id)
        );
    }
    
    public EntityNotFoundException(String entityName, String identifier) {
        super(
            "ENTITY_NOT_FOUND",
            String.format("%s with identifier %s not found", entityName, identifier)
        );
    }
}
```

---

### 3. Application Layer Components

**Purpose**: Create the application layer infrastructure for use cases and services.

#### 3.1 UseCase Interface

**File**: `application/common/usecase/UseCase.java`

```java
package com.faturaocr.application.common.usecase;

/**
 * Generic interface for all use cases.
 * Each use case represents a single application operation.
 * 
 * @param <I> Input type (Command/Query)
 * @param <O> Output type (Response)
 */
public interface UseCase<I, O> {
    
    O execute(I input);
}
```

#### 3.2 Command Interface (CQRS)

**File**: `application/common/usecase/Command.java`

```java
package com.faturaocr.application.common.usecase;

/**
 * Marker interface for commands (write operations).
 * Commands modify state and may return a result.
 */
public interface Command {
}
```

#### 3.3 Query Interface (CQRS)

**File**: `application/common/usecase/Query.java`

```java
package com.faturaocr.application.common.usecase;

/**
 * Marker interface for queries (read operations).
 * Queries only read data and never modify state.
 */
public interface Query {
}
```

#### 3.4 Application Service Marker

**File**: `application/common/service/ApplicationService.java`

```java
package com.faturaocr.application.common.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annotation to mark application services.
 * Application services are transactional by default.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
@Transactional
public @interface ApplicationService {
}
```

---

### 4. Infrastructure Layer Components

**Purpose**: Create infrastructure configurations and adapter base classes.

#### 4.1 JPA Configuration

**File**: `infrastructure/common/config/JpaConfig.java`

```java
package com.faturaocr.infrastructure.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for the application.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.faturaocr.infrastructure.persistence")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
}
```

#### 4.2 Base JPA Entity

**File**: `infrastructure/persistence/common/BaseJpaEntity.java`

```java
package com.faturaocr.infrastructure.persistence.common;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base JPA entity with common fields.
 * This is separate from domain entity to keep domain pure.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseJpaEntity {
    
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    protected BaseJpaEntity() {
        this.id = UUID.randomUUID();
    }
    
    protected BaseJpaEntity(UUID id) {
        this.id = id;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
```

---

### 5. Interfaces Layer Components

**Purpose**: Create REST API infrastructure components.

#### 5.1 Standard API Response Wrapper

**File**: `interfaces/rest/common/ApiResponse.java`

```java
package com.faturaocr.interfaces.rest.common;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper for consistent API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

#### 5.2 Error Response Structure

**File**: `interfaces/rest/common/ErrorResponse.java`

```java
package com.faturaocr.interfaces.rest.common;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldError> fieldErrors;
    
    public ErrorResponse(String errorCode, String message, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String errorCode, String message, String path, List<FieldError> fieldErrors) {
        this(errorCode, message, path);
        this.fieldErrors = fieldErrors;
    }
    
    // Getters
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    
    /**
     * Represents a field-level validation error.
     */
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
        
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        public String getField() { return field; }
        public String getMessage() { return message; }
        public Object getRejectedValue() { return rejectedValue; }
    }
}
```

#### 5.3 Global Exception Handler

**File**: `interfaces/rest/common/GlobalExceptionHandler.java`

```java
package com.faturaocr.interfaces.rest.common;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;

/**
 * Global exception handler for REST API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        logger.warn("Entity not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex, WebRequest request) {
        
        logger.warn("Domain exception: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed for one or more fields",
            request.getDescription(false).replace("uri=", ""),
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

#### 5.4 Base Controller

**File**: `interfaces/rest/common/BaseController.java`

```java
package com.faturaocr.interfaces.rest.common;

import org.springframework.http.ResponseEntity;

/**
 * Base controller with common helper methods.
 */
public abstract class BaseController {
    
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    protected <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(201).body(ApiResponse.success(data));
    }
    
    protected ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.noContent().build();
    }
}
```

---

### 6. ArchUnit Dependency Rules

**Purpose**: Enforce architectural boundaries using ArchUnit tests.

#### 6.1 Add ArchUnit Dependency

**Add to pom.xml**:
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.2.1</version>
    <scope>test</scope>
</dependency>
```

#### 6.2 Architecture Tests

**File**: `src/test/java/com/faturaocr/architecture/ArchitectureTest.java`

```java
package com.faturaocr.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests to enforce hexagonal architecture rules.
 */
class ArchitectureTest {
    
    private static JavaClasses classes;
    
    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.faturaocr");
    }
    
    @Nested
    @DisplayName("Layer Dependency Rules")
    class LayerDependencyTests {
        
        @Test
        @DisplayName("Domain layer should not depend on other layers")
        void domainShouldNotDependOnOtherLayers() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
                .because("Domain layer should be independent of other layers")
                .check(classes);
        }
        
        @Test
        @DisplayName("Application layer should not depend on infrastructure or interfaces")
        void applicationShouldNotDependOnInfrastructureOrInterfaces() {
            noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..interfaces..")
                .because("Application layer should only depend on domain")
                .check(classes);
        }
        
        @Test
        @DisplayName("Infrastructure should not depend on interfaces")
        void infrastructureShouldNotDependOnInterfaces() {
            noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..interfaces..")
                .because("Infrastructure should not depend on interfaces layer")
                .check(classes);
        }
    }
    
    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureTests {
        
        @Test
        @DisplayName("Layered architecture should be respected")
        void layeredArchitectureShouldBeRespected() {
            layeredArchitecture()
                .consideringAllDependencies()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                .layer("Interfaces").definedBy("..interfaces..")
                
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
                .whereLayer("Interfaces").mayOnlyAccessLayers("Domain", "Application")
                
                .check(classes);
        }
    }
    
    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionTests {
        
        @Test
        @DisplayName("Controllers should be suffixed with Controller")
        void controllersShouldBeSuffixed() {
            classes()
                .that().resideInAPackage("..interfaces.rest..")
                .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(classes);
        }
        
        @Test
        @DisplayName("Use cases should be suffixed with UseCase")
        void useCasesShouldBeSuffixed() {
            classes()
                .that().resideInAPackage("..application..usecase..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("UseCase")
                .check(classes);
        }
        
        @Test
        @DisplayName("Repository adapters should be suffixed with RepositoryAdapter")
        void repositoryAdaptersShouldBeSuffixed() {
            classes()
                .that().resideInAPackage("..infrastructure.persistence..")
                .and().haveSimpleNameContaining("Adapter")
                .should().haveSimpleNameEndingWith("RepositoryAdapter")
                .check(classes);
        }
    }
    
    @Nested
    @DisplayName("Domain Rules")
    class DomainRules {
        
        @Test
        @DisplayName("Domain entities should extend BaseEntity")
        void entitiesShouldExtendBaseEntity() {
            classes()
                .that().resideInAPackage("..domain..entity..")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("BaseEntity")
                .should().beAssignableTo(com.faturaocr.domain.common.entity.BaseEntity.class)
                .check(classes);
        }
        
        @Test
        @DisplayName("Domain should not use Spring annotations")
        void domainShouldNotUseSpringAnnotations() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .because("Domain should be framework-agnostic")
                .check(classes);
        }
    }
}
```

---

### 7. Documentation

**Purpose**: Create architecture documentation.

**File**: `docs/architecture/HEXAGONAL_ARCHITECTURE.md`

```markdown
# Hexagonal Architecture Guide

## Overview

This project follows Hexagonal Architecture (Ports and Adapters) to maintain
clean separation of concerns and ensure the domain logic remains independent
of external frameworks and infrastructure.

## Layer Responsibilities

### Domain Layer (`com.faturaocr.domain`)
- Contains pure business logic
- No dependencies on external frameworks (no Spring, no JPA)
- Defines ports (interfaces) for external communication
- Contains: Entities, Value Objects, Domain Services, Domain Events, Ports

### Application Layer (`com.faturaocr.application`)
- Orchestrates domain logic to fulfill use cases
- Contains application services and use case implementations
- Handles transactions
- Depends only on Domain layer

### Infrastructure Layer (`com.faturaocr.infrastructure`)
- Implements ports defined in Domain layer
- Contains adapters for external systems (database, messaging, etc.)
- Contains framework-specific configurations
- Depends on Domain and Application layers

### Interfaces Layer (`com.faturaocr.interfaces`)
- Entry points to the application (REST API, WebSocket, etc.)
- Contains controllers, DTOs, and API-specific logic
- Depends on Application layer (never directly on Domain)

## Dependency Rules

```
Interfaces → Application → Domain ← Infrastructure
```

- Domain depends on nothing
- Application depends only on Domain
- Infrastructure implements Domain ports
- Interfaces uses Application services

## Adding New Features

1. Start with Domain: Define entities, value objects, and ports
2. Create Application use cases
3. Implement Infrastructure adapters
4. Create Interface endpoints

## Testing Strategy

- Domain: Pure unit tests, no mocking needed
- Application: Unit tests with mocked ports
- Infrastructure: Integration tests with real dependencies
- Interfaces: API tests with MockMvc
```

---

## TESTING REQUIREMENTS

### Test 1: Verify Package Structure
```bash
# Check that all packages exist
ls -la backend/src/main/java/com/faturaocr/domain/
ls -la backend/src/main/java/com/faturaocr/application/
ls -la backend/src/main/java/com/faturaocr/infrastructure/
ls -la backend/src/main/java/com/faturaocr/interfaces/
```

### Test 2: Run ArchUnit Tests
```bash
cd backend
mvn test -Dtest=ArchitectureTest
# All architecture tests should pass
```

### Test 3: Compile Check
```bash
cd backend
mvn compile
# Should compile without errors
```

### Test 4: Verify Dependency Rules
Manually verify by trying to add a wrong dependency:
1. Try to import Spring annotation in domain layer
2. ArchUnit test should fail
3. Remove the import, test should pass

---

## VERIFICATION CHECKLIST

After completing this phase, verify all items:

- [ ] Domain layer package structure created
- [ ] BaseEntity class implemented
- [ ] ValueObject interface and Money example created
- [ ] DomainEvent interface and AbstractDomainEvent created
- [ ] DomainException hierarchy created
- [ ] Application layer package structure created
- [ ] UseCase, Command, Query interfaces created
- [ ] ApplicationService annotation created
- [ ] Infrastructure layer package structure created
- [ ] JpaConfig configuration created
- [ ] BaseJpaEntity for persistence created
- [ ] Interfaces layer package structure created
- [ ] ApiResponse wrapper created
- [ ] ErrorResponse structure created
- [ ] GlobalExceptionHandler implemented
- [ ] BaseController helper created
- [ ] ArchUnit dependency added to pom.xml
- [ ] ArchitectureTest with all rules created
- [ ] All ArchUnit tests pass
- [ ] Project compiles without errors
- [ ] Architecture documentation created
- [ ] CI pipeline passes with new tests

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_2_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created
Complete list of all files with their paths.

### 4. Package Structure
```
com.faturaocr/
├── domain/
│   ├── common/
│   │   ├── entity/
│   │   ├── valueobject/
│   │   ├── event/
│   │   └── exception/
│   └── ...
├── application/
├── infrastructure/
└── interfaces/
```

### 5. ArchUnit Test Results
```bash
$ mvn test -Dtest=ArchitectureTest
# Include actual output
```

### 6. Compilation Status
```bash
$ mvn compile
# Include actual output
```

### 7. Issues Encountered
Document any problems and solutions.

### 8. Architecture Diagram
Include or link to architecture diagram.

### 9. Next Steps
What needs to be done in Phase 3 (Database Schema).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Environment Setup ✅
- **Phase 1**: CI/CD Pipeline Setup ✅

### Required By (blocks these phases)
- **Phase 3**: Database Schema (needs entity structure)
- **Phase 4**: Authentication (needs application layer)
- **Phase 7**: Invoice CRUD API (needs all layers)
- All subsequent backend phases

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ All four layers (domain, application, infrastructure, interfaces) have proper package structure
2. ✅ Base classes and interfaces are created for each layer
3. ✅ ArchUnit tests enforce dependency rules
4. ✅ All ArchUnit tests pass
5. ✅ Domain layer has no framework dependencies
6. ✅ Project compiles without errors
7. ✅ CI pipeline passes with architecture tests
8. ✅ Architecture documentation is complete
9. ✅ GlobalExceptionHandler properly handles all exception types
10. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **Domain Purity**: Domain layer must NOT have any Spring/JPA annotations
2. **Separate JPA Entities**: Infrastructure has its own JPA entities, mapped from domain
3. **Ports in Domain**: Repository interfaces (ports) are defined in domain, implemented in infrastructure
4. **No Shortcuts**: Don't skip ArchUnit tests - they prevent architecture erosion
5. **Package by Feature**: Within each layer, organize by feature (user, invoice, etc.)

---

**Phase 2 Completion Target**: Clean, testable architecture foundation for all future development

# Hexagonal Architecture Guidelines

This project follows the **Hexagonal Architecture** (also known as Ports and Adapters) pattern to ensure separation of concerns, testability, and independence from external frameworks.

## Core Principles

1.  **Domain First**: The core logic resides in the `domain` package and has NO dependencies on outer layers or external frameworks (Spring, JPA, etc.).
2.  **Dependency Rule**: Dependencies always point INWARD.
    *   `Infrastructure` -> `Application` -> `Domain`
    *   `Interfaces` -> `Application` -> `Domain`
3.  **Ports & Adapters**:
    *   **Ports**: Interfaces defined in the `domain` (for driven ports like Repositories) or `application` (for driving ports like Use Cases).
    *   **Adapters**: Implementations in `infrastructure` (driven adapters) or `interfaces` (driving adapters).

## Layer Structure

### 1. Domain Layer (`com.faturaocr.domain`)
The heart of the application. Contains business logic and state.

*   **Components**: Entities, Value Objects, Domain Events, Domain Exceptions, Repository Interfaces (Ports).
*   **Dependencies**: None (Java standard library only).
*   **Rules**:
    *   Entities must extend `BaseEntity`.
    *   Value Objects should implement `ValueObject`.
    *   No Spring annotations (e.g., `@Service`, `@Autowired`).

### 2. Application Layer (`com.faturaocr.application`)
Orchestrates domain objects to perform specific tasks.

*   **Components**: Use Cases, Commands, Queries, Application Services.
*   **Dependencies**: `Domain`.
*   **Rules**:
    *   Implements `UseCase<Input, Output>` interface.
    *   Uses `@ApplicationService` annotation for transaction management.
    *   Maps DTOs to Domain objects and vice versa.

### 3. Infrastructure Layer (`com.faturaocr.infrastructure`)
Provides implementations for domain ports (e.g., persistence, external APIs).

*   **Components**: JPA Repositories, JPA Entities, Persistence Mappers, External Service Clients.
*   **Dependencies**: `Domain`, `Application`, Spring Data JPA, other libraries.
*   **Rules**:
    *   Implements Domain Repository interfaces.
    *   Maps Domain entities to JPA entities using Mappers (e.g., `UserPersistenceMapper`).
    *   Implementation details (database, email provider) are hidden here.

### 4. Interfaces Layer (`com.faturaocr.interfaces`)
Handles interaction with the outside world (REST APIs, CLI, etc.).

*   **Components**: REST Controllers, DTOs (Request/Response), Exception Handlers.
*   **Dependencies**: `Domain` (for DTO mapping), `Application`.
*   **Rules**:
    *   Controllers must return `ResponseEntity<ApiResponse<T>>`.
    *   Handles HTTP mapping and validation.
    *   Delegates business logic to Application Use Cases.

## Enforced Rules (ArchUnit)

We use **ArchUnit** tests (`ArchitectureTest.java`) to enforce these rules automatically:

*   **Layer Dependencies**: Verified strict dependency direction.
*   **Naming Conventions**:
    *   Controllers end with `Controller`.
    *   Use Cases end with `UseCase`.
    *   Repository Adapters end with `RepositoryAdapter`.
*   **Domain Purity**: Domain classes must not depend on Spring or other frameworks.

## Implementation Details

*   **BaseEntity**: Handles `id`, `createdAt`, `updatedAt`, `isDeleted`.
*   **Money**: Value object for currency handling.
*   **User Persistence**:
    *   `User` (Domain Entity) <-> `UserPersistenceMapper` <-> `UserJpaEntity` (Database Table).
    *   `UserRepositoryAdapter` implements `UserRepository` using `UserJpaRepository`.

## Testing Strategy

*   **Unit Tests**: Focus on Domain logic and Use Cases (mocking dependencies).
*   **Architecture Tests**: Enforce structural integrity.
*   **Integration Tests**: Verify Infrastructure adapters and API endpoints.

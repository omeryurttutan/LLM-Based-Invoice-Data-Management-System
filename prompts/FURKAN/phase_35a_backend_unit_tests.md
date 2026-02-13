# PHASE 35-A: UNIT TESTS — BACKEND (SPRING BOOT)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080 — Java 17, Spring Boot 3.2, Hexagonal Architecture
  - **Python Microservice**: Port 8000 — FastAPI (tested in Phase 35-B)
  - **Next.js Frontend**: Port 3000 (tested in Phase 37)

### Current State (Phases 0-34 Completed)
All 34 phases are complete. The backend now contains:
- ✅ Authentication (JWT + Redis), RBAC (4 roles), Company/User CRUD
- ✅ Invoice CRUD with status workflow (PENDING → VERIFIED/REJECTED)
- ✅ Audit Log (AOP-based, immutable)
- ✅ Duplication Control (hash-based)
- ✅ Advanced Filtering (JPA Specifications)
- ✅ Export (XLSX/CSV + accounting formats: Logo, Mikro, Netsis, Luca)
- ✅ Dashboard statistics endpoints
- ✅ Notification system (WebSocket, email, push)
- ✅ Version History & Rollback
- ✅ Template Learning & Rule Engine
- ✅ KVKK Compliance (AES-256-GCM encryption, consent, right to be forgotten, data retention)
- ✅ Rate Limiting (Redis-based) & Security Hardening

### Hexagonal Architecture Layers (Phase 2)
```
com.faturaocr/
├── domain/           — Entities, Value Objects, Domain Services
│   ├── invoice/
│   ├── user/
│   ├── company/
│   ├── category/
│   ├── notification/
│   ├── template/
│   └── rule/
├── application/      — Use Cases, Application Services, DTOs
│   ├── invoice/
│   ├── user/
│   ├── auth/
│   ├── company/
│   ├── notification/
│   ├── template/
│   └── rule/
├── infrastructure/   — Repository Adapters, External Services, Config
│   ├── persistence/
│   ├── security/
│   ├── messaging/
│   ├── notification/
│   └── common/
└── interfaces/       — REST Controllers, Global Exception Handler
    └── rest/
```

### Test Tools Already Available (Phase 0-1)
- JUnit 5 (via spring-boot-starter-test)
- Mockito (via spring-boot-starter-test)
- AssertJ (via spring-boot-starter-test)
- JaCoCo (if configured in build file — otherwise set up in this phase)
- ArchUnit (Phase 2 — for architecture tests)

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 3-4 days
- **Parallel**: FURKAN works on Phase 35-B (Python extraction service unit tests) simultaneously

---

## OBJECTIVE

Write comprehensive unit tests for the Spring Boot backend covering:

1. **Domain Layer**: Entity behavior, value objects, business rules
2. **Application Layer**: Service/use case logic with mocked dependencies
3. **Infrastructure Layer**: Encryption service, rate limiting, utility classes
4. **Target**: Minimum 80% code coverage on critical modules
5. **Tooling**: JaCoCo for coverage reporting, integrated into CI/CD

---

## DETAILED REQUIREMENTS

### 1. Test Infrastructure Setup

**1.1 JaCoCo Configuration:**

Add JaCoCo plugin to the build file (Maven pom.xml or Gradle build.gradle):
- Generate coverage reports on `mvn test` / `gradle test`
- Output: HTML report + XML report (for CI/CD)
- Enforce minimum coverage: 80% on `application` and `domain` packages
- Exclude from coverage: DTOs, configuration classes, entity getters/setters, `Application.java`

**1.2 Test Directory Structure:**

Mirror the main source structure:
```
src/test/java/com/faturaocr/
├── domain/
│   ├── invoice/
│   ├── user/
│   ├── company/
│   └── ...
├── application/
│   ├── invoice/
│   ├── auth/
│   ├── user/
│   └── ...
├── infrastructure/
│   ├── security/
│   ├── kvkk/
│   └── ...
└── testutil/          — Shared test utilities, builders, fixtures
```

**1.3 Test Utilities (create in `testutil` package):**

- `TestDataBuilder`: Fluent builders for creating test entities (Invoice, User, Company, Category)
- `TestFixtures`: Pre-built common test objects (a default company, admin user, sample invoice)
- These utilities DRY up test code and make tests more readable

Example:
```
TestDataBuilder.anInvoice()
    .withInvoiceNumber("FTR-2026-001")
    .withSupplierName("ABC Ltd")
    .withTotalAmount(new BigDecimal("1500.00"))
    .withStatus(InvoiceStatus.PENDING)
    .build();
```

---

### 2. Domain Layer Tests

Test entity behavior, validation rules, and domain logic. These tests should have NO mocks — they test pure domain logic.

**2.1 Invoice Entity Tests** (`InvoiceTest.java`):

- Invoice creation with valid data succeeds
- Invoice status transitions:
  - PENDING → VERIFIED (valid)
  - PENDING → REJECTED (valid)
  - VERIFIED → PENDING via reopen (valid)
  - VERIFIED → VERIFIED (invalid — should throw)
  - REJECTED → VERIFIED (invalid — should throw)
- Soft delete sets `deletedAt` timestamp
- `calculateTotal()` correctly sums items (if such domain logic exists)
- Confidence score must be between 0 and 100
- Currency validation (only TRY, USD, EUR allowed)

**2.2 User Entity Tests** (`UserTest.java`):

- User creation with valid data
- Role assignment and changes
- Email validation
- Soft delete behavior

**2.3 Company Entity Tests** (`CompanyTest.java`):

- Company creation
- Tax number format validation (10 digits for VKN)

**2.4 Invoice Items Tests**:

- Item creation with valid data
- Amount calculations (quantity × unit price = line total)
- Negative quantity or price rejected

**2.5 Value Object Tests** (if applicable):

- Money/Amount value object (if exists): equality, addition, currency mismatch
- TaxNumber value object (if exists): format validation, VKN vs TCKN

---

### 3. Application Layer Tests (Service / Use Case Tests)

These are the core tests. Each application service/use case should have its dependencies MOCKED.

**Pattern:**
```java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CompanyContextHolder companyContextHolder;
    @InjectMocks private InvoiceService invoiceService;
    
    @Test
    void shouldCreateInvoiceSuccessfully() {
        // given
        when(companyContextHolder.getCompanyId()).thenReturn(1L);
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        // when
        var result = invoiceService.create(createInvoiceRequest);
        
        // then
        assertThat(result.getInvoiceNumber()).isEqualTo("FTR-001");
        verify(invoiceRepository).save(any());
    }
}
```

**3.1 AuthService Tests** (`AuthServiceTest.java`):

- Register: successful registration, duplicate email error, invalid input
- Login: successful login returns tokens, wrong password, nonexistent user, locked account (Phase 32)
- Refresh token: valid refresh, expired refresh, blacklisted refresh
- Logout: token blacklisted, Redis cleaned

**3.2 InvoiceService Tests** (`InvoiceServiceTest.java`):

- Create invoice: valid creation, missing required fields, duplicate detection (Phase 9)
- Get invoice: found, not found (404), belongs to different company (403)
- List invoices: pagination, returns correct page size
- Update invoice: valid update, status change via update blocked
- Delete invoice (soft): sets deletedAt
- Verify invoice: status changes to VERIFIED, audit log called
- Reject invoice: status changes to REJECTED
- Reopen invoice: VERIFIED → PENDING
- Company isolation: never returns invoices from other companies

**3.3 CompanyService Tests** (`CompanyServiceTest.java`):

- Create company
- Update company
- Delete company (soft)
- Get company: found, not found

**3.4 UserService Tests** (`UserServiceTest.java`):

- Create user (admin only)
- Update user, role change
- Deactivate user
- Profile update (user updates own profile)
- Company-scoped user listing

**3.5 CategoryService Tests** (`CategoryServiceTest.java`):

- CRUD operations
- Prevent deletion of category with linked invoices

**3.6 NotificationService Tests** (`NotificationServiceTest.java`):

- Create notification
- Mark as read, mark all as read
- Unread count
- Notification dispatch to correct channels (in-app, email, push)

**3.7 ExportService Tests** (`ExportServiceTest.java`):

- XLSX export generates valid file
- CSV export generates valid file
- Accounting format exports (Logo, Mikro, Netsis, Luca)
- Export respects active filters
- Export handles empty result set

**3.8 DashboardService Tests** (`DashboardServiceTest.java`):

- Statistics calculation: total invoices, total amount, by status, by category
- Date range filtering
- Currency filtering

**3.9 VersionHistoryService Tests** (`VersionHistoryServiceTest.java`):

- Version created on invoice update
- Version snapshot contains correct data
- Diff between versions
- Revert to previous version

**3.10 TemplateLearningService Tests** (`TemplateLearningServiceTest.java`):

- Template created/updated on invoice verification
- Field accuracy stats updated
- Template suggestions generated after minimum sample count
- Template matched by supplier tax number

**3.11 RuleEngineService Tests** (`RuleEngineServiceTest.java`):

- Rule CRUD
- Condition evaluation: all operators (equals, not_equals, contains, greater_than, less_than, in, between, is_null)
- AND/OR condition logic
- Action execution: SET_CATEGORY, SET_STATUS, ADD_NOTE, FLAG_FOR_REVIEW
- Priority-based execution order
- Re-entry guard prevents infinite loops
- Dry-run (test) mode

**3.12 AuditLogService Tests** (`AuditLogServiceTest.java`):

- Audit entry created for each operation type
- Old and new values captured
- Sensitive fields masked (Phase 31)
- Query with filters

---

### 4. Infrastructure Layer Tests

**4.1 EncryptionService Tests** (`EncryptionServiceTest.java` — Phase 31):

- Encrypt and decrypt round-trip produces original value
- Different plaintexts produce different ciphertexts
- Same plaintext encrypted twice produces different ciphertexts (unique IV)
- Null input returns null
- Empty string handling
- Decryption with wrong key returns placeholder
- Invalid ciphertext handling

**4.2 RateLimitService Tests** (`RateLimitServiceTest.java` — Phase 32):

- Request within limit succeeds
- Request exceeding limit returns false
- Different tiers have different limits
- Counter resets after window
- Redis unavailable → fail open (allow)

**4.3 DataRetentionService Tests** (`DataRetentionServiceTest.java` — Phase 31):

- Identifies records past retention period
- Hard deletes expired records
- Skips users with invoices under retention
- Logs deletion counts

**4.4 ConsentService Tests** (`ConsentServiceTest.java` — Phase 31):

- Grant consent
- Revoke consent
- Query latest consent by type
- Consent history

**4.5 SanitizationUtils Tests** (`SanitizationUtilsTest.java` — Phase 32):

- HTML tags stripped
- Script tags removed
- Normal text preserved
- Null handling

---

### 5. Architecture Tests (ArchUnit — Phase 2 extension)

Extend the existing ArchUnit tests:

- Domain layer does not depend on Application or Infrastructure
- Application layer does not depend on Infrastructure (only interfaces)
- Controllers are only in `interfaces.rest` package
- Repositories are only in `infrastructure.persistence` package
- No cycles between packages
- All services are annotated with `@Service` or `@Component`
- All controllers are annotated with `@RestController`

---

### 6. Test Naming Convention

Use descriptive test method names following the pattern:
- `should{ExpectedBehavior}When{StateOrCondition}`
- Examples:
  - `shouldCreateInvoiceSuccessfully()`
  - `shouldThrowWhenInvoiceNotFound()`
  - `shouldReturnForbiddenWhenAccessingOtherCompanyInvoice()`
  - `shouldMaskSensitiveFieldsInAuditLog()`

Use `@DisplayName` annotation for human-readable test names where the method name is not sufficient.

---

### 7. Test Coverage Targets

| Package | Target | Rationale |
|---|---|---|
| domain/** | 90%+ | Pure business logic, most critical |
| application/** | 80%+ | Service layer with business rules |
| infrastructure/security/** | 80%+ | Security-critical code |
| infrastructure/kvkk/** | 80%+ | KVKK compliance code |
| interfaces/rest/** | 60%+ | Controllers mostly delegate (integration tests in Phase 36) |
| infrastructure/persistence/** | Excluded | Tested via integration tests (Phase 36) |
| DTOs, Config classes | Excluded | No logic to test |

---

### 8. CI/CD Integration

Update the GitHub Actions workflow (Phase 1) to:

- Run `mvn test` (or `gradle test`) with JaCoCo
- Publish JaCoCo report as a build artifact
- Fail the build if coverage drops below 80% on critical packages
- Display test results summary in the PR check

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_35a_result.md`

The result file must include:

1. Phase summary
2. Test infrastructure setup (JaCoCo config, test utilities)
3. Test count by layer (domain, application, infrastructure)
4. Total test count
5. Coverage report summary (per-package percentages)
6. Test execution time
7. Files created (list all test files with paths)
8. TestDataBuilder and TestFixtures documentation
9. ArchUnit test additions
10. CI/CD changes
11. Tests that uncovered bugs (list any bugs found and fixed during testing)
12. Issues encountered and solutions
13. Next steps (Phase 36 integration tests)

---

## DEPENDENCIES

### Requires (must be completed first)
- **All Phases 0-34**: All backend code must be implemented before testing
- **Phase 1**: CI/CD pipeline (for JaCoCo integration)
- **Phase 2**: ArchUnit (for architecture tests)

### Required By
- **Phase 36**: Integration Tests (builds on test infrastructure from this phase)
- **Phase 38**: Performance Optimization (coverage data helps identify hot paths)
---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] JaCoCo plugin configured in pom.xml/build.gradle
- [ ] `mvn test jacoco:report` generates HTML coverage report
- [ ] TestDataBuilder utility class created with builder methods for all entities
- [ ] TestFixtures class created with pre-built common test scenarios
- [ ] Domain entity tests: Invoice status transitions (PENDING→VERIFIED, PENDING→REJECTED, etc.)
- [ ] Domain entity tests: invalid transitions throw exceptions
- [ ] Domain value object tests: all value objects validated
- [ ] AuthService tests: register, login, refresh, logout flows
- [ ] AuthService tests: locked account scenario
- [ ] AuthService tests: invalid credentials scenario
- [ ] InvoiceService tests: create, read, update, delete
- [ ] InvoiceService tests: verify, reject, reopen status changes
- [ ] InvoiceService tests: company isolation (user from company A cannot access company B)
- [ ] ExportService tests: XLSX format generation
- [ ] ExportService tests: CSV format generation
- [ ] ExportService tests: each accounting format (Logo, Mikro, Netsis, Luca)
- [ ] NotificationService tests: in-app, email, push channels
- [ ] RuleEngineService tests: all operator types
- [ ] RuleEngineService tests: AND/OR logic combinations
- [ ] RuleEngineService tests: re-entry guard prevents infinite loop
- [ ] TemplateService tests: template learning on verification
- [ ] EncryptionService tests: encrypt/decrypt round-trip
- [ ] EncryptionService tests: different key produces different ciphertext
- [ ] RateLimitService tests: under limit allows request
- [ ] RateLimitService tests: over limit blocks request
- [ ] Audit log masking tests: sensitive fields are masked in log output
- [ ] ArchUnit tests: domain layer has no dependency on infrastructure
- [ ] ArchUnit tests: application layer has no dependency on interfaces
- [ ] Domain package coverage ≥ 90%
- [ ] Application package coverage ≥ 80%
- [ ] Total test count ≥ 150
- [ ] All tests pass with zero failures
- [ ] CI/CD pipeline runs tests and publishes coverage report
- [ ] Result file created at docs/OMER/step_results/faz_35a_result.md
---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ JaCoCo is configured and generates HTML + XML coverage reports
2. ✅ TestDataBuilder and TestFixtures utility classes are created
3. ✅ Domain layer tests cover all entity behaviors and status transitions
4. ✅ Every application service has comprehensive unit tests with mocked dependencies
5. ✅ AuthService tests cover register, login, refresh, logout, locked account scenarios
6. ✅ InvoiceService tests cover full CRUD + verify/reject/reopen + company isolation
7. ✅ ExportService tests verify all export formats
8. ✅ RuleEngineService tests cover all operators, AND/OR logic, re-entry guard
9. ✅ EncryptionService tests verify encrypt/decrypt round-trip and edge cases
10. ✅ RateLimitService tests verify tier limits and Redis failure handling
11. ✅ Audit log masking tests verify sensitive field protection
12. ✅ ArchUnit tests extended for all architecture rules
13. ✅ Domain package coverage ≥ 90%
14. ✅ Application package coverage ≥ 80%
15. ✅ Total backend test count ≥ 150 tests
16. ✅ All tests pass (zero failures)
17. ✅ CI/CD pipeline runs tests and publishes coverage report
18. ✅ Result file is created at docs/OMER/step_results/faz_35a_result.md

---

## IMPORTANT NOTES

1. **Unit Tests Only**: This phase is strictly unit tests. No database, no Redis, no RabbitMQ. All external dependencies must be mocked with Mockito. Integration tests with real infrastructure come in Phase 36.

2. **Test in Isolation**: Each test class should be independent. No test should depend on the execution order of another test. Use `@BeforeEach` for setup, not shared mutable state.

3. **AssertJ Over JUnit Assertions**: Prefer AssertJ's fluent API (`assertThat(...).isEqualTo(...)`) over JUnit's `assertEquals()`. AssertJ produces better failure messages and is more readable.

4. **Mock Only Direct Dependencies**: Mock only the interfaces/classes that the class under test directly depends on. Don't mock internal Java classes or value objects.

5. **Test Edge Cases**: Don't just test the happy path. Test: null inputs, empty collections, boundary values (0, max, negative), concurrent access (if applicable), exception handling.

6. **Tests Document Behavior**: Well-written tests serve as documentation. Someone reading the tests should understand what the service does without reading the implementation.

7. **Fix Bugs Found**: If writing tests reveals bugs in the existing code, fix them. Document the bugs and fixes in the result file — this is valuable information for the graduation project report.

8. **Don't Test Frameworks**: Don't test that Spring injects dependencies correctly, or that JPA saves to the database. Test YOUR code — the business logic and rules.

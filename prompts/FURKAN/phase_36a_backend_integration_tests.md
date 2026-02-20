# PHASE 36-A: INTEGRATION TESTS — BACKEND (SPRING BOOT)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)

### Current State (Phases 0-35 Completed)
All features implemented. Phase 35-A added comprehensive unit tests for the backend with JaCoCo coverage reporting and TestDataBuilder utilities.

### What Are Integration Tests?
Unlike unit tests (Phase 35-A) which test classes in isolation with mocked dependencies, integration tests verify that **multiple layers work together** with **real infrastructure**:
- Real PostgreSQL database (via Testcontainers)
- Real Redis (via Testcontainers)
- Real RabbitMQ (via Testcontainers)
- Real Spring Security filter chain
- Real HTTP requests via MockMvc
- Real JPA/Hibernate queries against a real database

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 3-4 days
- **Parallel**: FURKAN works on Phase 36-B (Python integration tests) simultaneously

---

## OBJECTIVE

Write integration tests for the Spring Boot backend that verify:

1. **Controller endpoints** work end-to-end (HTTP → Controller → Service → Repository → DB)
2. **Authentication & Authorization** works correctly per role
3. **Repository custom queries** and JPA Specifications produce correct SQL
4. **RabbitMQ messaging** sends and receives messages correctly
5. **Cross-cutting concerns** (audit logging, encryption, rate limiting) work in integration
6. **Database migrations** produce a working schema

---

## DETAILED REQUIREMENTS

### 1. Testcontainers Setup

**1.1 Base Test Configuration:**

Create an abstract base class that all integration tests extend:

- Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` or `@SpringBootTest` with `@AutoConfigureMockMvc`
- Configure Testcontainers for:
  - **PostgreSQL 15**: Same version as production
  - **Redis 7**: For JWT tokens and rate limiting
  - **RabbitMQ 3**: For async messaging
- Use `@Testcontainers` and `@Container` annotations or a shared container approach (singleton containers for faster tests)
- Override Spring datasource, Redis, and RabbitMQ properties to point to test containers
- Apply Flyway migrations automatically against the test PostgreSQL
- Use `@Transactional` for test isolation where appropriate (auto-rollback)

**1.2 Test Data Seeding:**

Create a `TestDataSeeder` utility or use `@Sql` annotations:
- Seed a test company
- Seed users for each role (ADMIN, MANAGER, ACCOUNTANT, INTERN) belonging to that company
- Seed a second company with its own users (for multi-tenant isolation tests)
- Seed sample categories
- Seed sample invoices in various statuses (PENDING, VERIFIED, REJECTED)
- Provide helper methods: `getAdminToken()`, `getManagerToken()`, `getAccountantToken()`, `getInternToken()`

These helpers should perform actual login via the auth API and return valid JWT tokens.

**1.3 Singleton Containers (Performance):**

Starting a new container per test class is slow. Use a shared container pattern:

```java
abstract class BaseIntegrationTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withReuse(true);
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379).withReuse(true);
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management")
        .withReuse(true);
    
    static { postgres.start(); redis.start(); rabbit.start(); }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        // ... Redis, RabbitMQ properties
    }
}
```

---

### 2. Authentication & Authorization Integration Tests

Test the complete auth flow with real JWT processing and Redis.

**2.1 Auth Flow Tests** (`AuthIntegrationTest.java`):

- **Register**: POST /api/v1/auth/register → 201, user created in DB, can login
- **Register duplicate email**: → 409 Conflict
- **Register invalid input**: missing fields → 400 with validation errors
- **Login**: POST /api/v1/auth/login → 200, returns accessToken + refreshToken
- **Login wrong password**: → 401
- **Login nonexistent user**: → 401
- **Refresh token**: POST /api/v1/auth/refresh → 200, new access token
- **Refresh with expired token**: → 401
- **Logout**: POST /api/v1/auth/logout → 200, refresh token invalidated in Redis
- **Access protected endpoint without token**: → 401
- **Access protected endpoint with expired token**: → 401
- **Brute-force lockout (Phase 32)**: 5 failed logins → 423 Locked

**2.2 RBAC Tests** (`RbacIntegrationTest.java`):

For each role, test access to key endpoints:

| Endpoint | ADMIN | MANAGER | ACCOUNTANT | INTERN |
|---|---|---|---|---|
| GET /api/v1/invoices | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| POST /api/v1/invoices | ✅ 201 | ✅ 201 | ✅ 201 | ✅ 201 |
| PUT /api/v1/invoices/{id} | ✅ 200 | ✅ 200 | ✅ 200 | ❌ 403 |
| DELETE /api/v1/invoices/{id} | ✅ 200 | ✅ 200 | ❌ 403 | ❌ 403 |
| POST /api/v1/invoices/{id}/verify | ✅ 200 | ✅ 200 | ✅ 200 | ❌ 403 |
| GET /api/v1/admin/users | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 |
| GET /api/v1/audit-logs | ✅ 200 | ✅ 200 | ❌ 403 | ❌ 403 |
| GET /api/v1/admin/kvkk/report | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 |

**2.3 Multi-Tenant Isolation Tests** (`MultiTenantIntegrationTest.java`):

- Company A user cannot see Company B invoices
- Company A user cannot update Company B invoices
- Company A admin cannot manage Company B users
- Listing endpoints only return current company data

---

### 3. Invoice CRUD Integration Tests

**`InvoiceIntegrationTest.java`:**

- **Create invoice**: POST → verify 201, response body matches, DB record exists
- **List invoices**: GET with pagination → verify correct page size, total count
- **Get invoice detail**: GET /{id} → verify all fields including items
- **Update invoice**: PUT /{id} → verify 200, DB record updated
- **Delete invoice**: DELETE /{id} → verify soft delete (deleted_at set, record still in DB)
- **Verify invoice**: POST /{id}/verify → status changes to VERIFIED in DB
- **Reject invoice**: POST /{id}/reject → status changes to REJECTED
- **Reopen invoice**: POST /{id}/reopen → status back to PENDING
- **Invalid status transition**: verify → verify → 400 error
- **Duplicate detection (Phase 9)**: upload same invoice twice → detected

---

### 4. Filtering & Search Integration Tests

**`FilteringIntegrationTest.java`:**

Test that JPA Specifications produce correct results against a real database.

Seed test data with varied invoices, then test:

- Filter by status (PENDING) → only PENDING invoices returned
- Filter by date range → correct subset
- Filter by supplier → exact match
- Filter by category → correct subset
- Filter by amount range (min-max) → correct subset
- Filter by currency → correct subset
- Filter by source type (LLM, E_INVOICE, MANUAL) → correct subset
- Full text search by invoice number → found
- Full text search by supplier name → found
- Combined filters (status + date + supplier) → correct intersection
- Empty result set → 200 with empty list
- Pagination with filters → correct total count and pages

---

### 5. Export Integration Tests

**`ExportIntegrationTest.java`:**

- XLSX export → response is valid XLSX file with correct headers and data rows
- CSV export → response is valid CSV with correct delimiters
- Export with filters → only filtered data exported
- Export empty result set → valid file with headers only
- Accounting format export (Logo) → verify format structure
- Accounting format export (Mikro) → verify format structure

Test by checking: Content-Type header, Content-Disposition header, file parseable, row count matches.

---

### 6. Repository Integration Tests

**`InvoiceRepositoryIntegrationTest.java`:**

Test custom queries and specifications against a real database:

- Find by company_id → correct results
- Find by status → correct results
- Specification with multiple criteria → correct SQL (verify via result, not SQL string)
- Soft-deleted records not returned by default queries
- Count by status and company → correct numbers
- Dashboard aggregation queries → correct sums and counts

**`UserRepositoryIntegrationTest.java`:**

- Find by email → found
- Find by email non-existent → empty
- Find by company_id → correct list

---

### 7. Audit Log Integration Tests

**`AuditLogIntegrationTest.java`:**

- Create an invoice → audit log entry exists with action CREATE
- Update an invoice → audit log with old_value and new_value
- Delete an invoice → audit log with action DELETE
- Verify an invoice → audit log with action VERIFY
- Login → audit log with action LOGIN
- Sensitive fields masked in audit log (Phase 31): check that tax numbers are masked
- Audit log immutability: attempt UPDATE on audit_logs → fails (trigger)

---

### 8. RabbitMQ Integration Tests

**`RabbitMqIntegrationTest.java`:**

Test with a real RabbitMQ container:

- Publish extraction request message → message arrives in queue
- Message format is correct JSON
- Dead letter queue receives rejected messages
- Result listener processes incoming result messages
- Result updates invoice status in database
- Full round-trip: publish request → simulate result → verify DB update

---

### 9. KVKK Integration Tests

**`KvkkIntegrationTest.java`:**

- **Encryption round-trip via DB**: Save entity with encrypted field → read back → decrypted value matches original
- **Hash-based search**: Save entity with hashed tax number → search by tax number hash → found
- **Consent API**: Grant consent → query → consent recorded in DB
- **Right to be forgotten**: Anonymize user → verify user data anonymized in DB, invoices preserved
- **Data retention**: Create soft-deleted record with old timestamp → run retention job → record hard-deleted

---

### 10. Rate Limiting Integration Tests

**`RateLimitIntegrationTest.java`:**

- Send requests within limit → all succeed (200)
- Send requests exceeding limit → 429 response with Retry-After header
- Rate limit headers present in responses (X-RateLimit-Limit, X-RateLimit-Remaining)
- Login lockout: 5 failed logins → 423, then wait or admin unlock → login succeeds

---

### 11. Notification Integration Tests

**`NotificationIntegrationTest.java`:**

- Create notification via service → stored in DB
- Mark as read → updated in DB
- Unread count endpoint → correct count
- WebSocket connection (if testable — may skip WebSocket in integration tests)

---

### 12. Version History Integration Tests

**`VersionHistoryIntegrationTest.java`:**

- Update invoice → version created in DB
- Multiple updates → multiple versions with correct sequence
- Revert to version → invoice data matches version snapshot
- Version diff → correct changed fields identified

---

### 13. Template & Rule Engine Integration Tests

**`TemplateRuleIntegrationTest.java`:**

- Verify invoice → template created/updated for supplier
- Template lookup by supplier tax number → found
- Create rule → rule saved in DB
- Evaluate rule against invoice → correct condition matching
- Rule dry-run (test mode) → returns evaluation result without applying

---

### 14. CI/CD Integration

Update GitHub Actions:

- Add Testcontainers service containers or use Testcontainers Cloud
- Integration tests run in a separate step from unit tests: `mvn verify -Pit` (integration test profile)
- Integration tests may be slower — consider running on merge to main only (not every push)
- Publish test results and timing

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_36a_result.md`

The result file must include:

1. Phase summary
2. Testcontainers setup details (containers, versions, shared pattern)
3. Test data seeding approach
4. Test count by category (auth, RBAC, CRUD, filtering, export, audit, RabbitMQ, KVKK, rate limit, notifications, versions, templates/rules)
5. Total integration test count
6. Test execution time (slower than unit tests — document)
7. Files created (all test files with paths)
8. CI/CD changes
9. Bugs found during integration testing (document and fix)
10. Issues encountered (Testcontainers quirks, timing issues, etc.)
11. Next steps (Phase 37 frontend/E2E tests)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 35-A**: Unit tests (test utilities: TestDataBuilder, TestFixtures)
- **All feature phases (0-34)**: All backend code must be implemented

### Required By
- **Phase 38**: Performance Optimization (integration tests help identify slow queries)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Testcontainers dependency added for PostgreSQL 15, Redis 7, RabbitMQ 3
- [ ] Singleton container pattern configured for test performance
- [ ] Flyway migrations run successfully against test PostgreSQL container
- [ ] Test data seeding: users for all 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN)
- [ ] Test data seeding: at least 2 companies for multi-tenant testing
- [ ] Auth: register → login → get token flow works end-to-end
- [ ] Auth: refresh token flow works
- [ ] Auth: logout invalidates refresh token
- [ ] Auth: account lockout after N failed attempts
- [ ] RBAC: ADMIN can access all endpoints
- [ ] RBAC: MANAGER can access permitted endpoints, blocked from admin-only
- [ ] RBAC: ACCOUNTANT can access permitted endpoints, blocked from delete
- [ ] RBAC: INTERN can only view and create, blocked from edit/delete
- [ ] Multi-tenant: Company A user cannot see Company B invoices
- [ ] Multi-tenant: Company A user cannot modify Company B data
- [ ] Invoice CRUD: create, read, update, soft-delete tested
- [ ] Invoice status transitions: PENDING→VERIFIED, PENDING→REJECTED tested
- [ ] Filtering with JPA Specifications: date range, status, supplier, amount range tested against real DB
- [ ] Export: XLSX generation produces valid file
- [ ] Export: CSV generation produces valid file
- [ ] Audit log: created automatically on invoice create/update/delete
- [ ] Audit log: immutability verified (no update/delete on audit_logs)
- [ ] RabbitMQ: message publish verified
- [ ] RabbitMQ: message consume verified
- [ ] KVKK encryption: encrypted fields stored encrypted in DB, decrypted on read
- [ ] KVKK hash search: tax number lookup via hash works
- [ ] KVKK consent: grant, revoke, query tested
- [ ] KVKK right to be forgotten: anonymization tested
- [ ] Rate limiting: returns 429 when exceeded
- [ ] Version history: created on invoice update
- [ ] Version history: revert to previous version works
- [ ] Template learning: template created/updated on invoice verification
- [ ] Rule evaluation: rule triggers and applies action correctly
- [ ] Total integration test count ≥ 80
- [ ] All integration tests pass
- [ ] CI/CD runs integration tests
- [ ] Result file created at docs/OMER/step_results/faz_36a_result.md
---
## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Testcontainers configured for PostgreSQL 15, Redis 7, RabbitMQ 3
2. ✅ Singleton container pattern for fast test execution
3. ✅ Flyway migrations run successfully against test PostgreSQL
4. ✅ Test data seeding provides users for all 4 roles + multi-company setup
5. ✅ Auth flow fully tested (register, login, refresh, logout, lockout)
6. ✅ RBAC tested for all 4 roles across all endpoint categories
7. ✅ Multi-tenant isolation verified (Company A cannot access Company B)
8. ✅ Invoice CRUD + status transitions tested end-to-end
9. ✅ Filtering with JPA Specifications verified against real DB
10. ✅ Export produces valid XLSX/CSV files
11. ✅ Audit logs created automatically for all tracked operations
12. ✅ Audit log immutability verified
13. ✅ RabbitMQ message publish and consume tested
14. ✅ KVKK: encryption, hash search, consent, right to be forgotten, retention tested
15. ✅ Rate limiting returns 429 when exceeded
16. ✅ Version history and revert tested
17. ✅ Template learning and rule evaluation tested
18. ✅ Total integration test count ≥ 80
19. ✅ All integration tests pass
20. ✅ CI/CD runs integration tests
21. ✅ Result file created at docs/OMER/step_results/faz_36a_result.md

---

## IMPORTANT NOTES

1. **Testcontainers Requires Docker**: The CI/CD environment (GitHub Actions) must have Docker available. Use `ubuntu-latest` runner which includes Docker. Locally, developers need Docker Desktop running.

2. **Slower Than Unit Tests**: Integration tests are inherently slower (starting containers, DB operations). A full suite might take 2-5 minutes. This is acceptable. Don't make tests faster by reducing coverage.

3. **@Transactional Caveat**: When using `@Transactional` on test methods, the transaction is rolled back after each test. This is great for isolation. BUT: it means the changes are not committed, so things like auto-increment IDs and database triggers behave slightly differently. For tests that need committed data (e.g., testing async listeners), use `@Commit` or seed data in `@BeforeEach` without `@Transactional`.

4. **Test Order Independence**: Each test must be independent. Do not rely on other tests running first. Use `@BeforeEach` or `@BeforeAll` for setup.

5. **Auth Token Caching**: Getting a JWT token for each test (by calling login) is slow. Cache tokens per role in a `@BeforeAll` setup method. Tokens are valid for 15 minutes — plenty for a test run.

6. **Reuse Phase 35-A Utilities**: Use TestDataBuilder and TestFixtures from Phase 35-A. Do not duplicate test utility code.

7. **Real Database State Matters**: Unlike unit tests, integration tests can uncover issues with: database constraints, index behavior, Flyway migration ordering, JPA lazy loading, N+1 queries, and transaction boundaries. These are valuable findings.

8. **RabbitMQ Tests May Be Flaky**: Async message tests can be timing-sensitive. Use `Awaitility` library for polling assertions with timeouts rather than `Thread.sleep()`.

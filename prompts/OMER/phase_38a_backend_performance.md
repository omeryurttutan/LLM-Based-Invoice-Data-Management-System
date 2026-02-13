# PHASE 38-A: PERFORMANCE OPTIMIZATION — BACKEND

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid — Spring Boot (8080), Python FastAPI (8000), Next.js (3000)

### Current State (Phases 0-37 Completed)
All features implemented and tested (unit + integration + E2E). The system is functional but has not been explicitly optimized for performance.

### Existing Infrastructure
- **PostgreSQL 15**: Indexes created in Phase 3 (email, status, invoice_date, company_id composite)
- **Redis 7**: Used for JWT tokens (Phase 4), rate limiting (Phase 32), and basic dashboard caching (Phase 26)
- **RabbitMQ 3**: Async extraction pipeline
- **Phase 31**: AES-256-GCM encryption on sensitive fields + SHA-256 hash columns for searchable encrypted fields
- **Phase 23**: JPA Specifications for dynamic filtering
- **Phase 26**: Dashboard endpoints with 60-second Redis cache

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 2-3 days
- **Parallel**: FURKAN works on Phase 38-B (Frontend performance) simultaneously

---

## OBJECTIVE

Optimize backend performance across three layers:

1. **Database**: Identify and fix slow queries, add missing indexes, optimize JPA mappings
2. **Cache**: Expand Redis caching strategy beyond dashboard, implement proper invalidation
3. **Application**: Fix N+1 queries, optimize serialization, tune connection pools

---

## DETAILED REQUIREMENTS

### 1. Database Query Analysis & Optimization

**1.1 Enable Slow Query Logging:**

Configure PostgreSQL to log slow queries:
- Set `log_min_duration_statement = 200` (log queries taking > 200ms)
- Or configure Spring Boot: `spring.jpa.properties.hibernate.generate_statistics=true` to log query timing
- Add a `PerformanceInterceptor` or use `spring.jpa.show-sql=true` with timing in development

**1.2 Run EXPLAIN ANALYZE on Critical Queries:**

Identify the most frequently used queries and analyze their execution plans:

| Query | Expected Frequency | Target Time |
|---|---|---|
| Invoice list with filters (Phase 23) | Very High | < 100ms |
| Invoice count by status (dashboard) | High | < 50ms |
| Invoice monthly trend (dashboard) | High | < 100ms |
| Top suppliers aggregation (dashboard) | High | < 100ms |
| Unread notification count | Very High | < 20ms |
| Audit log query with filters | Medium | < 200ms |
| Template lookup by supplier tax hash | Medium | < 50ms |
| Rule evaluation query | Medium | < 50ms |
| User lookup by email | High | < 20ms |
| Category list by company | High | < 20ms |

For each query: run EXPLAIN ANALYZE, check if indexes are used, identify sequential scans.

**1.3 Add Missing Indexes (Flyway Migration):**

Based on EXPLAIN ANALYZE results, create a Flyway migration: `V{next}__phase_38_performance_indexes.sql`

Likely candidates for new or adjusted indexes:

- `invoices(company_id, status, invoice_date DESC)` — composite for filtered + sorted list
- `invoices(company_id, supplier_name)` — top suppliers aggregation
- `invoices(company_id, category_id)` — category distribution
- `invoices(company_id, created_at DESC)` — recent invoices
- `notifications(user_id, is_read, created_at DESC)` — unread notifications
- `audit_logs(company_id, created_at DESC)` — audit log pagination
- `invoice_versions(invoice_id, version_number DESC)` — version history
- `templates(company_id, supplier_tax_number_hash)` — template lookup
- `rules(company_id, is_active, priority)` — active rules sorted by priority
- `user_consents(user_id, consent_type, created_at DESC)` — consent lookup

Use partial indexes where applicable: `WHERE is_deleted = false` on frequently queried tables.

**1.4 Optimize Soft Delete Queries:**

All main entities use soft delete (`is_deleted` flag). Every query filters `WHERE is_deleted = false`. Consider:

- Adding partial indexes: `CREATE INDEX idx_invoices_active ON invoices(...) WHERE is_deleted = false`
- Verify that JPA `@Where(clause = "is_deleted = false")` or Hibernate filters are applied globally so all queries benefit

---

### 2. N+1 Query Detection & Fix

**2.1 Identify N+1 Problems:**

Common N+1 scenarios in this project:

- Invoice list → loading category for each invoice (N+1 on categories)
- Invoice list → loading items for each invoice (N+1 on invoice_items)
- Invoice detail → loading versions, items, category
- Notification list → loading user for each notification
- Dashboard → multiple separate queries instead of batch

**2.2 Fix with Fetch Strategies:**

- Use `@EntityGraph` on repository methods for list queries:
  ```
  @EntityGraph(attributePaths = {"category", "items"})
  Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);
  ```
- Or use JPQL `JOIN FETCH`:
  ```
  SELECT i FROM Invoice i JOIN FETCH i.category LEFT JOIN FETCH i.items WHERE ...
  ```
- For collections (items): use `@BatchSize(size = 20)` on `@OneToMany` to batch lazy loads

**2.3 Pagination + JOIN FETCH Caution:**

`JOIN FETCH` with `Pageable` causes "HHH90003004: firstResult/maxResults specified with collection fetch" warning. Solutions:
- Use `@EntityGraph` instead (works better with pagination)
- Or fetch IDs first (paginated), then load entities with `JOIN FETCH` by IDs (two-query approach)

---

### 3. Redis Cache Expansion

**3.1 Current Caching (Phase 26):**
Dashboard endpoints are cached with 60-second TTL per company + params.

**3.2 Expand Caching to More Endpoints:**

| Data | Cache Key Pattern | TTL | Invalidation |
|---|---|---|---|
| Category list (per company) | `categories:{companyId}` | 5 minutes | On category CRUD |
| Invoice count by status | `invoice-counts:{companyId}` | 30 seconds | On invoice status change |
| Unread notification count | `unread-count:{userId}` | 10 seconds | On new notification / mark read |
| Template list (per company) | `templates:{companyId}` | 5 minutes | On template update |
| Active rules (per company) | `rules:{companyId}:active` | 5 minutes | On rule CRUD |
| User profile | `user-profile:{userId}` | 10 minutes | On profile update |
| Company info | `company:{companyId}` | 10 minutes | On company update |

**3.3 Cache Invalidation Strategy:**

Two approaches (choose or combine):

**Event-Based Invalidation:**
- When an entity is created/updated/deleted, explicitly delete related cache keys
- Use Spring `@CacheEvict` or manual `redisTemplate.delete(key)`
- More complex but data is always fresh

**TTL-Based Expiration:**
- Set short TTLs (10-60 seconds) and let them expire naturally
- Simpler to implement, slight staleness acceptable
- Good for: dashboard data, counts, non-critical lists

**Recommended**: Use TTL-based for most caches (this is a graduation project — simplicity wins). Use event-based only for critical data where staleness causes confusion (e.g., notification count).

**3.4 Spring Cache Abstraction:**

Use `@Cacheable`, `@CacheEvict`, `@CachePut` annotations with Redis cache manager:

```java
@Cacheable(value = "categories", key = "#companyId")
public List<CategoryDto> getCategoriesByCompany(UUID companyId) { ... }

@CacheEvict(value = "categories", key = "#companyId")
public void createCategory(UUID companyId, CreateCategoryRequest request) { ... }
```

Configure `RedisCacheManager` with different TTLs per cache name.

---

### 4. Connection Pool Tuning

**4.1 HikariCP (Database):**

Tune connection pool settings in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 20000     # 20 seconds
      idle-timeout: 300000          # 5 minutes
      max-lifetime: 1200000         # 20 minutes
      leak-detection-threshold: 60000  # 60 seconds (detect connection leaks)
```

**4.2 Redis Connection Pool (Lettuce):**

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 2000ms
```

**4.3 RabbitMQ:**

Verify connection pool settings are reasonable. Default Spring AMQP settings are usually fine for this project.

---

### 5. Query Optimization Specifics

**5.1 Keyset Pagination (for Large Datasets):**

Standard offset-based pagination (`OFFSET 1000 LIMIT 20`) becomes slow for large offsets because the database reads and discards 1000 rows.

For the invoice list (Phase 23), if data grows large, consider keyset (cursor) pagination:

```sql
-- Instead of: SELECT * FROM invoices ORDER BY created_at DESC OFFSET 1000 LIMIT 20
-- Use: SELECT * FROM invoices WHERE created_at < :lastSeenDate ORDER BY created_at DESC LIMIT 20
```

For MVP: Document this as a potential optimization. Only implement if the dataset exceeds 10,000 records per company and pagination becomes noticeably slow. Standard offset pagination is fine for a graduation project.

**5.2 Projection Queries (DTOs):**

For list endpoints, select only needed columns instead of loading full entities:

```java
// Instead of loading full Invoice entity with all fields
@Query("SELECT new com.faturaocr.application.invoice.InvoiceListDto(i.id, i.invoiceNumber, i.supplierName, i.invoiceDate, i.totalAmount, i.currency, i.status, c.name) " +
       "FROM Invoice i LEFT JOIN i.category c WHERE ...")
Page<InvoiceListDto> findInvoiceList(Specification<Invoice> spec, Pageable pageable);
```

This avoids loading: items array, version history, encrypted fields, file paths — which are not needed for the list view.

**5.3 Dashboard Aggregation Optimization:**

Dashboard queries should use native SQL or JPQL aggregations, NOT load entities into Java and aggregate in memory:

```sql
-- Good: aggregation in DB
SELECT status, COUNT(*), SUM(total_amount)
FROM invoices
WHERE company_id = :companyId AND is_deleted = false
GROUP BY status

-- Bad: load all invoices, then group in Java
```

Verify all Phase 26 dashboard queries follow this pattern.

---

### 6. LLM Service Load Behavior (Python)

This section requires coordination with FURKAN. Test the Python extraction service under concurrent load:

**6.1 Concurrent Request Behavior:**
- Send 5 concurrent extraction requests
- Verify all complete without error
- Measure: total time, per-request time, resource usage

**6.2 LLM API Rate Limit Handling:**
- If multiple requests hit the LLM API simultaneously, rate limits may trigger
- Verify the fallback chain handles 429 correctly under load

**6.3 RabbitMQ Queue Backlog:**
- Queue 20 messages rapidly
- Verify consumer processes all without crashing
- Measure: queue drain time, memory usage

For MVP: Run these tests manually and document results. Do NOT build a complex load testing framework — that's overkill for a graduation project.

---

### 7. Encryption Performance Impact (Phase 31)

Phase 31 added AES-256-GCM encryption on sensitive fields. Measure its impact:

- List 100 invoices (each has encrypted supplier_tax_number) → measure time with vs without encryption
- If encryption adds > 100ms to list queries, consider: caching decrypted values in-memory for the current request, or loading encrypted fields lazily

For MVP: Document the measured impact. Only optimize if it's a noticeable problem.

---

### 8. Performance Monitoring Endpoints

Add lightweight performance monitoring:

**GET /api/v1/admin/performance/health** (ADMIN only):

Returns:
- Database connection pool stats (active, idle, total, waiting)
- Redis connection pool stats
- Average query time (last 100 queries)
- Cache hit/miss ratio
- JVM memory usage (heap used, max, GC count)
- Uptime

Use Spring Boot Actuator metrics where possible (`/actuator/metrics`).

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_38a_result.md`

The result file must include:

1. Phase summary
2. Slow query analysis results (EXPLAIN ANALYZE outputs for critical queries)
3. Indexes added (Flyway migration details)
4. N+1 queries identified and fixed
5. Redis cache expansion details (what's cached, TTLs, invalidation strategy)
6. Connection pool settings
7. Before/after performance measurements for key endpoints
8. Encryption performance impact measurement
9. LLM service load test results (coordinated with FURKAN)
10. Performance monitoring endpoint details
11. Keyset pagination assessment (needed or not)
12. Issues encountered and solutions
13. Next steps (Phase 39 Deployment)

---

## DEPENDENCIES

### Requires (must be completed first)
- **All feature phases (0-34)**: All code implemented
- **Phase 35-37**: Tests pass (don't break tests during optimization)
- **Phase 36**: Integration test baseline metrics

### Required By
- **Phase 39**: Deployment (optimized application for production)
- **Phase 40**: Monitoring (performance endpoints feed monitoring)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Slow query logging enabled (log queries > 200ms)
- [ ] EXPLAIN ANALYZE run on top 10 critical queries (documented)
- [ ] Missing indexes identified and added via Flyway migration
- [ ] Partial indexes created for soft-deleted entities (WHERE is_deleted = false)
- [ ] N+1 queries identified via Hibernate statistics or log analysis
- [ ] N+1 queries fixed with @EntityGraph or JOIN FETCH
- [ ] Redis cache: categories cached
- [ ] Redis cache: unread notification count cached
- [ ] Redis cache: supplier templates cached
- [ ] Redis cache: active rules cached
- [ ] Redis cache: user profile cached
- [ ] Cache invalidation: category cache cleared on create/update/delete
- [ ] Cache invalidation: notification count cleared on new notification
- [ ] Spring @Cacheable / @CacheEvict annotations applied correctly
- [ ] HikariCP pool settings tuned (documented values)
- [ ] Redis connection pool settings tuned (documented values)
- [ ] Invoice list endpoint responds < 100ms (for 1000 records, measured)
- [ ] Dashboard endpoints respond < 200ms (measured)
- [ ] Unread notification count responds < 20ms (measured)
- [ ] Encryption performance impact measured and documented
- [ ] GET /api/v1/admin/performance/health endpoint created and returns all metrics
- [ ] All existing tests still pass (no regressions)
- [ ] Result file created at docs/OMER/step_results/faz_38a_result.md
---

## SUCCESS CRITERIA

1. ✅ Slow query logging enabled and analyzed
2. ✅ EXPLAIN ANALYZE run on top 10 critical queries
3. ✅ Missing indexes identified and added via Flyway migration
4. ✅ Partial indexes for soft-deleted entities
5. ✅ N+1 queries identified and fixed (EntityGraph or JOIN FETCH)
6. ✅ Redis cache expanded: categories, notification count, templates, rules, user profile
7. ✅ Cache invalidation strategy documented and implemented
8. ✅ Spring Cache annotations applied to cached services
9. ✅ HikariCP and Redis connection pools tuned
10. ✅ Invoice list endpoint responds in < 100ms (for 1000 records)
11. ✅ Dashboard endpoints respond in < 200ms
12. ✅ Unread notification count responds in < 20ms
13. ✅ Encryption performance impact documented
14. ✅ Performance health endpoint created
15. ✅ All existing tests still pass (no regressions)
16. ✅ Result file created at docs/OMER/step_results/faz_38a_result.md

---

## IMPORTANT NOTES

1. **Measure Before Optimizing**: Always measure first, then optimize. Don't add indexes or caches based on intuition — base it on EXPLAIN ANALYZE and timing data. Document before/after numbers.

2. **Don't Break Tests**: All Phase 35-37 tests must pass after optimization. Run the full test suite after changes.

3. **Cache Consistency vs Performance**: For a graduation project, slight cache staleness (10-60 seconds) is acceptable. Don't over-engineer cache invalidation with complex event systems.

4. **Connection Pool Sizing**: Pool sizes depend on expected concurrent users. For a graduation demo, 5-20 connections is plenty. Don't over-allocate.

5. **Keyset Pagination is Optional**: Standard offset pagination works fine for < 10K records per company. Document it as a "future optimization" if not implemented.

6. **Projection Queries are High-Value**: Replacing full entity loads with DTO projections on list endpoints is one of the highest-impact optimizations. Prioritize this.

7. **Coordinate with FURKAN**: The LLM service load test (section 6) requires both developers. Plan a time to run this together.

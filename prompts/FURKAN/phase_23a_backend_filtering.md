# PHASE 23-A: BACKEND — ADVANCED FILTERING AND SEARCH API

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-22 Completed)
- ✅ Phase 0-3: Docker environment, CI/CD, Hexagonal Architecture, Database schema (companies, users, invoices, invoice_items, categories, audit_logs, batch_jobs — Flyway migrations, soft delete, indexes, composite indexes on invoice_number+company_id, status+invoice_date)
- ✅ Phase 4-6: JWT Auth (access/refresh tokens, Redis, BCrypt), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Company & User Management
- ✅ Phase 7: Invoice CRUD API — manual invoice creation, listing (paginated, sorted), updating, soft deletion, status workflow (PENDING → VERIFIED / REJECTED → PENDING), category management, company-scoped multi-tenant isolation. Current GET /invoices supports: page, size, sort, status (single value), categoryId (single value)
- ✅ Phase 8-9: Audit Log (immutable), Duplication Control (invoice_number + company_id)
- ✅ Phase 10-12: Frontend — Layout, Auth pages, Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms
- ✅ Phase 13-19: Python extraction service — FastAPI setup, image preprocessing, Gemini/GPT/Claude LLM integration, fallback chain, response validation & confidence score, e-Invoice XML parser, RabbitMQ async processing
- ✅ Phase 20: File Upload Infrastructure — single/bulk upload endpoints, ZIP support, batch job tracking, file serving, database migration for file columns
- ✅ Phase 21: Upload UI — drag-and-drop, progress tracking, batch status polling
- ✅ Phase 22: Verification UI — split-view document comparison, inline editing, confidence display, verify/reject workflow, correction tracking

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer — Backend focused)
- **Estimated Duration**: 2 days

---

## OBJECTIVE

Enhance the existing GET /api/v1/invoices endpoint with comprehensive filtering and full-text search capabilities using Spring Data JPA Specifications. The current endpoint only supports basic pagination, sorting, and single-value status/category filters. This phase adds multi-value filters, date ranges, amount ranges, text search, and combines all filters with AND logic — enabling the frontend (Phase 23-B) to build a powerful filter panel.

**Important**: This phase modifies the EXISTING endpoint, not a new one. The GET /api/v1/invoices endpoint must remain backward-compatible — existing calls without new filter params must work exactly as before.

---

## EXISTING ENDPOINT STATE (Phase 7)

The current GET /api/v1/invoices endpoint accepts:
- `page` (int, default 0)
- `size` (int, default 20, max 100)
- `sort` (string, e.g., "invoiceDate,desc")
- `status` (single InvoiceStatus enum value — optional)
- `categoryId` (single UUID — optional)

The repository currently uses `findAllByCompanyId(UUID, Pageable)` and `findAllByCompanyIdAndStatus(UUID, InvoiceStatus, Pageable)` — simple Spring Data derived query methods with no dynamic query building.

---

## DETAILED REQUIREMENTS

### 1. Spring Data JPA Specification Infrastructure

**Purpose**: Replace the current simple repository queries with a flexible, composable Specification-based approach that supports any combination of filters.

**Create an InvoiceSpecification utility class** in the infrastructure layer that provides static methods to build individual filter specifications. Each method returns a `Specification<InvoiceJpaEntity>` that can be combined with AND logic.

**Package location**: `infrastructure/persistence/invoice/InvoiceSpecification.java`

The specification builder must:
- Always include the company isolation filter (companyId = current user's company) — this is non-negotiable for multi-tenant security
- Always exclude soft-deleted records (is_deleted = false)
- Combine all active filters with AND logic
- Return all results when no filters are provided (just company + not-deleted)
- Handle null/empty filter values gracefully (skip the filter if value is null or empty)

### 2. Filter Parameters — New Query Parameters

Extend the GET /api/v1/invoices endpoint to accept the following additional query parameters. All are optional. When multiple filters are provided, they combine with AND logic.

**Date Filters:**
- `dateFrom` (String, ISO date format YYYY-MM-DD) — invoices with invoice_date >= this date
- `dateTo` (String, ISO date format YYYY-MM-DD) — invoices with invoice_date <= this date
- These two work as a date range. Either one can be used alone (open-ended range).

**Status Filter (Enhanced):**
- `status` (String, comma-separated) — support multiple statuses. Example: `status=PENDING,REJECTED` returns invoices with EITHER status. This replaces the current single-value filter while remaining backward-compatible (single value still works).

**Supplier Filter:**
- `supplierName` (String, comma-separated supplier names) — filter by one or more supplier names. Use case-insensitive LIKE matching for each name, combined with OR logic between suppliers. Example: `supplierName=ABC Ltd.,XYZ Corp.`

**Category Filter (Enhanced):**
- `categoryId` (String, comma-separated UUIDs) — support multiple categories. Example: `categoryId=uuid1,uuid2`

**Amount Range Filters:**
- `amountMin` (BigDecimal) — invoices with total_amount >= this value
- `amountMax` (BigDecimal) — invoices with total_amount <= this value

**Currency Filter:**
- `currency` (String, comma-separated) — filter by currency. Example: `currency=TRY,USD`

**Source Type Filter:**
- `sourceType` (String, comma-separated) — filter by source type. Example: `sourceType=LLM,E_INVOICE`. Valid values: LLM, E_INVOICE, MANUAL

**LLM Provider Filter:**
- `llmProvider` (String, comma-separated) — filter by LLM provider. Example: `llmProvider=GEMINI,GPT`. Only applicable when sourceType includes LLM.

**Confidence Score Filter:**
- `confidenceMin` (Double, 0-100) — invoices with confidence_score >= this value
- `confidenceMax` (Double, 0-100) — invoices with confidence_score <= this value

**Full-Text Search:**
- `search` (String) — searches across multiple text fields simultaneously: invoice_number, supplier_name, buyer_name, notes. Use case-insensitive LIKE (containing) matching with OR logic across fields. Example: `search=FTR-2026` matches invoices where invoice_number contains "FTR-2026" OR supplier_name contains "FTR-2026" OR buyer_name contains "FTR-2026" OR notes contains "FTR-2026".

**Created By Filter:**
- `createdByUserId` (UUID) — filter by the user who created the invoice

**Date Created Range:**
- `createdFrom` (String, ISO datetime) — invoices created after this timestamp
- `createdTo` (String, ISO datetime) — invoices created before this timestamp

### 3. Implementation — Specification Methods

Create individual specification methods for each filter. Each method should:
- Accept the filter value as parameter
- Return `Specification<InvoiceJpaEntity>`
- Return null or a no-op specification when the filter value is null/empty

**Specification methods to implement:**

- `hasCompanyId(UUID companyId)` — mandatory company isolation
- `isNotDeleted()` — mandatory soft delete exclusion
- `hasDateRange(LocalDate dateFrom, LocalDate dateTo)` — invoice_date range
- `hasStatuses(List<InvoiceStatus> statuses)` — IN clause for multiple statuses
- `hasSupplierNames(List<String> names)` — LIKE matching with OR for each name
- `hasCategoryIds(List<UUID> categoryIds)` — IN clause for categories
- `hasAmountRange(BigDecimal min, BigDecimal max)` — total_amount range
- `hasCurrencies(List<Currency> currencies)` — IN clause for currencies
- `hasSourceTypes(List<SourceType> sourceTypes)` — IN clause for source types
- `hasLlmProviders(List<String> providers)` — IN clause for LLM providers
- `hasConfidenceRange(Double min, Double max)` — confidence_score range
- `searchText(String query)` — OR across invoice_number, supplier_name, buyer_name, notes with LIKE
- `hasCreatedByUser(UUID userId)` — exact match on created_by_user_id
- `hasCreatedDateRange(LocalDateTime from, LocalDateTime to)` — created_at range

### 4. Filter Request DTO

Create a dedicated DTO to collect all filter parameters from the HTTP request:

**File**: `interfaces/rest/invoice/dto/InvoiceFilterRequest.java`

This DTO should:
- Use `@RequestParam` annotations for each filter parameter
- Include default values where appropriate (e.g., null for all optional filters)
- Include validation annotations where needed (@DateTimeFormat, @DecimalMin, etc.)
- Provide a convenience method to build the combined Specification from all non-null filters

### 5. Updated Controller

Modify the `InvoiceController.java` — the existing `GET /api/v1/invoices` method should:
- Accept the new InvoiceFilterRequest alongside existing pagination/sort params
- Build a Specification from the filter request
- Pass the Specification to the service/repository
- Maintain backward compatibility (no filters = all invoices for the company)

### 6. Updated Repository

Extend the InvoiceJpaRepository to use Specifications:
- Extend `JpaSpecificationExecutor<InvoiceJpaEntity>` (in addition to existing JpaRepository)
- Use `findAll(Specification<InvoiceJpaEntity>, Pageable)` for the filtered query

### 7. Service Layer Update

Update `InvoiceService.listInvoices()` to:
- Accept the filter DTO (or the built Specification) alongside pagination
- Always prepend the mandatory company isolation and soft-delete exclusion specifications
- Build the combined specification and pass to repository
- Return the paginated result

### 8. Supplier Name Autocomplete Endpoint

Add a new endpoint to support the frontend's supplier filter dropdown:

**GET /api/v1/invoices/suppliers**

- Returns a distinct list of supplier names for the current user's company
- Accepts an optional `search` query parameter for autocomplete/typeahead
- Returns a simple array of strings (supplier names)
- Company-scoped (only suppliers from invoices belonging to the user's company)
- Excludes soft-deleted invoices
- Ordered alphabetically
- Limited to 50 results (for performance)

### 9. Filter Metadata Endpoint

Add a new endpoint to provide filter options to the frontend:

**GET /api/v1/invoices/filter-options**

Returns available filter values for the current user's company:
- `statuses`: list of all InvoiceStatus enum values with their display names
- `categories`: list of active categories (id + name) for the company
- `currencies`: list of Currency enum values
- `sourceTypes`: list of SourceType enum values
- `llmProviders`: list of distinct LLM providers used in the company's invoices
- `amountRange`: { min, max } — the actual min and max total_amount in the company's invoices
- `dateRange`: { earliest, latest } — the earliest and latest invoice_date in the company
- `confidenceRange`: { min, max } — actual confidence score range

This endpoint helps the frontend build dynamic filter dropdowns and set appropriate slider ranges.

### 10. Database Considerations

**Check existing indexes**: Phase 3 already created indexes on status, invoice_date, company_id. Verify these are sufficient for the new filter queries.

**New indexes that may be needed:**
- Index on `source_type` if not already indexed
- Index on `llm_provider` if not already indexed
- Index on `currency` if not already indexed
- Index on `confidence_score` if not already indexed
- Index on `total_amount` if not already indexed
- Composite index on `company_id + invoice_date` for date-range queries (may already exist)
- Full-text index on `supplier_name` for LIKE queries (optional — standard LIKE with index may suffice for project scale)

If new indexes are needed, create a Flyway migration: `V{next_number}__phase_23_filter_indexes.sql`

**Important**: Only add indexes that are genuinely missing. Check what Phase 3 already created before adding duplicates. Document your findings in the result file.

### 11. Performance Considerations

- All queries must be company-scoped (no full table scans across all companies)
- For the `search` parameter with LIKE queries: use `LOWER()` function for case-insensitive matching, or consider `ILIKE` (PostgreSQL-specific)
- For large datasets: ensure pagination is applied AFTER all filters (the Specification + Pageable approach handles this correctly)
- The supplier autocomplete endpoint must be fast (< 200ms) — consider caching with Redis if the dataset is large
- Log slow queries (> 500ms) at WARN level

### 12. Logging

**INFO:**
- Filter request received with active filter summary (e.g., "Filtering invoices: status=[PENDING,REJECTED], dateFrom=2026-01-01, search=ABC")
- Result count for filtered query

**DEBUG:**
- Full filter parameters
- Generated SQL query (Spring Data JPA query logging)

**WARNING:**
- Slow query detection (> 500ms)
- Invalid filter parameter values (logged and ignored, not thrown)

---

## TESTING REQUIREMENTS

### 1. Unit Tests

- Each specification method individually: verify correct predicate generation
- Combined specifications: verify AND logic
- Null/empty filter handling: verify graceful skip
- Company isolation: verify specification always includes companyId filter

### 2. Integration Tests

- Filter by single status → correct results
- Filter by multiple statuses → correct results (OR within statuses)
- Date range filter → only invoices within range
- Amount range filter → only invoices within amount range
- Supplier name filter (case-insensitive) → correct matching
- Category filter (multiple categories) → correct results
- Full-text search across multiple fields → matches in any field
- Combined filters (date range + status + supplier) → correct AND combination
- Empty filters → all invoices (backward compatibility)
- Currency filter → correct filtering
- Source type filter → correct filtering
- Confidence score range → correct filtering
- Supplier autocomplete endpoint → returns distinct names, filtered by search term
- Filter options endpoint → returns correct metadata
- RBAC: all roles can use filters (filtering is a read operation)
- Multi-tenant: filters only return invoices from the user's company (never cross-company)

### 3. Performance Tests (Manual)

- Filter query on 1000+ invoices completes within 500ms
- Supplier autocomplete returns within 200ms
- Filter options endpoint returns within 300ms

---

## RESULT FILE

When this phase is complete, create a result file at:

**`docs/OMER/step_results/faz_23.0_result.md`**

The result file must contain:

### 1. Execution Summary
- Phase number (23-A), assigned developer, start/end dates
- Execution status (COMPLETED / PARTIAL / BLOCKED)
- Total time spent

### 2. Completed Tasks Checklist
- [ ] InvoiceSpecification utility class with all filter methods
- [ ] InvoiceFilterRequest DTO with all query parameters
- [ ] Updated InvoiceController with filter parameters
- [ ] Updated InvoiceJpaRepository with JpaSpecificationExecutor
- [ ] Updated InvoiceService with Specification-based listing
- [ ] Supplier autocomplete endpoint (GET /invoices/suppliers)
- [ ] Filter options endpoint (GET /invoices/filter-options)
- [ ] Database migration for new indexes (if needed)
- [ ] Backward compatibility verified
- [ ] Unit tests for specifications
- [ ] Integration tests for all filter combinations
- [ ] Multi-tenant isolation verified

### 3. Files Created/Modified
List every file with full path and description.

### 4. API Documentation

Document all query parameters for GET /api/v1/invoices:

| Parameter | Type | Example | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| ... | ... | ... | ... |

Document the new endpoints:
- GET /api/v1/invoices/suppliers
- GET /api/v1/invoices/filter-options

Include example requests and responses for each.

### 5. Database Changes
- List any new migration files
- List any new indexes added
- Explain which existing indexes were sufficient

### 6. Specification Architecture
- How specifications are composed
- How mandatory filters (company + not-deleted) are always applied
- How null parameters are handled

### 7. Test Results
- Unit test output summary
- Integration test output summary
- Number of tests passed/failed

### 8. Performance Observations
- Query execution times for various filter combinations
- Any slow queries identified and addressed
- Index effectiveness notes

### 9. Issues Encountered
Problems and their solutions

### 10. Next Steps
- What Phase 23-B (Frontend Filter Panel) needs from this phase (list all query params, endpoints, response formats)
- What Phase 24 (Export) needs — export will reuse the same filters
- Any optimization opportunities identified

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 3**: Database schema — invoices, invoice_items, categories tables with existing indexes
- **Phase 5**: RBAC — CompanyContextHolder for company isolation
- **Phase 7**: Invoice CRUD API — existing GET /invoices endpoint, InvoiceController, InvoiceService, InvoiceJpaRepository, DTOs

### Required By
- **Phase 23-B**: Frontend Filter Panel — will consume all query parameters and new endpoints defined here
- **Phase 24**: Export Module — will reuse the same filter parameters for filtered export
- **Phase 26**: Dashboard — may use filter aggregation queries
---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] GET /api/v1/invoices without new params returns same results as before (backward compatible)
- [ ] `status` param accepts comma-separated values (e.g., PENDING,VERIFIED)
- [ ] `dateFrom` and `dateTo` filter by invoice_date range correctly
- [ ] `dateFrom` alone works (open-ended range)
- [ ] `dateTo` alone works (open-ended range)
- [ ] `amountMin` and `amountMax` filter by total_amount correctly
- [ ] `supplierName` supports multiple comma-separated values with case-insensitive matching
- [ ] `categoryId` supports multiple comma-separated UUIDs
- [ ] `currency` filter works (TRY, USD, EUR)
- [ ] `sourceType` filter works (LLM, E_INVOICE, MANUAL)
- [ ] `llmProvider` filter works (GEMINI, GPT, CLAUDE)
- [ ] `confidenceMin` and `confidenceMax` filter by confidence_score range
- [ ] `search` param performs full-text search across invoice_number, supplier_name, buyer_name, notes
- [ ] All filters combine with AND logic (multiple filters narrow results)
- [ ] Company isolation is enforced (tested with two different company users)
- [ ] Soft-deleted records are excluded from all filter results
- [ ] GET /api/v1/invoices/suppliers returns distinct supplier names for current company
- [ ] GET /api/v1/invoices/filter-options returns dynamic metadata (status counts, category list, etc.)
- [ ] New database indexes created via Flyway migration
- [ ] No duplicate indexes created (checked against Phase 3 indexes)
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] `mvn test` or `gradle test` completes without failures
- [ ] Result file created at docs/OMER/step_results/faz_23.0_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ GET /api/v1/invoices accepts all new filter parameters
2. ✅ Existing calls without new params work exactly as before (backward compatible)
3. ✅ Multiple statuses can be filtered simultaneously (comma-separated)
4. ✅ Date range filtering works with dateFrom and/or dateTo
5. ✅ Amount range filtering works with amountMin and/or amountMax
6. ✅ Supplier name filter supports multiple names with case-insensitive matching
7. ✅ Category filter supports multiple category IDs
8. ✅ Currency, source type, and LLM provider filters work correctly
9. ✅ Confidence score range filter works correctly
10. ✅ Full-text search matches across invoice_number, supplier_name, buyer_name, notes
11. ✅ All filters combine with AND logic
12. ✅ Company isolation is ALWAYS enforced (no cross-tenant data leakage)
13. ✅ Soft-deleted records are ALWAYS excluded
14. ✅ GET /api/v1/invoices/suppliers returns distinct supplier names with autocomplete
15. ✅ GET /api/v1/invoices/filter-options returns dynamic filter metadata
16. ✅ Necessary database indexes are added via Flyway migration
17. ✅ All unit and integration tests pass
18. ✅ Result file is created at docs/OMER/step_results/faz_23.0_result.md

---

## IMPORTANT NOTES

1. **Backward Compatibility is Critical**: The existing frontend (Phase 12) already calls GET /invoices with page, size, sort, status, categoryId. These must continue to work exactly as before. The new filter params are purely additive.

2. **Specification Pattern — Not Repository Method Proliferation**: Do NOT create separate repository methods like `findByStatusAndDateRangeAndSupplierName(...)`. Use the Specification pattern to compose queries dynamically. This keeps the repository clean and supports any filter combination.

3. **Comma-Separated Multi-Value Params**: For parameters that accept multiple values (status, supplierName, categoryId, currency, sourceType, llmProvider), use comma separation in a single query parameter. Parse them in the controller or DTO. Example: `?status=PENDING,REJECTED&currency=TRY,USD`

4. **SQL Injection Prevention**: All filter values must be parameterized through JPA Specifications (which use PreparedStatement parameters). Never concatenate user input into queries directly.

5. **Case-Insensitive Search**: For the `search` and `supplierName` filters, use `LOWER()` on both the column and the search term, or use PostgreSQL's `ILIKE` operator. JPA Criteria API has `criteriaBuilder.lower()` for this.

6. **Empty String Handling**: Treat empty strings the same as null — skip the filter. Don't let `?search=` (empty) cause issues.

7. **Filter Options Caching**: The filter-options endpoint can be cached briefly (30-60 seconds) with Redis since the data changes infrequently. This is optional but recommended if it becomes a performance issue.

8. **Phase 24 Reuse**: The export module (Phase 24) will pass the exact same filter parameters to retrieve filtered data for XLSX/CSV export. Design the specification building logic in a reusable way (service method that both listing and export can call).

---

**Phase 23-A Completion Target**: A fully functional, backward-compatible advanced filtering API using Spring Data JPA Specifications, with all filter parameters, helper endpoints (supplier autocomplete, filter options), proper indexing, and comprehensive test coverage — ready for the frontend filter panel in Phase 23-B.

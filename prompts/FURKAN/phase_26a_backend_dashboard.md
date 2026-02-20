# PHASE 26-A: BACKEND — DASHBOARD STATISTICS API

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 — LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-25 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database (invoices, invoice_items, categories, companies, users, audit_logs — with composite indexes on company_id+status, company_id+invoice_date, status+invoice_date), Auth, RBAC, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend — Layout, Auth, Invoice list with TanStack Query
- ✅ Phase 13-19: Python extraction pipeline — LLM integration, fallback chain, validation, XML parser, RabbitMQ
- ✅ Phase 20-22: File Upload, Verification UI
- ✅ Phase 23: Advanced Filtering — JPA Specifications + Frontend filter panel
- ✅ Phase 24-25: Export Module (XLSX/CSV + Logo/Mikro/Netsis/Luca accounting formats)

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer — Backend)
- **Estimated Duration**: 1-2 days

### Relevant Database Schema

**invoices table** key columns for statistics:
- `company_id` (UUID) — multi-tenant isolation
- `status` (VARCHAR) — PENDING, VERIFIED, REJECTED, PROCESSING
- `source_type` (VARCHAR) — LLM, E_INVOICE, MANUAL
- `llm_provider` (VARCHAR) — GEMINI, GPT, CLAUDE
- `confidence_score` (DECIMAL 5,2) — 0-100
- `total_amount` (DECIMAL 15,2)
- `currency` (VARCHAR 3) — TRY, USD, EUR, GBP
- `category_id` (UUID FK → categories)
- `supplier_name` (VARCHAR 255)
- `invoice_date` (DATE)
- `created_at` (TIMESTAMPTZ)
- `verified_at` (TIMESTAMPTZ)
- `rejected_at` (TIMESTAMPTZ)
- `is_deleted` (BOOLEAN) — soft delete

**Existing indexes useful for dashboard queries:**
- `idx_invoices_company_status` — company_id + status (partial: is_deleted = FALSE)
- `idx_invoices_company_date` — company_id + invoice_date DESC (partial)
- `idx_invoices_status_date` — status + invoice_date DESC (partial)

---

## OBJECTIVE

Create backend API endpoints that provide aggregated statistics and metrics for the dashboard frontend (Phase 26-B). The endpoints must return company-scoped, pre-aggregated data optimized for dashboard rendering — including summary KPIs, category distribution, monthly trends, top suppliers, status breakdown, and a pending actions list. All queries must respect multi-tenant isolation and exclude soft-deleted records.

---

## DETAILED REQUIREMENTS

### 1. Dashboard Summary Endpoint

**GET /api/v1/dashboard/stats**

**Query Parameters:**
- `dateFrom` (optional, ISO date YYYY-MM-DD) — filter stats by date range start (invoice_date)
- `dateTo` (optional, ISO date YYYY-MM-DD) — filter stats by date range end (invoice_date)
- `currency` (optional, default "TRY") — currency for amount aggregations. Only invoices in the specified currency are included in amount calculations.

**Response:**

The response includes overall summary counts, source type breakdown, and confidence score statistics.

- `period` object: the applied dateFrom, dateTo, and currency
- `summary` object:
  - `totalInvoices` (int) — total count of all invoices (regardless of currency)
  - `totalAmount` (decimal) — sum of total_amount for invoices matching the currency
  - `averageAmount` (decimal) — average total_amount for matching currency invoices
  - `pendingCount` (int) — invoices with status PENDING
  - `pendingAmount` (decimal) — sum of pending invoices matching currency
  - `verifiedCount` (int) — invoices with status VERIFIED
  - `verifiedAmount` (decimal) — sum of verified invoices matching currency
  - `rejectedCount` (int) — invoices with status REJECTED
  - `processingCount` (int) — invoices with status PROCESSING
- `sourceBreakdown` object: keyed by source_type (LLM, E_INVOICE, MANUAL), each with count and percentage
- `confidenceStats` object:
  - `averageScore` (decimal) — average confidence_score for LLM invoices
  - `highConfidence` (int) — count with score 90-100
  - `mediumConfidence` (int) — count with score 70-89
  - `lowConfidence` (int) — count with score 0-69

**Implementation Notes:**
- Count fields include ALL currencies; amount fields only sum invoices matching the requested currency
- Percentage in sourceBreakdown is calculated from totalInvoices
- confidenceStats only considers invoices with source_type = LLM

### 2. Category Distribution Endpoint

**GET /api/v1/dashboard/categories**

**Query Parameters:** `dateFrom`, `dateTo`, `currency` (same as stats)

**Response:** An array of category objects, each containing:
- `categoryId` (UUID or null for uncategorized)
- `categoryName` (string — "Kategorisiz" for null category)
- `categoryColor` (string — hex color from categories table, "#9CA3AF" for uncategorized)
- `invoiceCount` (int)
- `totalAmount` (decimal)
- `percentage` (decimal) — relative to total amount across all categories

**Implementation Notes:**
- GROUP BY category_id, LEFT JOIN categories for name/color
- Include NULL category_id as "Kategorisiz" (Uncategorized)
- Sort by totalAmount descending
- Limit to top 10 categories; aggregate remaining as "Diğer" (Other) if more than 10

### 3. Monthly Trend Endpoint

**GET /api/v1/dashboard/monthly-trend**

**Query Parameters:**
- `months` (optional, int, default 12) — how many months to return
- `currency` (optional, default "TRY")

**Response:** An array of month objects (reverse chronological), each containing:
- `month` (string, "YYYY-MM" format)
- `label` (string, Turkish month name + year, e.g., "Şubat 2026")
- `invoiceCount` (int)
- `totalAmount` (decimal)
- `verifiedAmount` (decimal) — only VERIFIED invoices
- `averageAmount` (decimal)

**Implementation Notes:**
- GROUP BY YEAR(invoice_date), MONTH(invoice_date)
- Return the last N months in reverse chronological order
- Fill in months with zero invoices for continuous chart rendering (the frontend needs unbroken data points)
- Turkish month labels: Ocak, Şubat, Mart, Nisan, Mayıs, Haziran, Temmuz, Ağustos, Eylül, Ekim, Kasım, Aralık
- `verifiedAmount` only sums invoices with status VERIFIED for that month

### 4. Top Suppliers Endpoint

**GET /api/v1/dashboard/top-suppliers**

**Query Parameters:**
- `dateFrom`, `dateTo`, `currency` (same as stats)
- `limit` (optional, int, default 10, max 20)

**Response:**
- `suppliers` array: each with supplierName, supplierTaxNumber, invoiceCount, totalAmount, percentage
- `othersCount` (int) — count of invoices from suppliers not in top list
- `othersAmount` (decimal) — total amount from other suppliers

**Implementation Notes:**
- GROUP BY supplier_name (and supplier_tax_number for uniqueness disambiguation)
- Sort by totalAmount descending, return top N
- Aggregate remaining suppliers as "others"
- Percentage relative to total amount across all suppliers

### 5. Pending Actions Endpoint

**GET /api/v1/dashboard/pending-actions**

**Query Parameters:**
- `limit` (optional, int, default 10, max 20)

**Response:**
- `totalPending` (int) — total number of PENDING invoices
- `invoices` array: each with id, invoiceNumber, supplierName, totalAmount, currency, sourceType, confidenceScore, createdAt, daysPending

**Implementation Notes:**
- Filter: status = PENDING only
- Order: LOW confidence invoices first (they need more attention), then by createdAt ASC (oldest first)
- `daysPending` = difference between now and createdAt in days
- Include basic info for quick navigation from dashboard

### 6. Status Timeline Endpoint

**GET /api/v1/dashboard/status-timeline**

**Query Parameters:**
- `days` (optional, int, default 30)

**Response:** An array of day objects (reverse chronological), each containing:
- `date` (string, "YYYY-MM-DD")
- `created` (int) — invoices created on that date (by created_at)
- `verified` (int) — invoices verified on that date (by verified_at)
- `rejected` (int) — invoices rejected on that date (by rejected_at)

**Implementation Notes:**
- Three separate counts per day based on different timestamp columns
- Fill days with zero activity for continuous chart rendering
- Order by date descending

---
### 6.1 LLM Extraction Performance Endpoint

**GET /api/v1/dashboard/extraction-performance**

**Query Parameters:**
- `dateFrom`, `dateTo` (same as stats)

**Response:**
- `totalExtractions` (int) — total LLM extraction attempts in the period
- `successRate` (decimal, percentage) — successful extractions / total attempts
- `averageConfidence` (decimal) — average confidence score of successful extractions
- `averageDuration` (decimal, seconds) — average extraction duration
- `byProvider`: array of objects, each with:
  - `provider` (string — GEMINI, GPT, CLAUDE)
  - `attempts` (int) — how many times this provider was used
  - `successCount` (int)
  - `failureCount` (int)
  - `averageConfidence` (decimal)
  - `fallbackCount` (int) — times this provider was used as a fallback (not primary)
- `failureReasons`: array of `{ reason: string, count: int }` — top failure reasons

**Implementation Notes:**
- Query from invoices table: source_type = LLM, group by llm_provider
- Success = status is not FAILED (or has confidence_score > 0)
- Duration data may not be available in the invoices table — if extraction_duration is tracked (Phase 19/20), use it. If not, skip this field and document it.
- This endpoint is useful for the admin to understand LLM pipeline health from a business perspective (as opposed to the Phase 40 system-level monitoring).

**RBAC:** ADMIN, MANAGER only.

---
### 7. Implementation Architecture

**Package structure:**
- `application/dashboard/DashboardService.java` — orchestrates all queries
- `application/dashboard/dto/` — DashboardStatsResponse, CategoryDistributionResponse, MonthlyTrendResponse, TopSuppliersResponse, PendingActionsResponse, StatusTimelineResponse
- `interfaces/rest/dashboard/DashboardController.java` — REST controller with all 6 endpoints
- `infrastructure/persistence/dashboard/DashboardQueryRepository.java` — custom query repository using native SQL or JPQL

**Query Approach:**
- Use native SQL queries via `@Query(nativeQuery = true)` or JdbcTemplate for aggregation queries
- Standard JPA repository methods are NOT suitable for GROUP BY / SUM / COUNT aggregations — avoid loading all entities into memory
- All queries MUST include `WHERE company_id = :companyId AND is_deleted = FALSE`

**RBAC:**
- All dashboard endpoints require `REPORT_VIEW` permission
- All roles have REPORT_VIEW per Phase 5

### 8. Caching Strategy

**Redis caching (recommended):**
- Cache each endpoint response per company + parameters
- TTL: 60 seconds (dashboard data doesn't need to be real-time)
- Cache key format: `dashboard:{companyId}:{endpoint}:{hash(params)}`
- Let TTL handle invalidation (simpler than event-based invalidation for this phase)

**If Redis is too complex**: Use Spring `@Cacheable` with ConcurrentMapCache and a scheduled eviction every 60 seconds.

### 9. Performance and Database

- All queries must be company-scoped (leverage existing composite indexes)
- Target response time: < 500ms per endpoint
- Log slow queries (> 300ms) at WARN level
- Check if new indexes are needed for:
  - `company_id + supplier_name` (top suppliers aggregation)
  - `company_id + category_id` (category distribution)
  - `company_id + source_type` (source breakdown)
- If needed, create Flyway migration: `V{next}__phase_26_dashboard_indexes.sql`

---

## TESTING REQUIREMENTS

### Unit Tests
- Each service method returns correct aggregations for test data
- Date range filtering narrows results correctly
- Currency filtering limits amount calculations correctly
- Empty dataset returns zeros, not nulls or errors
- Monthly trend fills missing months with zeros
- Top suppliers correctly aggregates, sorts, and handles "others"
- Pending actions sorts by urgency (low confidence + oldest first)
- Turkish month labels are correct

### Integration Tests
- Each endpoint returns 200 with correct structure for seeded data
- Date range filter works across endpoints
- Currency filter limits amounts correctly
- Multi-tenant: only returns data for the user's company
- RBAC: all roles can access
- Empty company (no invoices): all zeros, no errors
- Large dataset performance check

---

## RESULT FILE

**`docs/FURKAN/step_results/faz_26.0_result.md`**

Include: execution summary, completed tasks checklist (all 6 endpoints, isolation, filtering, caching, indexes, tests), files created/modified, API documentation (URL + params + response schema + example for each endpoint), query performance observations, database changes, test results, issues encountered, next steps for Phase 26-B.

---

## DEPENDENCIES

### Requires
- **Phase 3**: Database schema with indexes
- **Phase 5**: RBAC — REPORT_VIEW permission
- **Phase 7**: Invoice entity and repository

### Required By
- **Phase 26-B**: Frontend Dashboard — consumes all endpoints

---

## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] GET /api/v1/dashboard/summary returns correct KPI data (total invoices, total amount, pending count, avg confidence)
- [ ] GET /api/v1/dashboard/category-distribution returns category breakdown with amounts
- [ ] GET /api/v1/dashboard/monthly-trend returns 12-month data with missing months filled as zero
- [ ] GET /api/v1/dashboard/top-suppliers returns top 10 suppliers ranked by total amount
- [ ] GET /api/v1/dashboard/status-breakdown returns count per status
- [ ] GET /api/v1/dashboard/pending-actions returns pending invoices ordered by urgency
- [ ] All endpoints accept date range filters (dateFrom, dateTo)
- [ ] Currency filter works on amount-related endpoints
- [ ] Multi-tenant isolation enforced on all endpoints (tested with two companies)
- [ ] Soft-deleted records excluded from all aggregations
- [ ] Empty dataset returns zeros/empty arrays (not errors or 500)
- [ ] Response time < 500ms per endpoint (with reasonable data volume)
- [ ] Redis caching implemented for dashboard endpoints
- [ ] Cache invalidation triggers on invoice create/update/delete
- [ ] All tests pass
- [ ] Result file created at docs/OMER/step_results/faz_26.0_result.md
---

## SUCCESS CRITERIA

1. ✅ All 6 dashboard endpoints return correct, company-scoped data
2. ✅ Date range and currency filtering work on all applicable endpoints
3. ✅ Multi-tenant isolation enforced
4. ✅ Empty dataset returns zeros (not errors)
5. ✅ Monthly trend fills missing months
6. ✅ Response time < 500ms per endpoint
7. ✅ Caching implemented
8. ✅ All tests pass
9. ✅ Result file created at docs/OMER/step_results/faz_26.0_result.md
10.✅ Extraction performance endpoint returns provider-level success rates and confidence scores

---

## IMPORTANT NOTES

1. **Aggregation Queries, Not ORM**: Use native SQL or JPQL with GROUP BY, SUM, COUNT. Do NOT load all invoices into memory and aggregate in Java.
2. **Currency Isolation for Amounts**: Summing TRY and USD together is meaningless. Amount metrics must filter by currency. Count metrics include all currencies.
3. **Soft Delete Exclusion**: Every query must include `is_deleted = FALSE`.
4. **Month Gap Filling**: If no invoices exist for a month, return a zero-value entry. The frontend needs continuous data for chart rendering.
5. **Turkish Month Names**: Provide Turkish labels in the response so the frontend doesn't need to translate.

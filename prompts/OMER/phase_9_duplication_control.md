# PHASE 9: INVOICE DUPLICATION CONTROL

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Backend**: Java 17 + Spring Boot 3.2 (Hexagonal Architecture)
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Security**: JWT Authentication + RBAC (4 roles)

### Current State
**Phases 0-8 have been completed:**
- ✅ Phase 0: Docker Compose environment (PostgreSQL, Redis, RabbitMQ)
- ✅ Phase 1: CI/CD Pipeline with GitHub Actions
- ✅ Phase 2: Hexagonal Architecture layer structure
- ✅ Phase 3: Database schema — `invoices` table with `invoice_number`, `invoice_date`, `total_amount`, `supplier_name`, `supplier_tax_number`, `company_id`. Unique index: `idx_invoices_company_invoice_number` on (company_id, invoice_number) WHERE is_deleted = FALSE.
- ✅ Phase 4: JWT Authentication
- ✅ Phase 5: RBAC with 4 roles, company-based multi-tenant isolation
- ✅ Phase 6: Company & User Management API
- ✅ Phase 7: Invoice CRUD API — `InvoiceService.createInvoice()`, `InvoiceService.updateInvoice()`, status workflow (PENDING → VERIFIED / REJECTED), Category CRUD, auto-calculated totals from items
- ✅ Phase 8: Audit Log Mechanism — Spring AOP `@Auditable` annotation on all service methods, AuditAspect, AuditLogQueryService, audit log query API

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer - Backend focused)
- **Estimated Duration**: 1-2 days

---

## OBJECTIVE

Implement automatic duplicate invoice detection that runs before every invoice creation (both manual and future LLM-extracted). When a potential duplicate is detected, the system should return a warning with details of the matching invoice(s), allowing the user to either cancel or force the creation. This prevents accidental double-entry of invoices, a common problem in accounting workflows. The detection must be company-scoped to avoid cross-tenant false positives.

---

## DETAILED REQUIREMENTS

### 1. Duplication Detection Service

**File**: `application/invoice/DuplicateDetectionService.java`

This service should be invoked by InvoiceService BEFORE saving a new invoice. It is a standalone service that can be used from multiple entry points (manual creation, LLM extraction in future phases, e-invoice import in future phases).

**Detection Strategy — Multi-Level Matching**:

The service should check for duplicates using three levels of matching, each with a different confidence:

**Level 1 — Exact Match (HIGH confidence)**:
- Same `invoice_number` + same `company_id`
- This is already enforced by the unique index, but duplicate detection should catch this before hitting the DB constraint and provide a user-friendly message

**Level 2 — Strong Match (MEDIUM confidence)**:
- Same `supplier_tax_number` + same `invoice_date` + same `total_amount` + same `company_id`
- Different invoice_number (if same number, it's Level 1)
- This catches cases where the same invoice is entered with a typo in the number

**Level 3 — Fuzzy Match (LOW confidence)**:
- Same `supplier_name` (case-insensitive, trimmed) + same `invoice_date` + similar `total_amount` (within ±1% tolerance) + same `company_id`
- No tax number match or tax number not provided
- This catches cases where the supplier name is slightly different or amounts have rounding differences

**Method signature**:
```java
public DuplicateCheckResult checkForDuplicates(DuplicateCheckRequest request);
```

**DuplicateCheckRequest** (application DTO):
- `invoiceNumber` (String)
- `invoiceDate` (LocalDate)
- `totalAmount` (BigDecimal)
- `supplierName` (String)
- `supplierTaxNumber` (String, nullable)
- `companyId` (UUID)
- `excludeInvoiceId` (UUID, nullable) — Exclude this invoice from results (used during updates to not flag itself)

**DuplicateCheckResult** (application DTO):
- `hasDuplicates` (boolean)
- `duplicates` (List<DuplicateMatch>)
- `highestConfidence` (DuplicateConfidence enum: HIGH, MEDIUM, LOW, NONE)

**DuplicateMatch** (application DTO):
- `invoiceId` (UUID) — ID of the matching existing invoice
- `invoiceNumber` (String)
- `invoiceDate` (LocalDate)
- `supplierName` (String)
- `totalAmount` (BigDecimal)
- `status` (InvoiceStatus)
- `confidence` (DuplicateConfidence: HIGH, MEDIUM, LOW)
- `matchReason` (String) — Human-readable reason, e.g., "Exact invoice number match", "Same supplier tax number, date, and amount"

---

### 2. Integration with Invoice Creation

**Modify**: `application/invoice/InvoiceService.java`
**Modify**: `interfaces/rest/invoice/InvoiceController.java`

The duplicate check should be integrated into the invoice creation flow with a **two-step approach**:

**Step 1 — Check before create**:
When `POST /api/v1/invoices` is called WITHOUT `forceDuplicate = true`:
1. Run `DuplicateDetectionService.checkForDuplicates()`
2. If duplicates found → return HTTP **409 Conflict** with duplicate details
3. If no duplicates → proceed with normal creation → return **201 Created**

**Step 2 — Force create**:
When `POST /api/v1/invoices` is called WITH `forceDuplicate = true` (query parameter):
1. Skip duplicate check (or run it but don't block)
2. Proceed with creation → return **201 Created**
3. Optionally: still log the duplicate warning in audit log for traceability

---

### 3. Standalone Duplicate Check Endpoint

**Purpose**: Allow the frontend to check for duplicates BEFORE submitting the full invoice form. This enables real-time duplicate warnings in the UI.

**Endpoint**: `POST /api/v1/invoices/check-duplicate`

**Request body**:
```json
{
  "invoiceNumber": "FTR-2026-001",
  "invoiceDate": "2026-02-10",
  "totalAmount": 960.00,
  "supplierName": "ABC Teknoloji Ltd.",
  "supplierTaxNumber": "1234567890"
}
```

**Response** (200 OK — always 200, the result indicates duplicates):
```json
{
  "success": true,
  "data": {
    "hasDuplicates": true,
    "highestConfidence": "MEDIUM",
    "duplicates": [
      {
        "invoiceId": "existing-invoice-uuid",
        "invoiceNumber": "FTR-2026-001",
        "invoiceDate": "2026-02-10",
        "supplierName": "ABC Teknoloji Ltd.",
        "totalAmount": 960.00,
        "status": "VERIFIED",
        "confidence": "HIGH",
        "matchReason": "Exact invoice number match within your company"
      }
    ]
  }
}
```

---

### 4. Duplicate Check During Update

When updating an invoice (`PUT /api/v1/invoices/{id}`), if the `invoiceNumber` is changed, run duplicate detection excluding the current invoice ID (`excludeInvoiceId`). This prevents flagging the invoice as a duplicate of itself.

---

### 5. Repository Queries for Duplicate Detection

**Add to**: `domain/invoice/port/InvoiceRepository.java` or create `domain/invoice/port/DuplicateCheckPort.java`

New query methods needed:
```java
// Level 1: Exact invoice number match
Optional<Invoice> findByInvoiceNumberAndCompanyIdAndIsDeletedFalse(
    String invoiceNumber, UUID companyId);

// Level 2: Strong match — supplier tax + date + amount
List<Invoice> findBySupplierTaxNumberAndInvoiceDateAndTotalAmountAndCompanyIdAndIsDeletedFalse(
    String supplierTaxNumber, LocalDate invoiceDate, BigDecimal totalAmount, UUID companyId);

// Level 3: Fuzzy match — supplier name + date + amount range
// Custom @Query:
// SELECT * FROM invoices 
// WHERE company_id = :companyId AND is_deleted = false
//   AND invoice_date = :invoiceDate
//   AND total_amount BETWEEN :minAmount AND :maxAmount
//   AND LOWER(TRIM(supplier_name)) = LOWER(TRIM(:supplierName))
//   AND id != :excludeId
List<Invoice> findPotentialDuplicatesBySupplierAndDateAndAmountRange(
    String supplierName, LocalDate invoiceDate, 
    BigDecimal minAmount, BigDecimal maxAmount, UUID companyId, UUID excludeId);
```

---

### 6. Amount Tolerance for Fuzzy Match

- Level 3 uses ±1% tolerance on `total_amount`
- Minimum tolerance: ±1 currency unit (for very small invoices)

```java
BigDecimal tolerance = totalAmount.multiply(new BigDecimal("0.01"));
if (tolerance.compareTo(BigDecimal.ONE) < 0) {
    tolerance = BigDecimal.ONE;
}
BigDecimal minAmount = totalAmount.subtract(tolerance);
BigDecimal maxAmount = totalAmount.add(tolerance);
```

---

## API ENDPOINTS

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| POST | `/api/v1/invoices/check-duplicate` | Check for duplicates before creating | Yes | ALL |
| POST | `/api/v1/invoices?forceDuplicate=true` | Create invoice skipping duplicate check | Yes | ALL |

Note: The existing `POST /api/v1/invoices` from Phase 7 is **modified** to include duplicate checking. No new CRUD endpoints.

---

## TECHNICAL SPECIFICATIONS

### DuplicateConfidence Enum

**File**: `domain/invoice/valueobject/DuplicateConfidence.java`

```
public enum DuplicateConfidence {
    HIGH,    // Exact invoice number match
    MEDIUM,  // Same supplier tax number + date + amount
    LOW,     // Fuzzy supplier name + date + similar amount
    NONE     // No duplicates found
}
```

### 409 Conflict Response Format

```json
{
  "success": false,
  "error": {
    "code": "INVOICE_DUPLICATE_DETECTED",
    "message": "Potential duplicate invoice(s) detected. Use forceDuplicate=true to create anyway.",
    "details": {
      "hasDuplicates": true,
      "highestConfidence": "HIGH",
      "duplicates": [
        {
          "invoiceId": "uuid",
          "invoiceNumber": "FTR-2026-001",
          "invoiceDate": "2026-02-10",
          "supplierName": "ABC Teknoloji",
          "totalAmount": 960.00,
          "status": "VERIFIED",
          "confidence": "HIGH",
          "matchReason": "Exact invoice number match within your company"
        }
      ]
    },
    "timestamp": "2026-02-10T14:30:00Z"
  }
}
```

### Error Codes

- `INVOICE_DUPLICATE_DETECTED` — Potential duplicate found (409 Conflict)

---

## DATABASE CHANGES

### No New Tables Required

### Potential Migration

**File**: `backend/src/main/resources/db/migration/V7__faz_9_duplicate_detection_indexes.sql`

```sql
-- Composite index for Level 2 duplicate detection
CREATE INDEX IF NOT EXISTS idx_invoices_dup_check_level2 
ON invoices(company_id, supplier_tax_number, invoice_date, total_amount) 
WHERE is_deleted = FALSE AND supplier_tax_number IS NOT NULL;

-- Index for Level 3 fuzzy match
CREATE INDEX IF NOT EXISTS idx_invoices_dup_check_level3 
ON invoices(company_id, invoice_date, total_amount) 
WHERE is_deleted = FALSE;
```

Only create if existing Phase 3 indexes are insufficient. Check first, document the decision.

---

## TESTING REQUIREMENTS

### Unit Tests

**File**: `DuplicateDetectionServiceTest.java`

1. **Level 1 — Exact match**: Same invoice_number + company → HIGH
2. **Level 1 — Cross-company**: Same number, different company → no match
3. **Level 1 — Soft-deleted**: Same number but deleted → no match
4. **Level 2 — Strong match**: Same tax_number + date + amount → MEDIUM
5. **Level 2 — Partial mismatch**: Same tax_number + different date → no match
6. **Level 3 — Fuzzy match**: Same name (case-insensitive) + date + amount ±1% → LOW
7. **Level 3 — Outside tolerance**: Amount outside ±1% → no match
8. **No duplicates**: Unique invoice → NONE
9. **Multiple matches**: Returns all with correct confidence levels
10. **Self-exclusion**: excludeInvoiceId works on update

### Integration Tests

1. Create invoice → 201, then create same number → 409 with duplicate details
2. Create with forceDuplicate=true → 201 despite duplicate
3. POST /invoices/check-duplicate with match → 200, hasDuplicates=true
4. POST /invoices/check-duplicate unique → 200, hasDuplicates=false
5. Cross-company: no false positives

### Manual Testing Steps

1. Create invoice FTR-001 → 201
2. Try creating FTR-001 again → 409 (HIGH confidence)
3. Create FTR-001 with `?forceDuplicate=true` → 201
4. Check duplicate endpoint with same supplier/date/amount → MEDIUM
5. Check with fuzzy name match → LOW

---

## VERIFICATION CHECKLIST

- [ ] DuplicateDetectionService created with three-level matching
- [ ] DuplicateConfidence enum created
- [ ] DTOs: DuplicateCheckRequest, DuplicateCheckResult, DuplicateMatch
- [ ] Level 1 (exact number): works correctly, company-scoped
- [ ] Level 2 (tax + date + amount): works correctly
- [ ] Level 3 (fuzzy name + date + ~amount): works with ±1% tolerance
- [ ] POST /invoices returns 409 when duplicates detected
- [ ] forceDuplicate=true bypasses check
- [ ] POST /invoices/check-duplicate standalone endpoint works
- [ ] Company isolation: no cross-tenant false positives
- [ ] Soft-deleted invoices excluded
- [ ] Update flow: self-exclusion works
- [ ] Audit log metadata notes forced duplicates
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] CI pipeline passes

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_9_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed, actual time vs estimated (1-2 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created/Modified
```
domain/invoice/
└── valueobject/DuplicateConfidence.java

application/invoice/
├── DuplicateDetectionService.java
├── InvoiceService.java (MODIFIED)
├── dto/DuplicateCheckRequest.java
├── dto/DuplicateCheckResult.java
└── dto/DuplicateMatch.java

interfaces/rest/invoice/
├── InvoiceController.java (MODIFIED)
└── dto/
    ├── DuplicateCheckRequestDTO.java
    └── DuplicateCheckResponse.java

infrastructure/persistence/invoice/
├── InvoiceJpaRepository.java (MODIFIED - custom queries)
└── InvoiceRepositoryAdapter.java (MODIFIED)
```

### 4. Detection Level Verification
| Level | Criteria | Test Result |
|-------|----------|-------------|
| HIGH | Same invoice_number + company | ✅/❌ |
| MEDIUM | Same tax_number + date + amount | ✅/❌ |
| LOW | Same name + date + ~amount (±1%) | ✅/❌ |
| Cross-company isolation | ✅/❌ |
| Soft-deleted excluded | ✅/❌ |
| Self-exclusion on update | ✅/❌ |

### 5. Test Results
### 6. Database Changes
### 7. Issues Encountered
### 8. Next Steps
Note how duplicate check will be used by frontend (Phase 12) and LLM pipeline (Phase 15-17).
### 9. Time Spent

---

## DEPENDENCIES

### Requires
- **Phase 7**: Invoice CRUD API (InvoiceService, InvoiceRepository) ✅
- **Phase 8**: Audit Log (for logging forced duplicates) ✅

### Required By
- **Phase 12**: Frontend Invoice CRUD UI (check-duplicate for real-time warnings)
- **Phase 15-17**: LLM Extraction Pipeline (duplicate check on extracted invoices)
- **Phase 20**: File Upload (duplicate check after extraction)

---

## SUCCESS CRITERIA

1. ✅ Three-level duplicate detection works (HIGH, MEDIUM, LOW)
2. ✅ Invoice creation returns 409 when duplicates found
3. ✅ forceDuplicate flag allows creation despite duplicates
4. ✅ Standalone check-duplicate endpoint works
5. ✅ Company-scoped: no cross-tenant false positives
6. ✅ Soft-deleted invoices excluded
7. ✅ Service is reusable for future LLM pipeline
8. ✅ All tests pass
9. ✅ Result file created at `docs/OMER/step_results/faz_9_result.md`

---

## IMPORTANT NOTES

1. **Performance**: Queries must use indexes efficiently. Level 3 fuzzy match should not do full table scans.
2. **Transaction**: Duplicate check runs in the SAME transaction as creation. The unique index is a fallback safety net.
3. **Reusability**: Service must be decoupled — callable from manual creation, LLM pipeline, e-invoice import, bulk upload.
4. **LOW confidence policy**: Consider only blocking on HIGH and MEDIUM. For LOW, include matches in response but allow creation without force flag. Document your design decision.
5. **Audit metadata**: When forceDuplicate=true, add to audit log metadata: `{"forcedDuplicate": true, "duplicateCount": 1, "highestConfidence": "HIGH"}`
6. **Soft delete**: All queries must filter `WHERE is_deleted = FALSE`.

---

**Phase 9 Completion Target**: Robust three-level duplicate detection integrated into invoice creation with force-override and standalone check endpoint.

# PHASE 7: INVOICE CRUD API

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
**Phases 0-6 have been completed:**
- ✅ Phase 0: Docker Compose environment (PostgreSQL, Redis, RabbitMQ)
- ✅ Phase 1: CI/CD Pipeline with GitHub Actions
- ✅ Phase 2: Hexagonal Architecture layer structure with ArchUnit tests
- ✅ Phase 3: Database schema with Flyway migrations — tables: companies, users, invoices, invoice_items, categories, audit_logs, refresh_tokens. Indexes, soft delete, UUID PKs, constraints all in place.
- ✅ Phase 4: JWT Authentication (register, login, refresh, logout, brute force protection, BCrypt strength 12)
- ✅ Phase 5: RBAC with 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN), Permission enum, @PreAuthorize annotations, CompanyContextFilter, CompanyContextHolder, SecurityExpressionService, custom security annotations (@IsAdmin, @IsManagerOrHigher, @CanEditInvoice, @CanDeleteInvoice, etc.)
- ✅ Phase 6: Company & User Management API (Company CRUD, User CRUD, Profile management, role assignment, toggle active, company-scoped user listing, pagination)

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer - Backend focused)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Implement a complete Invoice CRUD API for manual invoice creation, listing, updating, and soft deletion. Also implement Category management endpoints. Invoices must be company-scoped (multi-tenant isolation), support pagination and sorting, include status workflow management (PENDING → VERIFIED / REJECTED), and handle invoice line items (invoice_items) as a nested collection. This phase creates the core business data layer that all subsequent features (LLM extraction, filtering, export, dashboard) will build upon.

---

## ROLE PERMISSION MATRIX FOR THIS PHASE

```
┌──────────────────────────────┬────────┬─────────┬─────────────┬──────────┐
│ Operation                    │ ADMIN  │ MANAGER │ ACCOUNTANT  │  INTERN  │
├──────────────────────────────┼────────┼─────────┼─────────────┼──────────┤
│ View Invoices (list + detail)│   ✅   │   ✅    │     ✅      │    ✅    │
│ Create Invoice (manual)      │   ✅   │   ✅    │     ✅      │    ✅    │
│ Edit Invoice                 │   ✅   │   ✅    │     ✅      │    ❌    │
│ Delete Invoice (soft)        │   ✅   │   ✅    │     ❌      │    ❌    │
│ Verify Invoice               │   ✅   │   ✅    │     ✅      │    ❌    │
│ Reject Invoice               │   ✅   │   ✅    │     ✅      │    ❌    │
├──────────────────────────────┼────────┼─────────┼─────────────┼──────────┤
│ View Categories              │   ✅   │   ✅    │     ✅      │    ✅    │
│ Create Category              │   ✅   │   ✅    │     ❌      │    ❌    │
│ Update Category              │   ✅   │   ✅    │     ❌      │    ❌    │
│ Delete Category              │   ✅   │   ✅    │     ❌      │    ❌    │
└──────────────────────────────┴────────┴─────────┴─────────────┴──────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Invoice Domain Model

**Purpose**: Create the Invoice domain entity and all Hexagonal Architecture layers.

**1.1 Domain Layer**

**File**: `domain/invoice/entity/Invoice.java`

The Invoice domain entity must map to the existing `invoices` table (Phase 3). Fields:

- `id` (UUID) — PK
- `companyId` (UUID, required) — FK → companies
- `categoryId` (UUID, optional) — FK → categories
- `createdByUserId` (UUID) — FK → users (who created this invoice)
- `verifiedByUserId` (UUID, optional) — FK → users (who verified/rejected)
- **Invoice Identification**:
  - `invoiceNumber` (String, required, max 50)
  - `invoiceDate` (LocalDate, required)
  - `dueDate` (LocalDate, optional)
- **Supplier Information**:
  - `supplierName` (String, required, max 255)
  - `supplierTaxNumber` (String, optional, max 20) — VKN (10 digits) or TCKN (11 digits)
  - `supplierTaxOffice` (String, optional, max 255)
  - `supplierAddress` (String, optional, text)
  - `supplierPhone` (String, optional, max 20)
  - `supplierEmail` (String, optional, max 255)
- **Financial Details**:
  - `subtotal` (BigDecimal, required, precision 15 scale 2)
  - `taxAmount` (BigDecimal, required, precision 15 scale 2)
  - `totalAmount` (BigDecimal, required, precision 15 scale 2)
  - `currency` (String, default "TRY") — Allowed: TRY, USD, EUR, GBP
  - `exchangeRate` (BigDecimal, optional, precision 10 scale 4, default 1.0)
- **Status & Workflow**:
  - `status` (InvoiceStatus enum: PENDING, VERIFIED, REJECTED, PROCESSING)
- **Source Information**:
  - `sourceType` (SourceType enum: LLM, E_INVOICE, MANUAL)
  - `llmProvider` (LlmProvider enum: GEMINI, GPT, CLAUDE — nullable)
  - `confidenceScore` (BigDecimal, nullable, 0-100)
  - `processingDurationMs` (Integer, nullable)
- **File Information** (populated later by file upload phases):
  - `originalFilePath` (String, nullable)
  - `originalFileName` (String, nullable)
  - `originalFileSize` (Integer, nullable)
  - `originalFileType` (String, nullable)
- **E-Invoice Specific** (populated by e-invoice parser phases):
  - `eInvoiceUuid` (String, nullable)
  - `eInvoiceEttn` (String, nullable)
- **Notes**:
  - `notes` (String, nullable)
  - `rejectionReason` (String, nullable)
- **Timestamps**:
  - `verifiedAt` (LocalDateTime, nullable)
  - `rejectedAt` (LocalDateTime, nullable)
- **Soft Delete & Audit**:
  - `isDeleted`, `deletedAt`, `createdAt`, `updatedAt`
- **Nested Collection**:
  - `items` (List<InvoiceItem>) — The line items of this invoice

**File**: `domain/invoice/entity/InvoiceItem.java`

Maps to the existing `invoice_items` table:
- `id` (UUID) — PK
- `invoiceId` (UUID) — FK → invoices
- `lineNumber` (Integer, required)
- `description` (String, required, max 500)
- `quantity` (BigDecimal, required, precision 15 scale 4, default 1)
- `unit` (String, default "ADET") — Allowed: ADET, KG, LT, M, M2, M3, PAKET, KUTU, etc.
- `unitPrice` (BigDecimal, required, precision 15 scale 4)
- `taxRate` (BigDecimal, default 18.00) — KDV rate %
- `taxAmount` (BigDecimal, required, precision 15 scale 2)
- `subtotal` (BigDecimal, required, precision 15 scale 2)
- `totalAmount` (BigDecimal, required, precision 15 scale 2)
- `productCode` (String, optional, max 50)
- `barcode` (String, optional, max 50)
- `createdAt`, `updatedAt`

**File**: `domain/invoice/valueobject/InvoiceStatus.java`

```
public enum InvoiceStatus {
    PENDING,
    PROCESSING,
    VERIFIED,
    REJECTED
}
```

**File**: `domain/invoice/valueobject/SourceType.java`

```
public enum SourceType {
    LLM,
    E_INVOICE,
    MANUAL
}
```

**File**: `domain/invoice/valueobject/LlmProvider.java`

```
public enum LlmProvider {
    GEMINI,
    GPT,
    CLAUDE
}
```

**File**: `domain/invoice/valueobject/Currency.java`

```
public enum Currency {
    TRY, USD, EUR, GBP
}
```

**File**: `domain/invoice/port/InvoiceRepository.java`

Repository port (interface) in domain layer:
- `save(Invoice)` → Invoice
- `findById(UUID)` → Optional<Invoice>
- `findByIdAndCompanyId(UUID, UUID)` → Optional<Invoice>
- `findAllByCompanyId(UUID, Pageable)` → Page<Invoice>
- `findAllByCompanyIdAndStatus(UUID, InvoiceStatus, Pageable)` → Page<Invoice>
- `existsByInvoiceNumberAndCompanyId(String, UUID)` → boolean
- `softDelete(UUID)` → void
- `countByCompanyId(UUID)` → long
- `countByCompanyIdAndStatus(UUID, InvoiceStatus)` → long

**1.2 Application Layer**

**File**: `application/invoice/InvoiceService.java`

Use cases:
- `createInvoice(CreateInvoiceCommand)` → InvoiceResponse
  - Sets `sourceType = MANUAL` for this phase
  - Sets `status = PENDING`
  - Sets `createdByUserId` from authenticated user
  - Sets `companyId` from CompanyContextHolder
  - Auto-calculates `subtotal`, `taxAmount`, `totalAmount` from items if items provided
  - Saves invoice and items in a single transaction
- `getInvoiceById(UUID)` → InvoiceDetailResponse
  - Must include invoice items
  - Company isolation: verify invoice belongs to current user's company
- `listInvoices(Pageable)` → Page<InvoiceListResponse>
  - Automatically scoped to current user's company
  - Returns summary (no items) for list performance
  - Supports sorting by: invoiceDate, totalAmount, supplierName, status, createdAt
- `updateInvoice(UUID, UpdateInvoiceCommand)` → InvoiceResponse
  - Only PENDING invoices can be edited (VERIFIED/REJECTED cannot)
  - Items can be added, updated, removed in the same request
  - Recalculates totals from items
- `deleteInvoice(UUID)` → void
  - Soft delete (is_deleted = true, deleted_at = now)
  - Only ADMIN and MANAGER can delete
- `verifyInvoice(UUID, VerifyInvoiceCommand)` → InvoiceResponse
  - Changes status from PENDING → VERIFIED
  - Sets `verifiedByUserId` and `verifiedAt`
  - Optionally accepts notes
- `rejectInvoice(UUID, RejectInvoiceCommand)` → InvoiceResponse
  - Changes status from PENDING → REJECTED
  - Requires `rejectionReason`
  - Sets `verifiedByUserId` (the person who rejected) and `rejectedAt`

Business rules:
- Invoice number must be unique within a company (unique index: company_id + invoice_number WHERE is_deleted = false)
- Status transitions: PENDING → VERIFIED, PENDING → REJECTED, REJECTED → PENDING (allow re-evaluation)
- Cannot edit a VERIFIED invoice (must be rejected first to re-edit)
- Cannot delete a VERIFIED invoice
- totalAmount = subtotal + taxAmount (auto-calculated from items)
- For each item: subtotal = quantity × unitPrice, taxAmount = subtotal × (taxRate / 100), totalAmount = subtotal + taxAmount
- Invoice-level totals = sum of all item totals

**1.3 Infrastructure Layer**

**File**: `infrastructure/persistence/invoice/InvoiceJpaEntity.java`

JPA entity mapping to the existing `invoices` table. Use `@OneToMany(cascade = ALL, orphanRemoval = true)` for invoice items.

**File**: `infrastructure/persistence/invoice/InvoiceItemJpaEntity.java`

JPA entity mapping to the existing `invoice_items` table.

**File**: `infrastructure/persistence/invoice/InvoiceJpaRepository.java`

Spring Data JPA repository.

**File**: `infrastructure/persistence/invoice/InvoiceRepositoryAdapter.java`

Adapter implementing domain port → JPA repository delegation.

**File**: `infrastructure/persistence/invoice/InvoiceMapper.java`

Mapper between domain ↔ JPA entities. Must handle bidirectional mapping for items.

**1.4 Interfaces Layer**

**File**: `interfaces/rest/invoice/InvoiceController.java`

---

### 2. Category Management

**Purpose**: Allow ADMIN and MANAGER to manage invoice categories within their company.

**2.1 Domain Layer**

**File**: `domain/category/entity/Category.java`

Maps to existing `categories` table:
- `id` (UUID) — PK
- `companyId` (UUID, required) — FK → companies
- `name` (String, required, max 100) — Unique within company
- `description` (String, optional)
- `color` (String, optional, max 7) — Hex color code (#RRGGBB)
- `icon` (String, optional, max 50)
- `parentId` (UUID, optional) — FK → categories (hierarchy)
- `isActive` (boolean, default true)
- `isDeleted`, `deletedAt`, `createdAt`, `updatedAt`

**File**: `domain/category/port/CategoryRepository.java`

- `save(Category)` → Category
- `findById(UUID)` → Optional<Category>
- `findByIdAndCompanyId(UUID, UUID)` → Optional<Category>
- `findAllByCompanyId(UUID)` → List<Category> (categories are typically small sets, no pagination needed)
- `findAllActiveByCompanyId(UUID)` → List<Category>
- `existsByNameAndCompanyId(String, UUID)` → boolean
- `softDelete(UUID)` → void

**2.2 Application Layer**

**File**: `application/category/CategoryService.java`

- `createCategory(CreateCategoryCommand)` → CategoryResponse
- `updateCategory(UUID, UpdateCategoryCommand)` → CategoryResponse
- `getCategoryById(UUID)` → CategoryResponse
- `listCategories()` → List<CategoryResponse> (all active categories for current company)
- `listAllCategories()` → List<CategoryResponse> (include inactive, for admin views)
- `deleteCategory(UUID)` → void (soft delete)

Business rules:
- Category name must be unique within a company
- Cannot delete a category that is assigned to invoices (or set invoices' category to null)
- Company-scoped: categories belong to a company

**2.3 Infrastructure & Interfaces Layers**

Follow same Hexagonal Architecture pattern as Invoice.

**File**: `interfaces/rest/category/CategoryController.java`

---

### 3. Invoice Status Workflow

**Purpose**: Implement the status state machine for invoices.

```
                  ┌──────────┐
    create() ───→ │ PENDING  │ ←── reopen()
                  └────┬─────┘
                       │
              ┌────────┴────────┐
              │                 │
         verify()          reject()
              │                 │
              ▼                 ▼
        ┌──────────┐    ┌──────────┐
        │ VERIFIED │    │ REJECTED │
        └──────────┘    └─────┬────┘
                              │
                         reopen()
                              │
                              ▼
                        ┌──────────┐
                        │ PENDING  │
                        └──────────┘
```

Valid transitions:
- PENDING → VERIFIED (verify action)
- PENDING → REJECTED (reject action)
- REJECTED → PENDING (reopen action — allows re-evaluation)

Invalid transitions (should throw error):
- VERIFIED → anything (must explicitly reopen/reject first? Or keep immutable — design choice. Recommended: VERIFIED is final for this phase.)
- PROCESSING → any manual transition (PROCESSING is set by LLM pipeline in later phases)

Implement a `InvoiceStatusStateMachine` or validation method that enforces these rules.

---

### 4. HATEOAS Links (Optional but Recommended)

If using Spring HATEOAS, add relevant links to responses:
- `self` → GET /api/v1/invoices/{id}
- `update` → PUT /api/v1/invoices/{id} (if user has edit permission and status allows)
- `delete` → DELETE /api/v1/invoices/{id} (if user has delete permission)
- `verify` → PATCH /api/v1/invoices/{id}/verify (if status is PENDING)
- `reject` → PATCH /api/v1/invoices/{id}/reject (if status is PENDING)
- `items` → GET /api/v1/invoices/{id}/items

If HATEOAS feels too complex for this phase, at minimum ensure consistent response structure. HATEOAS can be added as an enhancement.

---

## API ENDPOINTS

### Invoice Endpoints

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| POST | `/api/v1/invoices` | Create invoice (manual) | Yes | ALL |
| GET | `/api/v1/invoices` | List invoices (paginated) | Yes | ALL |
| GET | `/api/v1/invoices/{id}` | Get invoice detail (with items) | Yes | ALL |
| PUT | `/api/v1/invoices/{id}` | Update invoice | Yes | ADMIN, MANAGER, ACCOUNTANT |
| DELETE | `/api/v1/invoices/{id}` | Soft delete invoice | Yes | ADMIN, MANAGER |
| PATCH | `/api/v1/invoices/{id}/verify` | Verify invoice | Yes | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | `/api/v1/invoices/{id}/reject` | Reject invoice | Yes | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | `/api/v1/invoices/{id}/reopen` | Reopen rejected invoice | Yes | ADMIN, MANAGER |

### Category Endpoints

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| POST | `/api/v1/categories` | Create category | Yes | ADMIN, MANAGER |
| GET | `/api/v1/categories` | List categories (active) | Yes | ALL |
| GET | `/api/v1/categories/all` | List all categories (incl. inactive) | Yes | ADMIN, MANAGER |
| GET | `/api/v1/categories/{id}` | Get category detail | Yes | ALL |
| PUT | `/api/v1/categories/{id}` | Update category | Yes | ADMIN, MANAGER |
| DELETE | `/api/v1/categories/{id}` | Soft delete category | Yes | ADMIN, MANAGER |

---

## TECHNICAL SPECIFICATIONS

### Request/Response DTOs

#### CreateInvoiceRequest
```json
{
  "invoiceNumber": "FTR-2026-001",
  "invoiceDate": "2026-02-10",
  "dueDate": "2026-03-10",
  "supplierName": "ABC Teknoloji Ltd.",
  "supplierTaxNumber": "1234567890",
  "supplierTaxOffice": "Kadıköy Vergi Dairesi",
  "supplierAddress": "Moda Cad. No:5 Kadıköy/İstanbul",
  "supplierPhone": "+902161234567",
  "supplierEmail": "info@abctek.com",
  "currency": "TRY",
  "exchangeRate": 1.0,
  "categoryId": "category-uuid-or-null",
  "notes": "Monthly hosting invoice",
  "items": [
    {
      "description": "Web Hosting - Premium Plan",
      "quantity": 1,
      "unit": "ADET",
      "unitPrice": 500.00,
      "taxRate": 20.00,
      "productCode": "WH-PREM-01"
    },
    {
      "description": "SSL Certificate",
      "quantity": 2,
      "unit": "ADET",
      "unitPrice": 150.00,
      "taxRate": 20.00,
      "productCode": "SSL-01"
    }
  ]
}
```

**Calculation for the above example**:
- Item 1: subtotal = 1 × 500 = 500, taxAmount = 500 × 0.20 = 100, total = 600
- Item 2: subtotal = 2 × 150 = 300, taxAmount = 300 × 0.20 = 60, total = 360
- Invoice: subtotal = 800, taxAmount = 160, totalAmount = 960

#### InvoiceDetailResponse (with items)
```json
{
  "id": "invoice-uuid",
  "invoiceNumber": "FTR-2026-001",
  "invoiceDate": "2026-02-10",
  "dueDate": "2026-03-10",
  "supplierName": "ABC Teknoloji Ltd.",
  "supplierTaxNumber": "1234567890",
  "supplierTaxOffice": "Kadıköy Vergi Dairesi",
  "supplierAddress": "Moda Cad. No:5 Kadıköy/İstanbul",
  "supplierPhone": "+902161234567",
  "supplierEmail": "info@abctek.com",
  "subtotal": 800.00,
  "taxAmount": 160.00,
  "totalAmount": 960.00,
  "currency": "TRY",
  "exchangeRate": 1.0,
  "status": "PENDING",
  "sourceType": "MANUAL",
  "llmProvider": null,
  "confidenceScore": null,
  "categoryId": "category-uuid",
  "categoryName": "Teknoloji",
  "notes": "Monthly hosting invoice",
  "rejectionReason": null,
  "createdByUserId": "user-uuid",
  "createdByUserName": "Ömer Talha Yurttutan",
  "verifiedByUserId": null,
  "verifiedByUserName": null,
  "verifiedAt": null,
  "rejectedAt": null,
  "createdAt": "2026-02-10T14:30:00Z",
  "updatedAt": "2026-02-10T14:30:00Z",
  "items": [
    {
      "id": "item-uuid-1",
      "lineNumber": 1,
      "description": "Web Hosting - Premium Plan",
      "quantity": 1.0000,
      "unit": "ADET",
      "unitPrice": 500.0000,
      "taxRate": 20.00,
      "taxAmount": 100.00,
      "subtotal": 500.00,
      "totalAmount": 600.00,
      "productCode": "WH-PREM-01",
      "barcode": null
    },
    {
      "id": "item-uuid-2",
      "lineNumber": 2,
      "description": "SSL Certificate",
      "quantity": 2.0000,
      "unit": "ADET",
      "unitPrice": 150.0000,
      "taxRate": 20.00,
      "taxAmount": 60.00,
      "subtotal": 300.00,
      "totalAmount": 360.00,
      "productCode": "SSL-01",
      "barcode": null
    }
  ]
}
```

#### InvoiceListResponse (summary, no items)
```json
{
  "id": "invoice-uuid",
  "invoiceNumber": "FTR-2026-001",
  "invoiceDate": "2026-02-10",
  "dueDate": "2026-03-10",
  "supplierName": "ABC Teknoloji Ltd.",
  "totalAmount": 960.00,
  "currency": "TRY",
  "status": "PENDING",
  "sourceType": "MANUAL",
  "categoryName": "Teknoloji",
  "itemCount": 2,
  "createdByUserName": "Ömer Talha Yurttutan",
  "createdAt": "2026-02-10T14:30:00Z"
}
```

#### UpdateInvoiceRequest
```json
{
  "invoiceNumber": "FTR-2026-001-R",
  "invoiceDate": "2026-02-10",
  "dueDate": "2026-03-15",
  "supplierName": "ABC Teknoloji Ltd.",
  "supplierTaxNumber": "1234567890",
  "currency": "TRY",
  "categoryId": "category-uuid",
  "notes": "Updated notes",
  "items": [
    {
      "id": "existing-item-uuid",
      "description": "Web Hosting - Premium Plan (Updated)",
      "quantity": 1,
      "unit": "ADET",
      "unitPrice": 550.00,
      "taxRate": 20.00
    },
    {
      "description": "New Item - Domain Registration",
      "quantity": 1,
      "unit": "ADET",
      "unitPrice": 200.00,
      "taxRate": 20.00
    }
  ]
}
```

Note on update items logic:
- Items WITH `id` → update existing item
- Items WITHOUT `id` → create new item
- Existing items NOT in the request → delete (orphan removal)

#### VerifyInvoiceRequest
```json
{
  "notes": "Verified after cross-checking with supplier"
}
```

#### RejectInvoiceRequest
```json
{
  "rejectionReason": "Invoice amount does not match purchase order"
}
```

#### CreateCategoryRequest
```json
{
  "name": "Teknoloji",
  "description": "Technology and IT related invoices",
  "color": "#3B82F6",
  "icon": "monitor",
  "parentId": null
}
```

#### CategoryResponse
```json
{
  "id": "category-uuid",
  "name": "Teknoloji",
  "description": "Technology and IT related invoices",
  "color": "#3B82F6",
  "icon": "monitor",
  "parentId": null,
  "parentName": null,
  "isActive": true,
  "invoiceCount": 12,
  "createdAt": "2026-02-10T14:30:00Z",
  "updatedAt": "2026-02-10T14:30:00Z"
}
```

### Validation Rules

**Invoice**:
- `invoiceNumber`: @NotBlank, @Size(max = 50)
- `invoiceDate`: @NotNull, must not be in the future (optional business rule)
- `supplierName`: @NotBlank, @Size(max = 255)
- `supplierTaxNumber`: @Pattern(regexp = "^[0-9]{10,11}$") — 10 digits (VKN) or 11 digits (TCKN)
- `currency`: @Pattern or enum validation — must be TRY, USD, EUR, or GBP
- `items`: @NotEmpty — at least one item required for manual creation

**InvoiceItem**:
- `description`: @NotBlank, @Size(max = 500)
- `quantity`: @NotNull, @DecimalMin("0.0001")
- `unitPrice`: @NotNull, @DecimalMin("0")
- `taxRate`: @NotNull, @DecimalMin("0"), @DecimalMax("100")

**Category**:
- `name`: @NotBlank, @Size(max = 100)
- `color`: @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") — hex color validation

### Error Codes

- `INVOICE_NOT_FOUND` — Invoice with given ID not found
- `INVOICE_NUMBER_EXISTS` — Invoice number already exists in this company
- `INVOICE_NOT_EDITABLE` — Cannot edit VERIFIED invoice
- `INVOICE_NOT_DELETABLE` — Cannot delete VERIFIED invoice
- `INVOICE_INVALID_STATUS_TRANSITION` — Invalid status change (e.g., VERIFIED → REJECTED)
- `INVOICE_ALREADY_VERIFIED` — Invoice already verified
- `INVOICE_ALREADY_REJECTED` — Invoice already rejected
- `INVOICE_ITEMS_REQUIRED` — At least one item required
- `INVOICE_CALCULATION_MISMATCH` — Calculated totals don't match (if client sends totals)
- `CATEGORY_NOT_FOUND` — Category not found
- `CATEGORY_NAME_EXISTS` — Category name already exists in company
- `CATEGORY_HAS_INVOICES` — Cannot delete category with assigned invoices
- `CATEGORY_ACCESS_DENIED` — Category belongs to another company

---

## PAGINATION SPECIFICATION

Same as Phase 6. All list endpoints support:
- `page` (0-based, default 0)
- `size` (default 20, max 100)
- `sort` (e.g., `invoiceDate,desc`, `totalAmount,asc`, `supplierName,asc`)

Response wrapped in standard paginated format.

---

## DATABASE CHANGES

### No New Tables Required
This phase uses the existing `invoices`, `invoice_items`, and `categories` tables from Phase 3.

### Potential Migration (if needed)
If missing columns or constraints are discovered, create:

**File**: `backend/src/main/resources/db/migration/V5__faz_7_invoice_adjustments.sql`

**IMPORTANT**: Check existing schema FIRST. The Phase 3 schema already includes all necessary columns including `source_type`, `llm_provider`, `confidence_score`, `original_file_*`, `e_invoice_*` fields. Only create migration if something is genuinely missing.

Possible additions to watch for:
- If `invoice_number + company_id` unique index needs adjustment for soft delete awareness
- If additional search indexes are needed for supplier name text search

---

## TESTING REQUIREMENTS

### Unit Tests

1. **InvoiceServiceTest**:
   - Create invoice with valid data and items → success, totals auto-calculated
   - Create invoice with duplicate invoice number in same company → error
   - Create invoice with duplicate invoice number in different company → success
   - Create invoice with no items → error (INVOICE_ITEMS_REQUIRED)
   - Update PENDING invoice → success
   - Update VERIFIED invoice → error (INVOICE_NOT_EDITABLE)
   - Update invoice: add new item, update existing item, remove old item → success
   - Delete invoice (soft delete) → success
   - Delete VERIFIED invoice → error (INVOICE_NOT_DELETABLE)
   - Verify PENDING invoice → status changes to VERIFIED, verifiedAt and verifiedByUserId set
   - Verify already VERIFIED invoice → error
   - Reject PENDING invoice with reason → status changes to REJECTED
   - Reject without reason → validation error
   - Reopen REJECTED invoice → status back to PENDING
   - Item total calculation: quantity × unitPrice = subtotal, subtotal × taxRate/100 = taxAmount
   - Invoice total calculation: sum of all item subtotals/taxAmounts/totals

2. **CategoryServiceTest**:
   - Create category → success
   - Create category with duplicate name in same company → error
   - Delete category with no invoices → success
   - Delete category with invoices → error (or set invoices' categoryId to null — decide policy)

3. **InvoiceStatusStateMachine Test**:
   - PENDING → VERIFIED: valid
   - PENDING → REJECTED: valid
   - REJECTED → PENDING: valid
   - VERIFIED → REJECTED: invalid
   - VERIFIED → PENDING: invalid
   - PROCESSING → VERIFIED: invalid (reserved for LLM pipeline)

### Integration Tests

1. **InvoiceController Integration Tests** (with Testcontainers recommended):
   - POST /invoices → 201 Created with auto-calculated totals
   - GET /invoices → 200 OK paginated list (only current company's invoices)
   - GET /invoices/{id} → 200 OK with items included
   - PUT /invoices/{id} → 200 OK (only for PENDING)
   - DELETE /invoices/{id} → 204 No Content (only ADMIN/MANAGER)
   - PATCH /invoices/{id}/verify → 200 OK
   - PATCH /invoices/{id}/reject → 200 OK
   - INTERN tries PUT → 403 Forbidden
   - ACCOUNTANT tries DELETE → 403 Forbidden
   - User from Company A tries to access Company B invoice → 403

2. **CategoryController Integration Tests**:
   - POST /categories → 201 Created
   - GET /categories → 200 OK (active categories)
   - INTERN tries POST /categories → 403 Forbidden

### Manual Testing Steps

1. **Create a category**:
   ```
   POST /api/v1/categories
   Body: { "name": "Teknoloji", "color": "#3B82F6" }
   → Expect: 201 Created
   ```

2. **Create an invoice with items**:
   ```
   POST /api/v1/invoices
   Body: { "invoiceNumber": "FTR-001", "invoiceDate": "2026-02-10", "supplierName": "Test Supplier", "currency": "TRY", "categoryId": "...", "items": [{ "description": "Item 1", "quantity": 2, "unit": "ADET", "unitPrice": 100, "taxRate": 18 }] }
   → Expect: 201, subtotal=200, taxAmount=36, totalAmount=236
   ```

3. **List invoices**:
   ```
   GET /api/v1/invoices?page=0&size=10&sort=invoiceDate,desc
   → Expect: 200 with paginated response
   ```

4. **Get invoice detail**:
   ```
   GET /api/v1/invoices/{id}
   → Expect: 200 with items array populated
   ```

5. **Update invoice**:
   ```
   PUT /api/v1/invoices/{id}
   Body: { ...updated fields, items: [...] }
   → Expect: 200 with recalculated totals
   ```

6. **Verify invoice**:
   ```
   PATCH /api/v1/invoices/{id}/verify
   Body: { "notes": "Looks good" }
   → Expect: 200, status=VERIFIED, verifiedAt set
   ```

7. **Try editing verified invoice**:
   ```
   PUT /api/v1/invoices/{id}
   → Expect: 400 INVOICE_NOT_EDITABLE
   ```

8. **Reject invoice**:
   ```
   PATCH /api/v1/invoices/{id}/reject
   Body: { "rejectionReason": "Wrong amount" }
   → Expect: 200, status=REJECTED
   ```

9. **Reopen rejected invoice**:
   ```
   PATCH /api/v1/invoices/{id}/reopen
   → Expect: 200, status=PENDING
   ```

10. **Test role permissions**:
    - Login as INTERN → POST /invoices → 201 (can create)
    - Login as INTERN → PUT /invoices/{id} → 403 (cannot edit)
    - Login as ACCOUNTANT → DELETE /invoices/{id} → 403 (cannot delete)

---

## VERIFICATION CHECKLIST

After completing this phase, verify:

- [ ] Invoice domain entity created with all fields matching DB schema
- [ ] InvoiceItem domain entity created with all fields
- [ ] Value objects: InvoiceStatus, SourceType, LlmProvider, Currency enums
- [ ] Invoice repository port (domain) and adapter (infrastructure)
- [ ] InvoiceJpaEntity with @OneToMany for items (cascade ALL, orphanRemoval true)
- [ ] InvoiceItemJpaEntity with proper mapping
- [ ] InvoiceService with create, read, update, delete, verify, reject, reopen
- [ ] Auto-calculation: item-level and invoice-level totals computed correctly
- [ ] Manual invoice creation sets sourceType=MANUAL, status=PENDING
- [ ] Status transitions enforced (PENDING→VERIFIED, PENDING→REJECTED, REJECTED→PENDING)
- [ ] VERIFIED invoices cannot be edited or deleted
- [ ] Invoice number unique per company (accounting for soft deletes)
- [ ] Company-based isolation on all queries
- [ ] Category CRUD working (create, list, update, delete)
- [ ] Category name unique per company
- [ ] Pagination and sorting on invoice list
- [ ] RBAC enforced: INTERN cannot edit/delete, ACCOUNTANT cannot delete
- [ ] HATEOAS links included (or consistent response format at minimum)
- [ ] Proper error responses with specific error codes
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] No linting errors
- [ ] CI pipeline passes

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_7_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (3-4 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created/Modified
Organized by layer:
```
domain/invoice/
├── entity/Invoice.java
├── entity/InvoiceItem.java
├── valueobject/InvoiceStatus.java
├── valueobject/SourceType.java
├── valueobject/LlmProvider.java
├── valueobject/Currency.java
└── port/InvoiceRepository.java

domain/category/
├── entity/Category.java
└── port/CategoryRepository.java

application/invoice/
├── InvoiceService.java
├── dto/CreateInvoiceCommand.java
├── dto/UpdateInvoiceCommand.java
├── dto/VerifyInvoiceCommand.java
├── dto/RejectInvoiceCommand.java
├── dto/InvoiceResponse.java
├── dto/InvoiceDetailResponse.java
└── dto/InvoiceListResponse.java

application/category/
├── CategoryService.java
├── dto/CreateCategoryCommand.java
├── dto/UpdateCategoryCommand.java
└── dto/CategoryResponse.java

infrastructure/persistence/invoice/
├── InvoiceJpaEntity.java
├── InvoiceItemJpaEntity.java
├── InvoiceJpaRepository.java
├── InvoiceRepositoryAdapter.java
└── InvoiceMapper.java

infrastructure/persistence/category/
├── CategoryJpaEntity.java
├── CategoryJpaRepository.java
├── CategoryRepositoryAdapter.java
└── CategoryMapper.java

interfaces/rest/invoice/
├── InvoiceController.java
└── dto/
    ├── CreateInvoiceRequest.java
    ├── UpdateInvoiceRequest.java
    ├── InvoiceItemRequest.java
    ├── VerifyInvoiceRequest.java
    ├── RejectInvoiceRequest.java
    ├── InvoiceDetailResponse.java
    ├── InvoiceListResponse.java
    └── InvoiceItemResponse.java

interfaces/rest/category/
├── CategoryController.java
└── dto/
    ├── CreateCategoryRequest.java
    ├── UpdateCategoryRequest.java
    └── CategoryResponse.java
```

### 4. API Endpoints Summary
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/v1/invoices | Create invoice | ✅/❌ |
| GET | /api/v1/invoices | List invoices | ✅/❌ |
| ... | ... | ... | ... |

### 5. Test Results
- Unit test count and results
- Integration test count and results
- Manual test curl outputs

### 6. Calculation Verification
Show at least one example of auto-calculation working correctly:
- Item-level: quantity × unitPrice = subtotal, etc.
- Invoice-level: sum of items

### 7. Status Workflow Verification
| Transition | Expected | Actual |
|------------|----------|--------|
| PENDING → VERIFIED | ✅ Allowed | ✅/❌ |
| PENDING → REJECTED | ✅ Allowed | ✅/❌ |
| REJECTED → PENDING | ✅ Allowed | ✅/❌ |
| VERIFIED → * | ❌ Blocked | ✅/❌ |

### 8. Database Changes
- List any migration files created (or "No migration needed")
- Note any observations about schema for future phases

### 9. Issues Encountered
Document any problems and their solutions.

### 10. Next Steps
What needs to be done in Phase 8 (Audit Log) and Phase 9 (Duplication Control). Note any:
- Integration points needed for audit logging on invoice operations
- Fields that might be used for duplicate detection

### 11. Time Spent
Actual time vs estimated (3-4 days).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 3**: Database Schema (invoices, invoice_items, categories tables) ✅
- **Phase 4**: Authentication (JWT) ✅
- **Phase 5**: RBAC (permissions, company isolation) ✅
- **Phase 6**: Company & User Management (company context, user info) ✅

### Required By (blocks these phases)
- **Phase 8**: Audit Log (needs invoice events to log)
- **Phase 9**: Duplication Control (needs invoice creation hook)
- **Phase 12**: Frontend Invoice CRUD UI (needs these API endpoints)
- **Phase 19-B**: RabbitMQ Producer (needs invoice events to publish)
- **Phase 23**: Advanced Filtering (builds on invoice queries)
- **Phase 24**: Export (needs invoice data access)
- **Phase 26**: Dashboard Stats (needs invoice counts/sums)

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Manual invoice creation works with items and auto-calculated totals
2. ✅ Invoice listing is paginated, sorted, and company-scoped
3. ✅ Invoice detail includes all items
4. ✅ Invoice update handles add/modify/remove items correctly
5. ✅ Soft delete works for invoices
6. ✅ Status workflow: verify, reject, reopen all work correctly
7. ✅ VERIFIED invoices are immutable (cannot edit/delete)
8. ✅ Invoice number uniqueness enforced per company
9. ✅ Category CRUD works with company isolation
10. ✅ RBAC enforced: correct permissions per role
11. ✅ Multi-tenant isolation: no cross-company data access
12. ✅ All tests pass (unit + integration)
13. ✅ Result file created at `docs/OMER/step_results/faz_7_result.md`

---

## IMPORTANT NOTES

1. **Existing Tables**: The `invoices`, `invoice_items`, and `categories` tables already exist from Phase 3. Build JPA entities to match them exactly. Do NOT recreate tables.
2. **sourceType for this phase**: Always `MANUAL`. The `LLM` and `E_INVOICE` source types will be set by future phases (13-18).
3. **LLM fields nullable**: `llmProvider`, `confidenceScore`, `processingDurationMs`, `originalFile*`, `eInvoice*` are all nullable and irrelevant for manual invoices. They will be populated by the Python extraction service in later phases.
4. **Invoice number uniqueness**: The unique index `idx_invoices_company_invoice_number` already exists in Phase 3 with `WHERE is_deleted = FALSE`. This means soft-deleted invoice numbers can be reused.
5. **Cascade delete on items**: Use `orphanRemoval = true` on the JPA relationship so that removing an item from the invoice's item list automatically deletes it from the database.
6. **BigDecimal for money**: Always use `BigDecimal` for financial calculations. Never use `float` or `double`.
7. **CompanyContextHolder**: Use this from Phase 5 to get the current user's company ID for all queries.
8. **@PreAuthorize annotations**: Use the custom security annotations from Phase 5 (@CanEditInvoice, @CanDeleteInvoice, etc.) on controller methods.

---

**Phase 7 Completion Target**: Complete Invoice CRUD with items, status workflow, category management, and company-scoped multi-tenant isolation.

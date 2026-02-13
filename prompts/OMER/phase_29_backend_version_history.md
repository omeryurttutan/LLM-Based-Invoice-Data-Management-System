# PHASE 29: VERSION HISTORY & ROLLBACK — BACKEND API

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-28 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend layout, auth pages, invoice list/CRUD UI
- ✅ Phase 13-22: Full extraction pipeline, upload, verification UI
- ✅ Phase 23-26: Filtering, Export, Dashboard
- ✅ Phase 27-28: Notification system (in-app WebSocket, email, push, preferences)

### Relevant Existing Infrastructure
- **Invoice Entity (Phase 7)**: Full invoice with fields — invoice_number, invoice_date, due_date, supplier_name, supplier_tax_number, buyer_name, buyer_tax_number, subtotal, tax_amount, total_amount, currency, status, category, notes, source_type, llm_provider, confidence_score, etc.
- **Invoice Items (Phase 3/7)**: invoice_items table — description, quantity, unit_price, tax_rate, tax_amount, line_total
- **Audit Log (Phase 8)**: Immutable log of WHO did WHAT and WHEN. Records actions but does NOT store full data snapshots and does NOT support rollback.
- **Soft Delete**: Invoices use soft delete (deleted_at column)

### Why Version History is Different from Audit Log
- **Audit Log (Phase 8)**: Records the action ("User X updated invoice Y at time Z"). It's a log of events.
- **Version History (this phase)**: Stores the FULL DATA SNAPSHOT before each change. It enables viewing what the data looked like at any point, comparing two versions side by side, and REVERTING to a previous version.

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 2-3 days (backend API portion)

### Relationship to Phase 29-F
Phase 29 is split:
- **29 (this phase)**: Backend — invoice_versions table, snapshot logic, REST API for versions/diff/revert
- **29-F**: Frontend (FURKAN) — version timeline, diff viewer, revert confirmation UI

---

## OBJECTIVE

Build the backend infrastructure for invoice version history: a table that stores full snapshots of invoice data before each modification, REST endpoints to list versions, view a specific version, compare two versions (diff), and revert an invoice to a previous version. Every time an invoice is updated (manually or by LLM re-extraction), the current state is automatically saved as a version before applying the change.

---

## DETAILED REQUIREMENTS

### 1. Database: invoice_versions Table

Create via Flyway migration: `V{next_number}__phase_29_version_history.sql`

**invoice_versions table:**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| invoice_id | BIGINT | NOT NULL, FK → invoices(id) | Parent invoice |
| version_number | INTEGER | NOT NULL | Sequential per invoice (1, 2, 3...) |
| snapshot_data | JSONB | NOT NULL | Full invoice data at this point in time |
| items_snapshot | JSONB | NOT NULL | Full invoice items array at this point in time |
| change_source | VARCHAR(50) | NOT NULL | What triggered the change (see enum below) |
| change_summary | TEXT | NULL | Human-readable summary of what changed |
| changed_fields | JSONB | NULL | List of field names that changed |
| changed_by | BIGINT | NOT NULL, FK → users(id) | Who made the change |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | When this version was created |

**Indexes:**
- idx_invoice_versions_invoice ON (invoice_id)
- idx_invoice_versions_invoice_version ON (invoice_id, version_number DESC) — for latest version lookup
- UNIQUE constraint on (invoice_id, version_number)

**change_source Enum Values:**

| Value | Description |
|---|---|
| MANUAL_EDIT | User edited invoice fields manually |
| LLM_EXTRACTION | LLM extraction populated/updated the data |
| LLM_RE_EXTRACTION | User triggered re-extraction on existing invoice |
| VERIFICATION | User approved/rejected during verification (Phase 22) |
| STATUS_CHANGE | Status changed (e.g., PENDING → VERIFIED) |
| REVERT | Reverted to a previous version |
| BULK_UPDATE | Updated as part of a batch operation |

### 2. Automatic Version Creation

**When to create a version:**
Every time the invoice entity is updated, BEFORE applying the change, snapshot the current state.

**Integration Points:**
- Invoice UPDATE endpoint (PUT /api/v1/invoices/{id}) — before saving changes
- Invoice status change (verify, reject, reopen)
- LLM extraction result updating the invoice (from result listener, Phase 19-B)
- Revert operation itself (save current state before reverting)

**Implementation Approach:**
- Option A (Recommended): Use a JPA @PreUpdate entity listener or a Spring AOP aspect that intercepts invoice save operations
- Option B: Explicitly call versionService.createSnapshot() in each service method that modifies an invoice
- Option A is cleaner but Option B is more explicit. Choose based on preference, but ensure no update path is missed.

**Snapshot Creation Flow:**
1. Load current invoice from DB (before applying changes)
2. Serialize invoice fields to JSON → snapshot_data
3. Serialize invoice items to JSON → items_snapshot
4. Calculate which fields changed (compare current vs incoming data) → changed_fields
5. Generate change_summary (e.g., "Tedarikçi adı güncellendi, tutar değişti")
6. Determine change_source based on context
7. Assign next version_number (max existing + 1)
8. Save the version record
9. Then apply the actual update to the invoice

### 3. Snapshot Data Format

**snapshot_data JSONB:**
All invoice-level fields serialized as JSON. This should capture every field that could be displayed or compared:

- invoice_number, invoice_date, due_date
- supplier_name, supplier_tax_number, supplier_address
- buyer_name, buyer_tax_number, buyer_address
- subtotal, tax_amount, total_amount, currency
- category_id, category_name
- status
- notes
- source_type, llm_provider, confidence_score
- Any other fields on the invoice entity

**items_snapshot JSONB:**
Array of invoice items, each with:
- description, quantity, unit_price, tax_rate, tax_amount, line_total

**Why JSONB instead of relational tables:**
- Version snapshots are read-only historical data
- JSONB avoids creating a parallel relational structure
- Easy to add new fields without migration
- Efficient storage and querying for this use case

### 4. REST API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | /api/v1/invoices/{id}/versions | List all versions of an invoice | Company-scoped |
| GET | /api/v1/invoices/{id}/versions/{versionId} | Get a specific version's full snapshot | Company-scoped |
| GET | /api/v1/invoices/{id}/versions/diff | Compare two versions (query params) | Company-scoped |
| POST | /api/v1/invoices/{id}/revert/{versionNumber} | Revert invoice to a previous version | ADMIN, MANAGER |
| GET | /api/v1/invoices/{id}/versions/latest | Get the latest (current) version number | Company-scoped |

### 5. GET /api/v1/invoices/{id}/versions

Returns a list of all versions for an invoice, ordered by version_number DESC (newest first).

**Response: array of version summaries:**

| Field | Type | Description |
|---|---|---|
| id | long | Version record ID |
| versionNumber | integer | Sequential version number |
| changeSource | string | What triggered the change |
| changeSummary | string | Human-readable description |
| changedFields | array of strings | List of field names that changed |
| changedBy | object | { id, fullName, email } — who made the change |
| createdAt | string (ISO 8601) | When this version was created |

Note: This endpoint does NOT return the full snapshot data (too heavy for a list). Use the specific version endpoint for full data.

### 6. GET /api/v1/invoices/{id}/versions/{versionId}

Returns the complete snapshot data for a specific version.

**Response:**

| Field | Type | Description |
|---|---|---|
| id | long | Version record ID |
| versionNumber | integer | |
| changeSource | string | |
| changeSummary | string | |
| changedFields | array | |
| changedBy | object | |
| createdAt | string | |
| snapshotData | object | Full invoice data JSON |
| itemsSnapshot | array | Full invoice items array |

### 7. GET /api/v1/invoices/{id}/versions/diff

Compares two versions and returns the differences.

**Query Parameters:**
- `from` (required): version number of the older version (e.g., 2)
- `to` (required): version number of the newer version (e.g., 5)

**Response:**

| Field | Type | Description |
|---|---|---|
| fromVersion | integer | |
| toVersion | integer | |
| fromCreatedAt | string | |
| toCreatedAt | string | |
| changes | array of FieldChange | List of field-level differences |
| itemChanges | object | Changes to invoice items |

**FieldChange object:**

| Field | Type | Description |
|---|---|---|
| fieldName | string | e.g., "supplier_name", "total_amount" |
| fieldLabel | string | Turkish display label (e.g., "Tedarikçi Adı", "Genel Toplam") |
| oldValue | any | Value in the `from` version |
| newValue | any | Value in the `to` version |
| changeType | string | MODIFIED, ADDED, REMOVED |

**itemChanges object:**

| Field | Type | Description |
|---|---|---|
| added | array | Items present in `to` but not in `from` |
| removed | array | Items present in `from` but not in `to` |
| modified | array | Items changed between versions (with field-level diff) |

**Diff Calculation Logic:**
- Compare each field in snapshot_data between the two versions
- For items: match by index or by a combination of description + quantity (best effort)
- Only include fields that actually changed
- Provide Turkish labels for field names (map field_name → Turkish label)

**Turkish Field Label Mapping:**

| Field Name | Turkish Label |
|---|---|
| invoice_number | Fatura Numarası |
| invoice_date | Fatura Tarihi |
| due_date | Vade Tarihi |
| supplier_name | Tedarikçi Adı |
| supplier_tax_number | Tedarikçi Vergi No |
| buyer_name | Alıcı Adı |
| buyer_tax_number | Alıcı Vergi No |
| subtotal | Ara Toplam |
| tax_amount | KDV Tutarı |
| total_amount | Genel Toplam |
| currency | Para Birimi |
| category_name | Kategori |
| status | Durum |
| notes | Notlar |
| confidence_score | Güven Skoru |
| llm_provider | LLM Sağlayıcı |

### 8. POST /api/v1/invoices/{id}/revert/{versionNumber}

Reverts an invoice to the state captured in a specific version.

**Flow:**
1. Validate the user has permission (ADMIN or MANAGER role)
2. Find the version record by invoice_id and version_number
3. If not found → 404
4. Create a NEW version snapshot of the CURRENT state (before reverting) with change_source = REVERT
5. Apply the snapshot_data from the target version to the invoice entity
6. Apply the items_snapshot: delete current items, recreate from snapshot
7. Set a new version_number (max + 1) for the revert action
8. Save the updated invoice
9. Return the updated invoice

**Response:** The full invoice entity (same as GET /api/v1/invoices/{id})

**RBAC:** Only ADMIN and MANAGER can revert. ACCOUNTANT and INTERN cannot.

**Restrictions:**
- Cannot revert a soft-deleted invoice
- Cannot revert to version 0 (if such a concept exists — first version is always 1)
- Revert creates a new version (it doesn't delete history)
- The invoice status after revert is whatever the target version's status was

### 9. Version Limit and Cleanup

- Maximum versions per invoice: 50 (configurable via `VERSION_MAX_PER_INVOICE`, default 50)
- When creating a new version that would exceed the limit: delete the oldest version(s)
- This prevents unbounded growth for frequently edited invoices

### 10. First Version (Initial State)

When an invoice is first created (POST /api/v1/invoices or first LLM extraction):
- Create version 1 with change_source = LLM_EXTRACTION (or MANUAL_EDIT if manual creation)
- This is the baseline version

### 11. Configuration

- `VERSION_MAX_PER_INVOICE`: Default 50
- `VERSION_CLEANUP_ENABLED`: Default true

---

## TESTING REQUIREMENTS

### 1. Unit Tests
- Snapshot creation captures all invoice fields correctly
- Snapshot creation captures all invoice items
- Version number auto-increments correctly
- change_summary generated correctly
- changed_fields detected correctly
- Diff calculation identifies modified/added/removed fields
- Diff on items identifies added/removed/modified items
- Turkish labels mapped correctly
- Revert applies snapshot data to invoice
- Revert recreates items from snapshot
- Version limit enforced (oldest deleted when over limit)

### 2. Integration Tests
- Create invoice → version 1 created
- Update invoice → version 2 created with correct snapshot
- GET /versions returns list sorted by version_number DESC
- GET /versions/{id} returns full snapshot data
- GET /versions/diff?from=1&to=2 returns correct field changes
- POST /revert/1 reverts to version 1, creates new version record
- Revert by ACCOUNTANT → 403 Forbidden
- Revert non-existent version → 404
- Version count does not exceed max limit
- Company scoping: user cannot access versions of another company's invoice

---

## VERIFICATION CHECKLIST

### Database
- [ ] invoice_versions table created via Flyway
- [ ] Indexes on invoice_id and (invoice_id, version_number)
- [ ] UNIQUE constraint on (invoice_id, version_number)

### Automatic Versioning
- [ ] Version created on manual invoice update
- [ ] Version created on LLM extraction result
- [ ] Version created on status change (verify/reject)
- [ ] Version created before revert
- [ ] Correct change_source set for each trigger
- [ ] changed_fields accurately detected

### REST API
- [ ] GET /versions returns version list (without snapshot data)
- [ ] GET /versions/{id} returns full snapshot
- [ ] GET /versions/diff returns field-level differences
- [ ] POST /revert applies target version and creates new version
- [ ] RBAC enforced on revert (ADMIN/MANAGER only)
- [ ] Company scoping on all endpoints

### Diff Logic
- [ ] Modified fields detected
- [ ] Added/removed fields detected
- [ ] Item-level changes detected
- [ ] Turkish labels provided for all fields
- [ ] Unchanged fields excluded from diff

### Cleanup
- [ ] Max versions per invoice enforced
- [ ] Oldest versions pruned when limit exceeded

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_29_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified
4. Database migration details
5. Endpoint summary with example request/response for each
6. Diff algorithm explanation
7. Version creation triggers documented
8. RBAC rules for revert
9. Test results
10. Coordination notes for Furkan (API contracts, response formats)
11. Issues and solutions
12. Next steps (Phase 29-F frontend)

---

## DEPENDENCIES

### Requires
- **Phase 7**: Invoice CRUD API (invoice entity, update logic)
- **Phase 8**: Audit Log (version history complements audit log, different purpose)

### Required By
- **Phase 29-F**: Frontend version history UI (FURKAN)

---

## SUCCESS CRITERIA

1. ✅ invoice_versions table created with JSONB snapshots
2. ✅ Version automatically created on every invoice update
3. ✅ GET /versions returns version list with summaries
4. ✅ GET /versions/{id} returns full snapshot data
5. ✅ GET /versions/diff returns field-level and item-level differences with Turkish labels
6. ✅ POST /revert restores invoice to target version state
7. ✅ Revert creates new version (history is never lost)
8. ✅ RBAC: only ADMIN/MANAGER can revert
9. ✅ Company scoping enforced
10. ✅ Version limit enforced (max 50 per invoice)
11. ✅ All tests pass
12. ✅ Result file created

---

## IMPORTANT NOTES

1. **JSONB Snapshots are Immutable**: Once a version is created, its snapshot_data and items_snapshot MUST NEVER be modified. They are historical records.

2. **Don't Confuse with Audit Log**: Audit log (Phase 8) records actions. Version history stores data snapshots. They serve different purposes. An audit log entry says "User X updated invoice Y". A version record shows exactly what invoice Y looked like before and after the update.

3. **Revert Creates History, Doesn't Erase It**: Reverting to version 3 doesn't delete versions 4, 5, 6. It creates a NEW version (7) that has the same data as version 3. The full timeline is preserved.

4. **Performance**: JSONB snapshots can be large (especially with many invoice items). The version list endpoint intentionally excludes snapshot data — only the diff and detail endpoints return full data.

5. **Cascade Delete**: If an invoice is hard-deleted, all its versions should be cascade-deleted. For soft-delete, versions remain (user might undelete later).

6. **Item Matching for Diff**: Matching items between two versions is tricky since items might be reordered, added, or removed. A simple approach: match by array index, or match by (description + quantity) combination. Document the chosen approach.

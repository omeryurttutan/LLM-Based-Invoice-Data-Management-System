# PHASE 31: KVKK (TURKISH DATA PROTECTION LAW) COMPLIANCE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000
  - **RabbitMQ**: Port 5672
  - **Redis**: Port 6379

### Current State (Phases 0-30 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend layout, auth pages, invoice list/CRUD UI
- ✅ Phase 13-22: Full extraction pipeline (FastAPI, Gemini/GPT/Claude fallback, validation, e-Invoice XML, RabbitMQ async, file upload, verification UI)
- ✅ Phase 23-26: Filtering & Search, Export (XLSX/CSV + accounting formats), Dashboard
- ✅ Phase 27-28: Notification system (in-app WebSocket, email, push, user preferences)
- ✅ Phase 29: Version History & Rollback (invoice_versions, JSONB snapshots, diff, revert)
- ✅ Phase 30: Template Learning & Rule Engine (supplier_templates, automation_rules, rule evaluation, pipeline integration)

### Relevant Existing Infrastructure

**Users Table (Phase 3/4)**: users — id, email, password_hash, full_name, role, company_id, phone (nullable), created_at, updated_at, deleted_at (soft delete). Currently, phone is stored in plain text.

**Companies Table (Phase 3/6)**: companies — id, name, tax_number, address, phone, email, created_at, updated_at, deleted_at. Tax number and address are stored in plain text.

**Invoices Table (Phase 3/7)**: invoices — includes supplier_tax_number, buyer_tax_number, supplier_name, buyer_name. Tax numbers are stored in plain text.

**Audit Log (Phase 8)**: audit_logs — immutable log of all data changes. Records old_value and new_value as JSONB. Currently logs contain plain text values of sensitive fields.

**Soft Delete**: All main tables use soft delete (deleted_at column). Records are never physically removed — until this phase.

**Auth System (Phase 4)**: JWT-based with Redis token storage. BCrypt password hashing already in place.

### What is KVKK?

KVKK (Kişisel Verilerin Korunması Kanunu — Law No. 6698) is Turkey's data protection regulation, similar to GDPR. Key requirements for this system:

1. **Data Encryption**: Personal data must be encrypted at rest
2. **Consent Management**: Data processing requires explicit user consent
3. **Data Minimization**: Only collect necessary data
4. **Right to be Forgotten**: Users can request deletion of their personal data
5. **Data Retention**: Personal data must be deleted after the legal retention period
6. **Audit Trail**: All data access and modifications must be traceable (already done in Phase 8)
7. **Data Breach Notification**: Mechanisms to detect and report breaches

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Implement KVKK compliance mechanisms in the Spring Boot backend:

1. **AES-256-GCM encryption** for sensitive personal data fields (tax numbers, phone numbers, addresses)
2. **JPA AttributeConverter** for transparent encryption/decryption at the entity level
3. **Consent tracking** table and API for recording user data processing consent
4. **Right to be forgotten** endpoint that anonymizes/deletes a user's personal data
5. **Scheduled data retention job** that automatically hard-deletes soft-deleted records after the legal retention period
6. **KVKK compliance report** endpoint for administrators

---

## DETAILED REQUIREMENTS

### 1. Encryption Service

Build a reusable encryption/decryption service using AES-256-GCM.

**Why AES-256-GCM?**
- AES-256: 256-bit key, very strong encryption
- GCM (Galois/Counter Mode): Authenticated encryption — provides both confidentiality and integrity
- Each encryption produces a unique IV (Initialization Vector), so the same plaintext encrypted twice produces different ciphertext

**Service Requirements:**

- Accept a plaintext string, return an encrypted string (Base64 encoded)
- Accept an encrypted string, return the decrypted plaintext
- Use a master encryption key stored as an environment variable (`KVKK_ENCRYPTION_KEY`)
- The key should be a 256-bit (32 byte) key, provided as a Base64-encoded string in the environment
- Generate a random 12-byte IV for each encryption operation
- Store the IV prepended to the ciphertext (IV + ciphertext combined, then Base64 encoded)
- Use 128-bit authentication tag length (GCM default)
- Handle errors gracefully: if decryption fails (wrong key, corrupted data), log the error and return a placeholder value (e.g., "[ENCRYPTED]") rather than crashing

**Environment Variable:**
- `KVKK_ENCRYPTION_KEY`: Base64 encoded 32-byte key
- Add to .env.example with a placeholder and generation instructions
- Add to docker-compose.yml environment section

---

### 2. JPA AttributeConverter for Transparent Encryption

Create a JPA AttributeConverter that automatically encrypts data when writing to the database and decrypts when reading.

**Converter name**: `EncryptedStringConverter`

**How it works:**
- Implements `AttributeConverter<String, String>`
- `convertToDatabaseColumn(String attribute)`: calls encryptionService.encrypt(attribute)
- `convertToEntityAttribute(String dbData)`: calls encryptionService.decrypt(dbData)
- Handles null values (null in → null out)

**Apply to these fields:**

| Entity | Field | Current Type | Notes |
|---|---|---|---|
| User | phone | VARCHAR(20) | Nullable — only encrypt if not null |
| Company | taxNumber | VARCHAR(20) | Company VKN — always present |
| Company | address | TEXT | Company address — nullable |
| Company | phone | VARCHAR(20) | Company phone — nullable |
| Invoice | supplierTaxNumber | VARCHAR(20) | Supplier VKN/TCKN |
| Invoice | buyerTaxNumber | VARCHAR(20) | Buyer VKN/TCKN |

**Database Migration:**

Create a Flyway migration: `V{next_number}__phase_31_kvkk_encryption.sql`

This migration needs to:
1. Increase column sizes for encrypted fields. Encrypted values are longer than plain text (Base64 of IV + ciphertext + auth tag). A 10-character VKN becomes approximately 60-80 characters when encrypted. Increase VARCHAR columns to VARCHAR(500) for encrypted fields.
2. Add a `data_encrypted` flag column to a system settings or metadata table so the application knows whether existing data has been migrated to encrypted form.

**IMPORTANT — Data Migration Strategy:**

Existing data in the database is in plain text. You need a one-time data migration:

1. Create a Spring Boot `CommandLineRunner` or `ApplicationRunner` that runs on startup (controlled by a flag)
2. Check if data_encrypted flag is false
3. If false: read all records with sensitive fields, encrypt them, update the records, then set data_encrypted = true
4. This should run in batches (e.g., 100 records at a time) to avoid memory issues
5. Make this migration idempotent — if it runs twice, it should not double-encrypt
6. Add a detection mechanism: try to decrypt the value; if it fails, assume it's already plain text and encrypt it

**Note on Searching Encrypted Fields:**

Once fields are encrypted, you CANNOT do SQL LIKE or equality searches on them directly. This affects:
- Searching invoices by supplier_tax_number — the existing filter from Phase 23 uses this
- Searching companies by tax_number

**Solutions:**
- For exact match lookups (tax number): store a hash (SHA-256) of the plain text in a separate indexed column (e.g., `supplier_tax_number_hash`). Search by hash, then decrypt the actual value for display.
- For the supplier templates system (Phase 30): the `supplier_tax_number` in supplier_templates should also use hashed lookups.
- Document which searches are affected and the workaround used.

---

### 3. Consent Tracking

**3.1 Database: user_consents Table**

Create via Flyway migration: `V{next_number}__phase_31_consent_tracking.sql`

**user_consents table:**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL, FK → users(id) | User who gave consent |
| company_id | BIGINT | NOT NULL, FK → companies(id) | Company context |
| consent_type | VARCHAR(50) | NOT NULL | Type of consent (see enum below) |
| consent_version | VARCHAR(20) | NOT NULL | Version of the consent text (e.g., "1.0", "1.1") |
| is_granted | BOOLEAN | NOT NULL | TRUE = consented, FALSE = revoked |
| ip_address | INET | NOT NULL | IP address at time of consent |
| user_agent | TEXT | NULL | Browser/client info |
| granted_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | When consent was given |
| revoked_at | TIMESTAMP | NULL | When consent was revoked (if applicable) |
| metadata | JSONB | NULL | Additional context (e.g., consent text hash) |

**consent_type Enum Values:**

| Value | Description |
|---|---|
| DATA_PROCESSING | General data processing consent (required for using the system) |
| DATA_SHARING | Consent to share data with third parties (e.g., LLM providers) |
| MARKETING | Marketing communications consent (optional) |
| ANALYTICS | Usage analytics consent (optional) |

**Indexes:**
- idx_user_consents_user ON (user_id)
- idx_user_consents_company ON (company_id)
- idx_user_consents_type ON (user_id, consent_type, granted_at DESC) — for latest consent lookup
- UNIQUE constraint on (user_id, consent_type, consent_version) — prevents duplicate consent records

**3.2 Consent API**

| Method | Endpoint | Description | RBAC |
|---|---|---|---|
| POST | /api/v1/consent | Record a new consent (grant or revoke) | All authenticated users (for their own consent) |
| GET | /api/v1/consent | Get current user's consent status for all types | All authenticated users |
| GET | /api/v1/consent/history | Get current user's full consent history | All authenticated users |
| GET | /api/v1/admin/consent/user/{userId} | Admin view of a specific user's consent | ADMIN only |
| GET | /api/v1/admin/consent/report | Consent overview report for the company | ADMIN only |

**POST /api/v1/consent Request:**
- consentType (String, required): one of the enum values
- consentVersion (String, required): version of the consent text
- isGranted (Boolean, required): true to grant, false to revoke

**Consent Logic:**
- When a user registers (Phase 4), DATA_PROCESSING consent is required. The system should check for this.
- DATA_SHARING consent should be checked before sending invoice data to LLM providers. If not granted, the system should still work but log a warning (for MVP, do not block LLM processing — just track consent status).
- MARKETING and ANALYTICS are optional and do not affect system functionality.
- Consent can be revoked at any time. Revoking DATA_PROCESSING consent should trigger a warning that the user may need to be deactivated.

---

### 4. Right to Be Forgotten (Unutulma Hakkı)

**Endpoint**: `POST /api/v1/admin/gdpr/forget/{userId}`

**RBAC**: ADMIN only

**What this endpoint does:**

1. **Validate**: Check that the target user exists and belongs to the same company as the admin
2. **Anonymize User Data**:
   - Replace full_name with "Anonim Kullanıcı #{userId}"
   - Replace email with "deleted_{userId}@anonymized.local"
   - Set phone to NULL
   - Set password_hash to a random unusable value (prevents login)
   - Keep the user record (for referential integrity) but mark as deleted (soft delete)
3. **Anonymize Related Invoice Data**:
   - For invoices created by this user: keep the invoice data (business records have legal retention requirements) but remove any personal identifiers that link to the user
   - Do NOT delete invoices — they are business records with tax implications
4. **Anonymize Audit Logs**:
   - Replace user_email in audit_logs where user_id matches with "anonymized"
   - Do NOT delete audit logs — they are legally required
5. **Delete Consent Records**: Hard delete all consent records for this user (consent data itself is personal data)
6. **Revoke Tokens**: Delete all Redis tokens for this user (force logout)
7. **Log the Action**: Create an audit log entry recording that the right to be forgotten was exercised (recording WHO performed the action, WHEN, for which user — but not the deleted data)
8. **Send Notification**: Notify ADMIN users that a data deletion was performed
9. **Return**: A summary of what was anonymized/deleted

**Important Constraints:**
- This action is IRREVERSIBLE. The endpoint should require a confirmation parameter (e.g., `?confirm=true`)
- A user cannot delete themselves — only an ADMIN can perform this
- The ADMIN performing the action cannot delete their own data
- Document the legal retention period consideration: Turkish tax law requires keeping financial records for 5 years. Invoice data related to the user should be retained but anonymized (remove personal link, keep financial data)

---

### 5. Data Retention Scheduled Job

**Purpose**: Automatically hard-delete records that have been soft-deleted beyond the legal retention period.

**Implementation**: Spring `@Scheduled` job that runs daily (configurable).

**Retention Periods** (configurable via application.yml):

| Data Type | Retention Period | Rationale |
|---|---|---|
| Soft-deleted users | 30 days | Grace period for accidental deletion |
| Soft-deleted invoices | 5 years (1825 days) | Turkish tax law requirement (VUK 253. madde) |
| Soft-deleted companies | 5 years | May have associated invoices under retention |
| Audit logs | 10 years | Legal compliance requirement |
| Notification records | 90 days | No legal requirement, cleanup for performance |
| Rule execution logs | 1 year (365 days) | Operational record |

**Job Logic:**
1. Find all soft-deleted records where `deleted_at < NOW() - retention_period`
2. For each record, perform hard delete (CASCADE will handle child records)
3. Before hard-deleting a user: verify no invoices under retention still reference this user. If they do, anonymize the user instead of deleting.
4. Log the number of records deleted per entity type
5. Send a summary notification to ADMIN users

**Configuration (application.yml):**
- `app.kvkk.retention.users-days`: 30
- `app.kvkk.retention.invoices-days`: 1825
- `app.kvkk.retention.companies-days`: 1825
- `app.kvkk.retention.audit-logs-days`: 3650
- `app.kvkk.retention.notifications-days`: 90
- `app.kvkk.retention.rule-logs-days`: 365
- `app.kvkk.retention.job-cron`: "0 0 2 * * ?" (daily at 2 AM)
- `app.kvkk.retention.enabled`: true

---

### 6. KVKK Compliance Report

**Endpoint**: `GET /api/v1/admin/kvkk/report`

**RBAC**: ADMIN only

**Returns a comprehensive report:**

- **Encryption Status**:
  - Data encryption enabled: true/false
  - Number of encrypted fields
  - List of encrypted entity fields
  - Data migration status (all existing data encrypted: true/false)

- **Consent Summary**:
  - Total users in the company
  - Users with DATA_PROCESSING consent: count and percentage
  - Users with DATA_SHARING consent: count and percentage
  - Users with revoked consents: count
  - Users without any consent recorded: count (compliance risk)

- **Data Retention Status**:
  - Retention job enabled: true/false
  - Last run timestamp
  - Records pending deletion (past retention period but not yet deleted)
  - Breakdown by entity type

- **Right to Be Forgotten**:
  - Total forget requests processed (count of anonymized users)
  - Last request timestamp

- **Audit Trail**:
  - Audit logging active: true/false
  - Total audit records
  - Oldest audit record date

- **Risk Indicators**:
  - Users without consent: HIGH risk if > 0
  - Unencrypted data detected: HIGH risk if true
  - Retention job disabled: MEDIUM risk
  - Records past retention period: LOW risk

---

### 7. Audit Log Enhancement for Sensitive Data

The existing audit log (Phase 8) records old_value and new_value as JSONB. When these contain sensitive fields, the logged values should be masked.

**Enhancement:**
- Before writing to audit_logs, check if the entity being logged contains encrypted fields
- For those fields, replace the actual value with a masked version in the log:
  - Tax numbers: "12345*****" (show first 5 digits, mask the rest)
  - Phone numbers: "+90 5** *** **12" (show country code and last 2 digits)
  - Addresses: "[MASKED]"
  - Email: "us***@domain.com" (show first 2 chars and domain)
- This prevents sensitive data from being exposed in audit logs
- The audit log still records THAT a change happened and WHO made it, just not the full sensitive value

---

### 8. Data Minimization Validation

Add validation annotations/checks to ensure the system only collects necessary data:

- Mark optional personal fields clearly in DTOs (phone, address are optional)
- Add a configuration flag `app.kvkk.strict-minimization` (default: false)
- When strict-minimization is true:
  - Warn when optional personal fields are provided in registration (do not block, just log)
  - Do not return sensitive fields in list endpoints (only in detail endpoints when specifically requested)
  - Mask sensitive fields in export files (XLSX/CSV) unless the user has ADMIN role

---

### 9. Testing Requirements

Write tests for:

- Encryption service: encrypt/decrypt round-trip, different inputs, null handling, wrong key detection
- AttributeConverter: entity save and load with encryption, null field handling
- Consent API: grant, revoke, history, admin view
- Right to be forgotten: user anonymization, related data handling, irreversibility
- Data retention job: identify records past retention, hard delete, skip records with dependencies
- KVKK report: verify all sections return correct data
- Audit log masking: verify sensitive fields are masked in logs
- Hash-based search: verify tax number lookup works with hashed column
- Data migration runner: verify existing plain text data is correctly encrypted

---

### 10. Configuration Summary

Add to application.yml:

- `app.kvkk.encryption.enabled`: true (global toggle for encryption)
- `app.kvkk.encryption.key-env-variable`: KVKK_ENCRYPTION_KEY
- `app.kvkk.consent.data-processing-required`: true
- `app.kvkk.consent.current-version`: "1.0"
- `app.kvkk.strict-minimization`: false
- `app.kvkk.retention.enabled`: true
- `app.kvkk.retention.job-cron`: "0 0 2 * * ?"
- `app.kvkk.retention.users-days`: 30
- `app.kvkk.retention.invoices-days`: 1825
- `app.kvkk.retention.companies-days`: 1825
- `app.kvkk.retention.audit-logs-days`: 3650
- `app.kvkk.retention.notifications-days`: 90
- `app.kvkk.retention.rule-logs-days`: 365
- `app.kvkk.audit-masking.enabled`: true

Add to .env.example:
- `KVKK_ENCRYPTION_KEY=<base64-encoded-32-byte-key>`
- Include a comment with key generation command: `openssl rand -base64 32`

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_31.0_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Database changes (list all migration files created, altered columns, new tables)
3. Files created or modified (with paths)
4. Encryption implementation details (algorithm, key management, IV strategy)
5. Encrypted fields list and column size changes
6. Hash-based search implementation for encrypted fields
7. Data migration strategy for existing plain text data
8. Consent API documentation (endpoints, request/response)
9. Right to be forgotten flow documentation
10. Data retention job configuration and schedule
11. KVKK report endpoint documentation
12. Audit log masking implementation
13. Test results (unit and integration tests summary)
14. Configuration properties added
15. Impact on existing features (especially Phase 23 filtering by tax number, Phase 30 template lookup)
16. Issues encountered and solutions
17. Next steps (Phase 32 Rate Limiting, any frontend changes needed for consent UI)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 3**: Database schema (tables to be altered)
- **Phase 4**: Authentication (user entity, JWT tokens, Redis)
- **Phase 7**: Invoice CRUD (invoice entity with tax number fields)
- **Phase 8**: Audit Log (to be enhanced with masking)
- **Phase 23**: Filtering (search by tax number — affected by encryption, needs hash-based workaround)
- **Phase 27**: Notification Service (for retention job notifications)
- **Phase 30**: Template Learning (supplier_tax_number lookup — affected by encryption)

### Required By
- **Phase 32**: Rate Limiting & Security Hardening (builds on security infrastructure)
- **Phase 35**: Unit Tests (KVKK service is a test target)
- **Phase 39**: Production Environment (encryption key management in production)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] AES-256-GCM encryption service encrypts and decrypts correctly (round-trip test)
- [ ] JPA AttributeConverter auto-encrypts on save and auto-decrypts on read
- [ ] Encrypted fields: supplier_tax_number, buyer_tax_number, phone, address are stored encrypted in DB
- [ ] Direct DB query shows encrypted (unreadable) values for sensitive fields
- [ ] Column sizes increased via Flyway migration to accommodate encrypted data
- [ ] Hash columns added for searchable encrypted fields (tax_number_hash)
- [ ] Tax number search works via hash-based lookup
- [ ] Existing plain text data migrated to encrypted form
- [ ] user_consents table created with Flyway migration
- [ ] POST /api/v1/users/me/consent — grant consent works
- [ ] DELETE /api/v1/users/me/consent — revoke consent works
- [ ] GET /api/v1/users/me/consent — query consent history works
- [ ] DATA_PROCESSING consent status tracked per user
- [ ] POST /api/v1/admin/kvkk/forget/{userId} — anonymizes user data
- [ ] Anonymized user: personal fields replaced with anonymized values
- [ ] Anonymized user: invoices preserved but personal references removed
- [ ] Anonymized user: audit logs show anonymized values
- [ ] Data retention scheduled job runs on configured schedule
- [ ] Retention job hard-deletes records past retention period
- [ ] Retention job respects dependency constraints (no dangling FK violations)
- [ ] GET /api/v1/admin/kvkk/report — returns compliance report with all sections
- [ ] Audit log masking: sensitive fields not logged in plain text
- [ ] Encryption key configurable via ENCRYPTION_SECRET_KEY environment variable
- [ ] All existing tests still pass (no regressions from encryption changes)
- [ ] All new tests pass
- [ ] Result file created at docs/OMER/step_results/faz_31.0_result.md

---
## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ AES-256-GCM encryption service is implemented and working
2. ✅ JPA AttributeConverter transparently encrypts/decrypts sensitive fields
3. ✅ Encrypted field column sizes are increased via migration
4. ✅ Hash columns are added for searchable encrypted fields (tax numbers)
5. ✅ Existing plain text data is migrated to encrypted form on startup
6. ✅ user_consents table is created with proper indexes
7. ✅ Consent API works: grant, revoke, query history
8. ✅ DATA_PROCESSING consent status is tracked per user
9. ✅ Right to be forgotten endpoint anonymizes user data correctly
10. ✅ Right to be forgotten preserves business records (invoices) while removing personal links
11. ✅ Audit logs for anonymized users show anonymized values
12. ✅ Data retention scheduled job runs and hard-deletes expired records
13. ✅ Retention job respects dependency constraints (does not delete users with active invoices under retention)
14. ✅ KVKK compliance report returns accurate data for all sections
15. ✅ Audit log masking prevents sensitive data from appearing in logs
16. ✅ Tax number search still works via hash-based lookup
17. ✅ Encryption key is configurable via environment variable
18. ✅ All tests pass
19. ✅ Result file is created at docs/OMER/step_results/faz_31.0_result.md

---

## IMPORTANT NOTES

1. **Encryption Key Management is Critical**: The encryption key is the most sensitive secret in the system. If lost, all encrypted data becomes unrecoverable. Document key backup procedures. In production, use a secrets manager (e.g., HashiCorp Vault). For development, the .env file is acceptable.

2. **Do Not Encrypt Everything**: Only encrypt fields that contain personal identifiable information (PII). Do not encrypt invoice amounts, dates, or status fields — these are business data, not personal data. Over-encryption hurts performance and makes debugging difficult.

3. **Searchability Trade-off**: Encrypting fields breaks SQL queries on those fields. The hash-based approach is a pragmatic trade-off: you can do exact match lookups (good for tax numbers) but NOT partial searches (LIKE queries). Document this limitation.

4. **Data Migration Must Be Idempotent**: The startup migration that encrypts existing data must be safe to run multiple times. If the application restarts, it should detect already-encrypted data and skip it.

5. **Performance Impact**: Encryption/decryption adds CPU overhead. For list endpoints returning many records, this could be noticeable. Monitor performance and consider caching decrypted values in memory if needed.

6. **Turkish Tax Law**: Turkish tax law (VUK — Vergi Usul Kanunu, Article 253) requires financial records to be kept for 5 years. This is why invoices have a 1825-day retention period. Do NOT allow hard deletion of invoices within this period, even via the "right to be forgotten" endpoint.

7. **Consent is Not Blocking (MVP)**: For this MVP, consent tracking is observational. The system records consent status but does not block operations if consent is missing (except logging warnings). A future phase could add enforcement.

8. **Coordinate with Furkan**: If a consent UI is needed in the frontend (e.g., consent dialog on first login), document the API contracts in the result file so Furkan can implement it in a future frontend update. For now, consent can be granted via API.

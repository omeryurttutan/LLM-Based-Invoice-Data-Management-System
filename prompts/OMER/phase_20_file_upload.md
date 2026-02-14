# PHASE 20: FILE UPLOAD INFRASTRUCTURE (SINGLE & BULK)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 - LLM-based extraction
  - **Next.js Frontend**: Port 3000
  - **RabbitMQ**: Port 5672

### Current State (Phases 0-19 Completed)
- ✅ Phase 0-9: Infrastructure, Docker Compose, CI/CD, Hexagonal Architecture, Database Schema, Auth (JWT), RBAC, Company/User API, Invoice CRUD API, Audit Log, Duplication Check
- ✅ Phase 10-12: Frontend layout, auth pages, invoice CRUD UI
- ✅ Phase 13-18 (FURKAN): FastAPI service, image preprocessing, Gemini + fallback chain (GPT/Claude), validation & confidence score, e-Invoice XML parser, file type detection (smart routing: image → LLM, XML → parser)
- ✅ Phase 19-B: RabbitMQ Producer — exchanges/queues/bindings declared, extraction request message schema defined, result listener with retry logic, invoice status lifecycle (QUEUED → PROCESSING → COMPLETED/FAILED), shared Docker volume (`invoice-files:/data/invoices`)
- ✅ Phase 19-A (FURKAN): RabbitMQ Consumer — Pika consumer on extraction queue, reads files from shared volume, routes to LLM or XML parser, publishes results back to result queue

### What Phase 19-B Delivered (Key for This Phase)
- **RabbitMQ Producer Service**: A service that accepts invoice metadata and publishes extraction request messages to the `invoice.extraction` exchange
- **Shared Volume**: `invoice-files:/data/invoices` mounted in both Spring Boot and Python containers
- **Status Lifecycle**: QUEUED → PROCESSING → COMPLETED → VERIFIED/PENDING or FAILED
- **Result Listener**: Automatically processes results from Python and updates invoices in the database
- **Message Schema**: Extraction request requires: invoice_id, file_path, file_name, file_type, file_size, company_id, user_id, correlation_id

### What Python Service Delivers (Phases 13-18)
- **POST /extract**: Synchronous HTTP endpoint — accepts a file, returns ExtractionResult (invoice_data, confidence_score, provider, validation_result)
- **RabbitMQ Consumer**: Asynchronous processing — reads files from shared volume, extracts, publishes results
- **Supports**: JPEG, PNG, PDF (images → LLM), XML (e-Invoice → XML parser)

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 3 days

---

## OBJECTIVE

Build the complete file upload infrastructure in Spring Boot that handles single and bulk invoice file uploads. Single uploads are processed synchronously (HTTP call to Python), while bulk uploads are queued for asynchronous processing (via RabbitMQ from Phase 19-B). The system validates files, saves them securely, creates invoice records, and triggers the appropriate extraction pipeline.

---

## TWO UPLOAD MODES

```
┌─────────────────────────────────────────────────────────────┐
│                    USER UPLOADS FILE(S)                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
                ┌─────────┴─────────┐
                │                   │
          SINGLE FILE          MULTIPLE FILES
          (1 file)             (2+ files or ZIP)
                │                   │
                ▼                   ▼
    ┌───────────────────┐  ┌───────────────────┐
    │  POST /upload     │  │ POST /bulk-upload  │
    │  (Synchronous)    │  │  (Asynchronous)    │
    └────────┬──────────┘  └────────┬──────────┘
             │                      │
             ▼                      ▼
    ┌───────────────────┐  ┌───────────────────┐
    │ Save file         │  │ Save all files    │
    │ Create invoice    │  │ Create invoices   │
    │ HTTP → Python     │  │ Queue via RabbitMQ│
    │ Wait for result   │  │ Return job ID     │
    │ Update invoice    │  │ (async processing)│
    │ Return result     │  │                   │
    └───────────────────┘  └───────────────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Supported File Formats

| Format | Extension | MIME Type | Max Size | Processing Route |
|---|---|---|---|---|
| JPEG | .jpg, .jpeg | image/jpeg | 10 MB | LLM extraction |
| PNG | .png | image/png | 10 MB | LLM extraction |
| PDF | .pdf | application/pdf | 20 MB | LLM extraction |
| GİB XML | .xml | application/xml, text/xml | 50 MB | XML parser |
| ZIP | .zip | application/zip | 100 MB | Extract → process each file |

### 2. File Validation

Before saving any file, validate:

**Format Validation:**
- Check file extension against allowed list
- Check MIME type (use content-type header AND file magic bytes detection)
- Reject files with mismatched extension/MIME type (e.g., .jpg file with application/pdf content)

**Size Validation:**
- Per-file size limits as defined above
- Total upload size limit for bulk: 200 MB
- ZIP archive: max 50 files inside

**Content Validation:**
- Verify file is not empty (0 bytes)
- For images: verify it's a valid image (attempt to read header)
- For XML: verify it starts with valid XML structure
- For ZIP: verify it's a valid ZIP archive, check for zip bombs (nested ZIPs or extremely high compression ratio)

**Security Validation:**
- Strip file path information (prevent path traversal attacks)
- Sanitize the original filename (remove special characters except .-_)
- Check for double extensions (.jpg.exe)
- Scan for null bytes in filename

### 3. Secure File Storage

**Storage Strategy:**
- Base path: `/data/invoices` (the shared Docker volume from Phase 19-B)
- Directory structure: `/{company_id}/{year}/{month}/`
- File naming: `{uuid}_{sanitized_original_name}`
- Example: `/data/invoices/5/2026/02/a1b2c3d4_fatura-001.pdf`

**Why this structure:**
- company_id isolation: each company's files in separate directories
- Year/month: prevents too many files in one directory
- UUID prefix: guarantees uniqueness, prevents overwrites
- Sanitized original name: preserves human-readability for debugging

**File metadata to store in the database (invoices table):**
- `original_file_name`: The user's original filename
- `stored_file_path`: The full path on the shared volume
- `file_size`: Size in bytes
- `file_type`: MIME type
- `file_hash`: SHA-256 hash of the file content (for deduplication — Phase 9)

### 4. Single File Upload Endpoint

**POST /api/v1/invoices/upload**

**Request:** Multipart form data with a single file field.

**Flow:**
1. Receive the file (MultipartFile)
2. Validate file (format, size, content, security)
3. Save file to shared volume with UUID naming
4. Create an invoice record in the database with status PROCESSING
5. Calculate file hash (SHA-256)
6. Check for duplicates using file hash (Phase 9 — call existing duplication check)
7. If duplicate found: return warning with the duplicate invoice ID (don't block, just warn)
8. Call the Python service synchronously: POST http://extraction-service:8000/extract with the file
9. Wait for the ExtractionResult response
10. Update the invoice record with the extracted data (invoice_number, dates, amounts, supplier, buyer, items)
11. Set confidence_score, llm_provider, source_type, suggested status
12. Set the final invoice status based on confidence (VERIFIED if auto_verify threshold met, else PENDING)
13. Return the complete invoice response to the frontend

**Response:** The full invoice entity (same format as GET /api/v1/invoices/{id}) with added extraction metadata (confidence_score, provider, validation_issues).

**Error Handling:**
- 400: Invalid file format or size
- 409: Duplicate file detected (optional: return duplicate info, allow override)
- 408: Python service timeout (extraction took too long)
- 502: Python service unavailable or returned error
- 500: Internal server error

**Timeout:** The synchronous HTTP call to Python has a timeout of 90 seconds (LLM extraction can take up to 30s per provider × 3 providers + preprocessing). Configurable via `EXTRACTION_HTTP_TIMEOUT_SECONDS`.

### 5. Bulk Upload Endpoint

**POST /api/v1/invoices/bulk-upload**

**Request:** Multipart form data with multiple file fields (or a single ZIP file).

**Flow:**
1. Receive all files
2. If ZIP file: extract contents to a temporary directory, then treat each extracted file as an individual upload
3. Validate each file individually (format, size, content, security)
4. Reject the entire batch if any file fails validation? NO — validate all, reject only the invalid ones, proceed with the valid ones
5. For each valid file:
   a. Save to shared volume
   b. Create invoice record with status QUEUED
   c. Calculate file hash
   d. Check duplicates (warn but don't block)
   e. Publish extraction request message to RabbitMQ (using Phase 19-B producer service)
6. Create a "batch job" record to track overall progress
7. Return the batch job ID and per-file status (accepted/rejected with reasons)

**Response:**

| Field | Type | Description |
|---|---|---|
| batch_id | string (UUID) | Unique identifier for this upload batch |
| total_files | integer | Total files received |
| accepted_files | integer | Files accepted for processing |
| rejected_files | integer | Files rejected due to validation |
| files | array | Per-file details (see below) |

Each file in the array:

| Field | Type | Description |
|---|---|---|
| file_name | string | Original filename |
| status | string | "ACCEPTED" or "REJECTED" |
| invoice_id | long | If accepted, the created invoice ID |
| rejection_reason | string | If rejected, why (e.g., "Invalid format", "Exceeds size limit") |

### 6. Batch Job Tracking

Create a mechanism to track bulk upload batch progress.

**Option A (Recommended): batch_jobs table**
Create a new table to track batch uploads:

| Column | Type | Description |
|---|---|---|
| id | BIGSERIAL PK | |
| batch_id | UUID UNIQUE | Public batch identifier |
| user_id | BIGINT FK | Who uploaded |
| company_id | BIGINT FK | Which company |
| total_files | INTEGER | Total files in batch |
| completed_files | INTEGER | Files extraction completed |
| failed_files | INTEGER | Files extraction failed |
| status | VARCHAR(20) | IN_PROGRESS / COMPLETED / PARTIALLY_COMPLETED / FAILED |
| created_at | TIMESTAMP | |
| completed_at | TIMESTAMP | When all files finished |

Also add `batch_id` column to the invoices table (nullable, only set for batch uploads) so individual invoices can be grouped by batch.

**Status tracking endpoints:**
- GET /api/v1/invoices/batch/{batch_id} — returns batch progress (total, completed, failed, status)
- GET /api/v1/invoices/batch/{batch_id}/files — returns all invoices in the batch with their current processing status

**Batch status update logic:**
When the result listener (Phase 19-B) receives a result for an invoice that belongs to a batch:
- Increment completed_files or failed_files on the batch_jobs record
- If completed_files + failed_files = total_files → update batch status to COMPLETED (or PARTIALLY_COMPLETED if any failed)

### 7. ZIP Archive Handling

**Requirements:**
- Accept .zip files in both single and bulk upload endpoints
- Extract ZIP contents to a temporary directory
- Validate each extracted file individually
- Skip unsupported files inside the ZIP (e.g., .txt, .doc) — log a warning, don't fail
- Nested ZIPs are NOT supported — skip inner .zip files
- Maximum files per ZIP: 50
- Zip bomb protection: reject if decompressed size exceeds 500 MB or compression ratio exceeds 100:1
- Clean up temporary directory after processing

### 8. Processing Status Endpoint

**GET /api/v1/invoices/{id}/status**

Returns the current processing status of a single invoice:

| Field | Type | Description |
|---|---|---|
| invoice_id | long | |
| status | string | QUEUED / PROCESSING / PENDING / VERIFIED / FAILED |
| confidence_score | float | If extraction completed |
| provider | string | Which LLM was used (if applicable) |
| source_type | string | LLM / E_INVOICE |
| batch_id | UUID | If part of a batch (null for single uploads) |
| processing_duration_ms | long | If completed |
| error_message | string | If failed |
| created_at | timestamp | |
| updated_at | timestamp | |

### 9. File Access Endpoint

**GET /api/v1/invoices/{id}/file**

Serves the original uploaded file for viewing in the frontend (Phase 22 verification UI needs this).

**Requirements:**
- Stream the file from the shared volume
- Set correct Content-Type header based on stored MIME type
- Set Content-Disposition to "inline" (for display) or "attachment" (for download) based on query param
- RBAC: Only users with access to the invoice's company can access the file
- Return 404 if file not found on storage

### 10. Database Migration

Create Flyway migration: `V{next_number}__phase_20_file_upload.sql`

**Changes needed:**
- Add `batch_jobs` table (as described in section 6)
- Add `batch_id UUID` column to `invoices` table (nullable, FK to batch_jobs)
- Add `original_file_name VARCHAR(255)` to `invoices` table (if not already present)
- Add `stored_file_path VARCHAR(500)` to `invoices` table (if not already present)
- Add `file_size BIGINT` to `invoices` table (if not already present)
- Add `file_type VARCHAR(100)` to `invoices` table (if not already present)
- Add `file_hash VARCHAR(64)` to `invoices` table (SHA-256 hash, for deduplication)

Check what already exists from Phase 3 — the invoices table may already have some of these columns. Only add what's missing.

### 11. Configuration — Environment Variables

- `UPLOAD_MAX_FILE_SIZE_MB`: Default 20 (per file)
- `UPLOAD_MAX_BULK_TOTAL_SIZE_MB`: Default 200
- `UPLOAD_MAX_ZIP_FILES`: Default 50
- `UPLOAD_MAX_ZIP_DECOMPRESSED_SIZE_MB`: Default 500
- `UPLOAD_ALLOWED_EXTENSIONS`: Default "jpg,jpeg,png,pdf,xml,zip"
- `UPLOAD_STORAGE_PATH`: Default "/data/invoices"
- `EXTRACTION_HTTP_TIMEOUT_SECONDS`: Default 90
- `EXTRACTION_SERVICE_URL`: Default "http://extraction-service:8000"

### 12. Logging

**INFO:**
- File upload received (filename, size, type, user_id)
- File saved to storage (stored_path, hash)
- Extraction triggered (mode: sync/async, invoice_id)
- Sync extraction completed (invoice_id, provider, confidence, duration)
- Batch created (batch_id, total_files, accepted, rejected)
- Batch completed (batch_id, completed, failed)

**WARNING:**
- File validation failed (filename, reason)
- Duplicate file detected (file_hash, existing_invoice_id)
- ZIP contains unsupported files (filenames)
- Extraction returned low confidence (invoice_id, score)

**ERROR:**
- File save failed (path, error)
- Python service unavailable (URL, error)
- Extraction timeout (invoice_id, timeout_seconds)
- ZIP extraction failed (filename, error)
- Batch tracking update failed (batch_id, error)

---

## TESTING REQUIREMENTS

### 1. Unit Tests for File Validation
- Test valid JPEG accepted
- Test valid PNG accepted
- Test valid PDF accepted
- Test valid XML accepted
- Test valid ZIP accepted
- Test invalid extension rejected (e.g., .exe, .doc)
- Test oversized file rejected
- Test empty file rejected
- Test MIME type mismatch rejected
- Test double extension rejected (.jpg.exe)
- Test filename sanitization (special characters removed)
- Test path traversal in filename blocked (../../etc/passwd)

### 2. Unit Tests for File Storage
- Test file saved with correct directory structure (company/year/month)
- Test UUID prefix uniqueness
- Test SHA-256 hash calculation
- Test file metadata stored correctly in invoice entity

### 3. Unit Tests for ZIP Handling
- Test valid ZIP extracted correctly
- Test ZIP with multiple valid files → all processed
- Test ZIP with unsupported files → unsupported skipped, valid processed
- Test nested ZIP rejected
- Test ZIP exceeding max files rejected
- Test zip bomb protection (high compression ratio)
- Test empty ZIP handled gracefully

### 4. Integration Tests
- Test POST /upload with valid image → invoice created, extraction called, result stored
- Test POST /upload with valid XML → invoice created, XML parsed, high confidence
- Test POST /upload with invalid file → 400 error
- Test POST /bulk-upload with multiple files → batch created, messages queued
- Test POST /bulk-upload with ZIP → extracted and queued
- Test GET /batch/{id} returns correct progress
- Test GET /invoices/{id}/file serves correct file with correct content type
- Test duplicate detection on same file uploaded twice

### 5. Integration Tests with Python Service (if available)
- Test full sync flow: upload image → Python extracts → invoice updated
- Test full async flow: bulk upload → RabbitMQ → Python processes → results received → invoices updated

---

## VERIFICATION CHECKLIST

### File Validation
- [ ] JPEG, PNG, PDF, XML, ZIP formats accepted
- [ ] Other formats rejected with clear error message
- [ ] Per-file size limits enforced
- [ ] MIME type checked (header + magic bytes)
- [ ] Empty files rejected
- [ ] Filename sanitized (special chars, path traversal)
- [ ] Double extensions blocked

### File Storage
- [ ] Files saved to shared volume at correct path
- [ ] Directory structure: /{company_id}/{year}/{month}/
- [ ] UUID prefix on filename
- [ ] SHA-256 hash calculated and stored
- [ ] Original filename preserved in database

### Single Upload
- [ ] POST /api/v1/invoices/upload accepts multipart file
- [ ] Invoice record created with status PROCESSING
- [ ] Python service called synchronously
- [ ] Invoice updated with extraction result
- [ ] Confidence score and provider stored
- [ ] Status set based on confidence threshold
- [ ] Duplicate warning returned if hash matches
- [ ] Timeout handled (90s default)

### Bulk Upload
- [ ] POST /api/v1/invoices/bulk-upload accepts multiple files
- [ ] ZIP files extracted and processed
- [ ] Each valid file saved and queued
- [ ] Invalid files rejected individually (not entire batch)
- [ ] Batch job record created
- [ ] RabbitMQ messages published for each file
- [ ] Per-file status returned in response

### Batch Tracking
- [ ] GET /api/v1/invoices/batch/{batch_id} returns progress
- [ ] GET /api/v1/invoices/batch/{batch_id}/files returns invoice list
- [ ] Batch status updated as results come in
- [ ] Batch marked COMPLETED when all files processed

### File Access
- [ ] GET /api/v1/invoices/{id}/file serves original file
- [ ] Correct Content-Type header set
- [ ] RBAC enforced (company access)
- [ ] 404 for missing files

### Database
- [ ] batch_jobs table created
- [ ] Necessary columns added to invoices table
- [ ] Flyway migration runs successfully

### Configuration
- [ ] All size limits configurable
- [ ] Storage path configurable
- [ ] Python service URL configurable
- [ ] Extraction timeout configurable

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_20_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified with full paths
4. Database migration details
5. Endpoint summary table (method, path, description, status)
6. Single upload test results (various file types, sync extraction)
7. Bulk upload test results (multiple files, ZIP, batch tracking)
8. File validation test results table
9. Batch tracking example (batch_id → progress → completion)
10. Test results (unit + integration)
11. Docker Compose changes (shared volume confirmation)
12. Issues encountered and solutions
13. Next steps (Phase 21 — Upload UI needs these endpoints)

---

## DEPENDENCIES

### Requires
- **Phase 7**: Invoice CRUD API (invoice entity, repository, service)
- **Phase 9**: Duplication Check (file hash-based dedup)
- **Phase 19-B**: RabbitMQ Producer (publishes extraction messages for bulk uploads)

### Required By
- **Phase 21**: Upload UI (FURKAN) — uses POST /upload and POST /bulk-upload endpoints
- **Phase 22**: Verification UI (FURKAN) — uses GET /invoices/{id}/file to display original document

---

## SUCCESS CRITERIA

1. ✅ Single file upload (POST /upload) works end-to-end: upload → save → extract → update invoice
2. ✅ Bulk upload (POST /bulk-upload) works: upload → save → queue → async processing
3. ✅ ZIP archive upload works: extract → validate → queue individual files
4. ✅ File validation catches all invalid/dangerous files
5. ✅ Files stored securely with UUID naming and correct directory structure
6. ✅ SHA-256 hash computed for duplicate detection
7. ✅ Batch job tracking works (progress, completion)
8. ✅ GET /invoices/{id}/file serves the original file correctly
9. ✅ RBAC enforced on all upload and file access endpoints
10. ✅ Database migration runs successfully
11. ✅ All tests pass
12. ✅ Result file created

---

## IMPORTANT NOTES

1. **Two Modes, One Output**: Both single and bulk upload ultimately produce the same invoice entity with the same fields. The only difference is sync (user waits) vs async (background processing).

2. **Shared Volume is Critical**: Both Spring Boot and Python must access the same files. The file_path stored in the database and sent in RabbitMQ messages must be valid inside BOTH containers. Use the shared Docker volume mount point: /data/invoices.

3. **Don't Call Python for Bulk**: For bulk uploads, do NOT call the Python HTTP endpoint. Use RabbitMQ (Phase 19-B producer). The Python consumer (Phase 19-A) will process them asynchronously.

4. **RBAC on Upload**: Users can only upload files for their own company. The company_id must come from the authenticated user's context, not from the request.

5. **Audit Log**: All uploads should be logged via the audit log system (Phase 8). Log who uploaded, when, what file, and the result.

6. **Duplication is a Warning, Not a Block**: If a file hash matches an existing invoice, warn the user but still allow the upload. The user may intentionally re-upload a corrected version or want a fresh extraction.

7. **Spring Boot MultipartFile Config**: Ensure Spring Boot's multipart configuration supports the max file sizes. Set `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` in application.yml.

8. **Cleanup on Failure**: If invoice creation fails after the file is saved, clean up the saved file. If RabbitMQ publishing fails after invoice creation, set status to FAILED and log.

9. **HttpClient for Python Service**: Use WebClient or RestTemplate (async-friendly) for the synchronous HTTP call to Python. Set appropriate timeouts. The URL should use Docker service name (extraction-service:8000).

10. **Batch Size Limit**: Consider a maximum batch size (e.g., 50 files per batch, same as ZIP limit). Very large batches could overwhelm the queue and storage.

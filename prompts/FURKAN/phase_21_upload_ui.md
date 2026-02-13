# PHASE 21: FRONTEND — FILE UPLOAD INTERFACE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-20 Completed)
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation, Shadcn/ui, responsive layout, dark/light mode
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor, token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, soft delete, skeleton loading, optimistic updates
- ✅ Phase 13-19 (Backend/Python): Full extraction pipeline — LLM fallback chain, validation, confidence score, XML parser, RabbitMQ async messaging
- ✅ Phase 20 (ÖMER): File Upload Infrastructure — POST /api/v1/invoices/upload (single, sync), POST /api/v1/invoices/bulk-upload (multi, async via RabbitMQ), ZIP support, file validation, secure storage (shared volume), batch job tracking (GET /batch/{id}), GET /invoices/{id}/file, GET /invoices/{id}/status

### What Phase 20 Delivers (Backend Endpoints for This Phase)

**Single Upload:**
- POST /api/v1/invoices/upload — multipart file, synchronous extraction, returns full invoice with confidence_score, provider, validation_issues
- Timeout: up to 90 seconds (LLM extraction can be slow)

**Bulk Upload:**
- POST /api/v1/invoices/bulk-upload — multiple files or ZIP, returns batch_id and per-file status (ACCEPTED/REJECTED)
- Async processing via RabbitMQ (background)

**Status Tracking:**
- GET /api/v1/invoices/batch/{batch_id} — batch progress (total_files, completed, failed, status)
- GET /api/v1/invoices/batch/{batch_id}/files — all invoices in batch with status
- GET /api/v1/invoices/{id}/status — single invoice processing status

**File Access:**
- GET /api/v1/invoices/{id}/file — serves original uploaded file

**File Validation Rules (enforced by backend, mirror on frontend for UX):**
- Allowed: .jpg, .jpeg, .png, .pdf, .xml, .zip
- Max per file: 20 MB (images/PDF), 50 MB (XML), 100 MB (ZIP)
- Max bulk total: 200 MB
- Max files in ZIP: 50

### Phase Assignment
- **Assigned To**: FURKAN (Frontend/AI Developer)
- **Estimated Duration**: 2-3 days

### Frontend Tech Stack (from Phase 10-12)
- Next.js 14+ (App Router)
- React 19
- TypeScript 5.x
- Tailwind CSS 3.x
- Shadcn/ui (component library)
- TanStack Query 5.x (server state)
- Zustand 4.x (client state)
- Axios (HTTP client with interceptor from Phase 11)

---

## OBJECTIVE

Build the complete file upload user interface for the invoice system. This includes a drag-and-drop upload zone, file picker, client-side validation, upload progress tracking, real-time batch processing status, and smart navigation to the extraction result after completion. The upload page is the primary entry point for getting invoices into the system.

---

## PAGE LOCATION AND ROUTING

- **Route**: `/invoices/upload`
- **Navigation**: Accessible from the sidebar (add "Fatura Yükle" / "Upload Invoice" menu item) and from a prominent "Yükle" button on the invoice list page (Phase 12)
- **Layout**: Uses the existing app layout (sidebar + header from Phase 10)

---

## DETAILED REQUIREMENTS

### 1. Upload Page Layout

The upload page has two main sections:

**Section A — Upload Zone (top)**
- Large drag-and-drop area occupying the main content area
- File picker button as alternative
- Clear visual cues for drag state (hover, active drop)
- Accepted file types displayed below the zone

**Section B — Upload Queue / Status (bottom)**
- Appears after files are selected/dropped
- Shows each file with its status
- For single uploads: shows extraction progress inline
- For bulk uploads: shows batch progress with per-file status

### 2. Drag-and-Drop Upload Zone

**Visual States:**

| State | Appearance |
|---|---|
| Default | Dashed border, cloud/upload icon, "Dosyalarınızı sürükleyip bırakın" text, "veya" divider, "Dosya Seç" button |
| Drag Over | Border turns to accent color (e.g., blue), background slightly highlighted, icon animates |
| Uploading | Zone becomes less prominent, status section becomes primary focus |
| Disabled | Greyed out (during active single upload) |

**Interaction:**
- Click anywhere in the zone → opens file picker dialog
- Drag files over → visual feedback
- Drop files → triggers validation and upload flow
- "Dosya Seç" button → opens file picker dialog
- Multiple file selection enabled in file picker

### 3. Client-Side File Validation

Validate files BEFORE sending to the backend (instant feedback, no network request):

| Check | Rule | Error Message (Turkish) |
|---|---|---|
| Format | .jpg, .jpeg, .png, .pdf, .xml, .zip only | "Desteklenmeyen dosya formatı. Desteklenen: JPG, PNG, PDF, XML, ZIP" |
| Size (image/PDF) | Max 20 MB per file | "Dosya boyutu 20 MB'ı aşamaz" |
| Size (XML) | Max 50 MB | "XML dosya boyutu 50 MB'ı aşamaz" |
| Size (ZIP) | Max 100 MB | "ZIP dosya boyutu 100 MB'ı aşamaz" |
| Total size (bulk) | Max 200 MB total | "Toplam dosya boyutu 200 MB'ı aşamaz" |
| Empty file | 0 bytes | "Boş dosya yüklenemez" |
| File count | Max 50 files at once | "En fazla 50 dosya yüklenebilir" |

Files that fail validation should be shown in the queue with a red error state and the specific error message. Valid files proceed.

### 4. Single File Upload Flow

When the user drops/selects exactly ONE file:

1. Client-side validation
2. Show file in the queue section with status "Yükleniyor..." and progress bar
3. Send POST /api/v1/invoices/upload as multipart form data
4. Show HTTP upload progress (Axios onUploadProgress) in the progress bar
5. Once upload completes (file sent to server), change status to "İşleniyor..." (extraction in progress on server)
6. Wait for the response (up to 90 seconds — show a spinner/animation)
7. On success: Show green checkmark, confidence score badge, provider badge (Gemini/GPT/Claude/XML), and a "Sonucu Görüntüle" (View Result) button
8. On click "Sonucu Görüntüle": navigate to `/invoices/{id}` (the invoice detail page) or directly to the verification page (Phase 22 route: `/invoices/{id}/verify`)
9. On failure: Show red error with the error message from the backend

**Progress States for Single Upload:**

| Stage | UI Indicator |
|---|---|
| Uploading file | Progress bar (0-100%, based on HTTP upload) |
| Server processing (LLM) | Indeterminate spinner + "Fatura analiz ediliyor..." text |
| Completed | Green check + confidence score + "Sonucu Görüntüle" button |
| Failed | Red X + error message + "Tekrar Dene" (Retry) button |

**Long Wait UX:**
Since LLM extraction can take 30-90 seconds:
- Show an animated indicator (pulsing dots, rotating icon, etc.)
- Show elapsed time counter: "İşlem süresi: 12s"
- Show an informational message: "LLM analizi devam ediyor, bu işlem 30-90 saniye sürebilir"
- Allow the user to navigate away — add a note: "Sayfadan ayrılabilirsiniz, işlem arka planda devam eder"

### 5. Bulk Upload Flow

When the user drops/selects MULTIPLE files (2 or more) OR a ZIP file:

1. Client-side validation for each file
2. Show all files in the queue section, each with individual validation status
3. Show a summary: "5 dosya seçildi (3 geçerli, 2 hatalı)" with the option to remove invalid ones
4. User clicks "Yüklemeyi Başlat" (Start Upload) button
5. Send POST /api/v1/invoices/bulk-upload with all valid files
6. Show HTTP upload progress for the entire batch
7. On response: receive batch_id and per-file status (ACCEPTED/REJECTED)
8. Transition to batch tracking mode

**Batch Tracking Mode:**
After the bulk upload API responds:
- Show each file with its current status from the backend
- Start polling GET /api/v1/invoices/batch/{batch_id} every 5 seconds
- Update the UI with progress: "İşlenen: 3/5, Başarılı: 2, Hatalı: 1"
- Show a progress bar for overall batch completion (completed + failed / total)
- For each individual file, show status: QUEUED → PROCESSING → COMPLETED / FAILED
- When a file completes, show its confidence score and a "Görüntüle" link

**Polling vs WebSocket:**
- Start with polling (GET /batch/{id} every 5 seconds) — simpler to implement
- Phase 27 will add WebSocket support for real-time updates — at that point, upgrade this polling to WebSocket
- Polling should stop when batch status is COMPLETED or PARTIALLY_COMPLETED

**Batch Summary (when all done):**
- "Toplu yükleme tamamlandı: 4 başarılı, 1 hatalı"
- List of all files with final status, confidence scores, and "Görüntüle" links
- "Fatura Listesine Git" button → navigate to invoice list (Phase 12)

### 6. File Queue / Status List Component

A reusable component that displays files in the upload queue.

**Per-file row displays:**

| Element | Description |
|---|---|
| File icon | Based on file type (image icon for jpg/png, PDF icon, XML icon, ZIP icon) |
| File name | Original filename (truncate if too long, tooltip for full name) |
| File size | Human-readable (e.g., "2.4 MB") |
| Status indicator | Color-coded badge: grey=waiting, blue=uploading, yellow=processing, green=completed, red=failed |
| Progress bar | Only during upload phase |
| Confidence score | After extraction completed (color-coded: green 90+, yellow 70-89, red <70) |
| Provider badge | After extraction: "Gemini" / "GPT" / "Claude" / "XML" small tag |
| Action button | "Görüntüle" (completed) / "Tekrar Dene" (failed) / "Kaldır" (before upload) |

**Remove file:** Before upload starts, user can click X to remove a file from the queue.
**Retry file:** After a failure, user can retry that specific file (single upload call).

### 7. Upload Mode Detection

The UI should automatically detect the upload mode based on user action:

| Action | Mode | API Endpoint |
|---|---|---|
| Drop/select 1 file (not ZIP) | Single (sync) | POST /upload |
| Drop/select 1 ZIP file | Bulk (async) | POST /bulk-upload |
| Drop/select 2+ files | Bulk (async) | POST /bulk-upload |

Show a subtle indicator to the user: "Tekli yükleme — sonuç hemen görüntülenir" or "Toplu yükleme — dosyalar arka planda işlenir"

### 8. State Management

Use Zustand for upload state (or keep it local in React state if it doesn't need to persist across pages):

**Upload State:**
- files: array of file objects with metadata and status
- uploadMode: "single" / "bulk" / null
- isUploading: boolean
- batchId: string (for bulk uploads)
- batchProgress: object (total, completed, failed)

**TanStack Query Usage:**
- Mutation for POST /upload (single)
- Mutation for POST /bulk-upload (bulk)
- Query for GET /batch/{batch_id} (polling with refetchInterval: 5000, enabled only when batchId is set and batch is not complete)
- Query for GET /invoices/{id}/status (if needed for individual status)

### 9. Error Handling

| Error Scenario | UI Response |
|---|---|
| Client validation fails | Red badge on file, error message below, file not uploaded |
| Upload HTTP error (network) | Toast notification: "Bağlantı hatası. Lütfen tekrar deneyin." + retry button |
| Backend returns 400 (invalid file) | Red badge on file with backend error message |
| Backend returns 409 (duplicate) | Yellow warning badge: "Bu dosya daha önce yüklenmiş. Devam etmek istiyor musunuz?" + Continue/Cancel |
| Backend returns 408/502 (timeout/unavailable) | Toast: "Sunucu yanıt vermiyor. Lütfen daha sonra tekrar deneyin." |
| Extraction fails (LLM error) | Red badge: "Veri çıkarım başarısız" + error detail + retry option |

### 9.1 Total LLM Failure Scenario (All Providers Failed)

When the backend returns a FAILED status with an error indicating that all LLM providers have been exhausted (AllProvidersFailedError), the UI must provide a richer experience than a generic error badge. This is a distinct scenario from a network error or a single-provider timeout.

**Detection:**
The backend's FAILED response (from GET /invoices/{id}/status or batch status) should include an `error_code` field. When `error_code === "ALL_PROVIDERS_FAILED"`, apply the special handling below instead of the generic error state.

**UI for Single Upload (Immediate Failure):**

When a single file upload returns with ALL_PROVIDERS_FAILED:

1. Show an expanded error card (not just a red badge) with:
   - Title: "Otomatik veri çıkarım başarısız" (Automatic extraction failed)
   - Explanation: "Tüm LLM sağlayıcıları denenmiş ancak veri çıkarılamadı." (All LLM providers were tried but extraction could not be completed.)
   - Provider attempt details (if available in the response): a small list showing which providers were tried and why they failed. Example:
     - ❌ Gemini: Zaman aşımı (Timeout)
     - ❌ GPT: API hatası (API Error 500)
     - ❌ Claude: Oran sınırı (Rate Limited)
   - If the backend does not return per-provider failure details, show: "3 farklı sağlayıcı denendi, hiçbiri başarılı olamadı."

2. Present two action buttons:
   - **"Tekrar Dene"** (Retry) — Re-triggers the extraction (same as existing retry). Useful if the failure was transient (rate limiting, temporary API outage).
   - **"Manuel Olarak Gir"** (Enter Manually) — Navigates to the invoice edit form (`/invoices/{id}/edit`) with the uploaded file still attached as the document but with all data fields empty, allowing the user to manually type the invoice data while viewing the document.

3. Below the buttons, show a subtle note: "İpucu: Dosya sisteme kaydedildi. Veri çıkarımı daha sonra tekrar denenebilir." (Tip: The file has been saved. Extraction can be retried later.)

**UI for Bulk Upload (Batch Failure):**

When one or more files in a batch fail with ALL_PROVIDERS_FAILED:

1. In the batch file list, show the failed file with:
   - Red badge: "Çıkarım Başarısız"
   - Expandable detail: same provider attempt information as above
   - Two action buttons per file: "Tekrar Dene" and "Manuel Gir"

2. At the batch summary level:
   - If ALL files failed: Show a banner: "Otomatik veri çıkarım servisi şu anda kullanılamıyor. Tüm dosyalar için manuel giriş yapabilirsiniz." with a link to the invoice list filtered to status=FAILED.
   - If SOME files failed: Show count: "3/10 dosya için veri çıkarılamadı" with an "expand" to see which ones.

**Navigation from "Manuel Olarak Gir":**
- Navigate to `/invoices/{id}/edit` (the existing invoice edit form from Phase 12)
- The invoice already exists in the database with status FAILED and the uploaded file attached
- The edit form should pre-populate with any partial data the backend may have extracted (if one provider returned partial results before another was tried)
- After the user saves the form, the invoice status should change from FAILED to PENDING (awaiting verification) or directly to VERIFIED if the user is an ADMIN/MANAGER

**Error Code Contract (coordinate with ÖMER):**
The backend should include the following in the invoice/status response when extraction fails:
- `error_code`: "ALL_PROVIDERS_FAILED"
- `error_details` (optional): Array of `{ provider: string, error: string, timestamp: string }`
- If this field is not available in the current Phase 20 API response, document it as a required backend enhancement and handle its absence gracefully (show generic message).

### 10. Accessibility and UX

- Keyboard accessible: Tab to upload zone, Enter/Space to open file picker
- Screen reader: Appropriate ARIA labels for the drop zone, status updates
- Touch-friendly: Large touch targets for mobile (the entire drop zone is a tap target)
- Responsive: Drop zone full width on mobile, constrained on desktop
- File type icons: Visually distinguishable icons for each format
- Turkish UI text: All labels, messages, tooltips in Turkish

### 11. Duplicate Warning Dialog

When the backend returns a duplicate warning (409):
- Show a Shadcn/ui AlertDialog
- Title: "Dosya Daha Önce Yüklenmiş"
- Body: "Bu dosya {date} tarihinde yüklenmiş ve {invoice_number} fatura numarasıyla kayıtlı. Yine de yeni bir çıkarım başlatmak istiyor musunuz?"
- Buttons: "İptal" (cancel) and "Devam Et" (continue — re-upload with override flag)

### 12. Navigation Integration

**From Invoice List (Phase 12):**
- Add a prominent "Fatura Yükle" button at the top of the invoice list page
- This button navigates to `/invoices/upload`

**From Sidebar (Phase 10):**
- Add "Fatura Yükle" as a menu item in the sidebar navigation (with upload icon)
- Position it prominently (near the top, after Dashboard)

**After Successful Upload:**
- Single: Navigate to invoice detail or verification page
- Bulk: Show "Fatura Listesine Git" or stay on page to monitor progress

---

## TESTING REQUIREMENTS

### 1. Component Tests
- Upload zone renders correctly in default state
- Drag-over visual state activates on dragenter
- File picker opens on click
- Files appear in queue after drop/selection
- Client validation catches invalid file types
- Client validation catches oversized files
- Client validation catches empty files
- Remove button removes file from queue before upload
- Upload mode correctly detected (single vs bulk)

### 2. Upload Flow Tests (with mocked API)
- Single upload: file sent → progress shown → result displayed
- Single upload: file sent → error → error displayed with retry
- Bulk upload: files sent → batch_id received → polling starts
- Bulk upload: polling updates progress correctly
- Bulk upload: polling stops when batch completes
- Duplicate warning dialog appears on 409 response
- Timeout handling shows appropriate message

### 3. Visual / Snapshot Tests
- Upload zone renders in all states (default, dragover, uploading, disabled)
- File queue item renders in all states (waiting, uploading, processing, completed, failed)
- Confidence score badge colors are correct (green/yellow/red)
- Responsive layout works on mobile viewport

---

## VERIFICATION CHECKLIST

### Upload Zone
- [ ] Drag-and-drop works (dragenter, dragleave, drop events)
- [ ] File picker opens on click and button click
- [ ] Visual feedback on drag over
- [ ] Multiple files can be dropped/selected
- [ ] Only allowed file types accepted in file picker (accept attribute)

### Client Validation
- [ ] Invalid formats rejected with Turkish error message
- [ ] Oversized files rejected with limit shown
- [ ] Empty files rejected
- [ ] File count limit enforced (50)
- [ ] Total size limit enforced for bulk (200 MB)

### Single Upload
- [ ] HTTP upload progress bar works (0-100%)
- [ ] "İşleniyor..." state shown during server extraction
- [ ] Elapsed time counter displayed during processing
- [ ] Confidence score shown on completion
- [ ] Provider badge shown (Gemini/GPT/Claude/XML)
- [ ] "Sonucu Görüntüle" navigates to invoice detail
- [ ] Error state with retry button works
- [ ] Navigate-away note shown during processing

### Bulk Upload
- [ ] Multiple files or ZIP triggers bulk mode
- [ ] "Yüklemeyi Başlat" button present before upload
- [ ] Batch ID received and polling starts
- [ ] Per-file status updates during polling
- [ ] Overall progress bar updates
- [ ] Polling stops on batch completion
- [ ] Batch summary shown when all done
- [ ] "Fatura Listesine Git" navigation works

### Error Handling
- [ ] Network error toast shown
- [ ] Backend validation error shown per file
- [ ] Duplicate warning dialog works
- [ ] Timeout message shown
- [ ] Extraction failure shown with retry

### UX/Accessibility
- [ ] Keyboard navigation works
- [ ] Responsive on mobile
- [ ] Turkish text for all user-facing strings
- [ ] File type icons correct
- [ ] Loading skeletons/spinners appropriate

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_21_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified with full paths
4. Component list (each component, its purpose, location)
5. Screenshots of key states: default upload zone, drag-over, file queue with mixed statuses, single upload processing, bulk upload batch tracking, completion state, error state
6. State management approach (Zustand store or local state)
7. API integration details (endpoints used, TanStack Query configuration)
8. Polling implementation details (interval, stop condition)
9. Test results (component tests, flow tests)
10. Accessibility notes
11. Issues encountered and solutions
12. Next steps (Phase 22 — Verification UI will link from this page's "Sonucu Görüntüle" button)

---

## DEPENDENCIES

### Requires
- **Phase 10**: App layout, sidebar navigation, Shadcn/ui setup
- **Phase 11**: Auth store, Axios interceptor, protected routes
- **Phase 12**: Invoice list page (navigation integration, TanStack Query patterns)
- **Phase 20**: Backend upload endpoints (POST /upload, POST /bulk-upload, GET /batch/{id}, GET /invoices/{id}/status)

### Required By
- **Phase 22**: LLM Verification UI — uses "Sonucu Görüntüle" navigation from this page, GET /invoices/{id}/file for original document display

---

## SUCCESS CRITERIA

1. ✅ Drag-and-drop upload zone works with visual feedback
2. ✅ File picker alternative works
3. ✅ Client-side validation catches invalid files with Turkish error messages
4. ✅ Single file upload shows progress, processing state, and result (confidence + provider)
5. ✅ Bulk upload shows batch progress with per-file status updates
6. ✅ Polling updates batch status every 5 seconds until completion
7. ✅ Navigation to invoice detail/verification after successful extraction
8. ✅ Error handling covers network errors, validation errors, timeouts, duplicates
9. ✅ Duplicate warning dialog works with continue/cancel
10. ✅ Responsive design works on mobile and desktop
11. ✅ All user-facing text is in Turkish
12. ✅ Sidebar and invoice list have "Fatura Yükle" navigation
13. ✅ All tests pass
14. ✅ Result file created with screenshots
15. ✅ ALL_PROVIDERS_FAILED scenario shows expanded error card with provider details
16. ✅ "Manuel Olarak Gir" button navigates to edit form with file attached
17. ✅ Batch failure shows per-file and summary-level failure information

---

## IMPORTANT NOTES

1. **Long Processing Wait**: Single upload can take 30-90 seconds for LLM extraction. The UI MUST handle this gracefully — never leave the user staring at a blank screen. Show elapsed time, informational messages, and the option to navigate away.

2. **Polling is Temporary**: The 5-second polling for batch status will be replaced by WebSocket real-time updates in Phase 27. Design the polling logic so it can be easily swapped out later (use a custom hook like `useBatchStatus(batchId)` that internally switches from polling to WebSocket).

3. **Turkish UI**: ALL user-facing text must be in Turkish. Error messages, button labels, status text, informational messages — everything. English is only for variable names and code comments.

4. **Shadcn/ui Components**: Use existing Shadcn/ui components wherever possible: Button, Card, Progress, Badge, AlertDialog, Toast, Skeleton, Tooltip. Don't build custom UI from scratch.

5. **TanStack Query Patterns**: Follow the same patterns established in Phase 12. Use mutations for uploads, queries for status polling. Invalidate the invoice list query cache after a successful upload so the list auto-refreshes.

6. **File Size Display**: Use human-readable file sizes (KB, MB). Helper: `formatFileSize(bytes)`.

7. **Optimistic UI**: After clicking "Yüklemeyi Başlat", immediately show files as "uploading" — don't wait for the request to start.

8. **ZIP File UX**: When a user drops a ZIP, show it as a single item initially with a "ZIP arşivi — içerik sunucuda açılacak" note. After the backend responds, show the individual files from the ZIP.

9. **Abort Upload**: Consider adding a "Cancel" button during upload that aborts the HTTP request (Axios cancel token / AbortController). Not mandatory but nice-to-have.

10. **No Backend Changes**: This phase is frontend-only. All backend endpoints are ready from Phase 20. If something is missing from the backend, coordinate with Ömer.

# PHASE 22: FRONTEND — LLM RESULT VERIFICATION UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-21 Completed)
- ✅ Phase 0-3: Docker environment, CI/CD, Hexagonal Architecture, Database schema (companies, users, invoices, invoice_items, categories, audit_logs, batch_jobs — Flyway migrations, soft delete, indexes)
- ✅ Phase 4-6: JWT Auth (access/refresh tokens, Redis, BCrypt), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Company & User Management
- ✅ Phase 7: Invoice CRUD API (items, categories, status workflow: PENDING → VERIFIED / REJECTED, company-scoped, pagination, sorting)
- ✅ Phase 8-9: Audit Log (immutable), Duplication Control (invoice_number + company_id)
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation, Shadcn/ui, responsive layout, dark/light mode
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor, token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, soft delete, skeleton loading, optimistic updates
- ✅ Phase 13: Python FastAPI service setup with health endpoints, structured logging, error handling, CORS, Docker configuration, LLM API key env variables, Spring Boot ↔ FastAPI connectivity
- ✅ Phase 14: Image preprocessing pipeline (Pillow + PyMuPDF) — format detection, PDF conversion, orientation fix, deskew, contrast/brightness/sharpness enhancement, size optimization, base64 encoding
- ✅ Phase 15: Gemini 3 Flash integration — abstract base provider (Strategy Pattern), Gemini client, extraction prompt (versioned, Turkish-specific JSON schema), response parser, InvoiceData Pydantic model, ExtractionResult model, custom error hierarchy, extraction orchestrator, POST /extract endpoints
- ✅ Phase 16: LLM Fallback Chain — GPT-5.2 provider (OpenAI SDK), Claude Haiku 4.5 provider (Anthropic SDK), fallback chain manager (Gemini → GPT → Claude, 2s delay, sequential cascade), provider health tracking (HEALTHY/DEGRADED/UNHEALTHY), prompt adaptation per provider, response normalization, AllProvidersFailedError, GET /providers endpoints
- ✅ Phase 17: LLM Response Validation & Confidence Score — 5 validation categories (field completeness, format, math consistency, value range, cross-field logic), weighted confidence score (0-100), suggested status (AUTO_VERIFIED / NEEDS_REVIEW / LOW_CONFIDENCE), configurable thresholds, POST /validate endpoint for re-validating edited data, field-level validation issues with severity (CRITICAL/WARNING/INFO)
- ✅ Phase 18: E-Invoice XML Parser — UBL 2.1 / TR 1.2 parsing, same InvoiceData model output, source_type distinction (LLM vs E_INVOICE vs MANUAL)
- ✅ Phase 19-A: RabbitMQ Consumer — async invoice processing from queue
- ✅ Phase 19-B: RabbitMQ Producer — Spring Boot message publishing for bulk uploads
- ✅ Phase 20: File Upload Infrastructure (Backend/ÖMER) — POST /api/v1/invoices/upload (single, sync), POST /api/v1/invoices/bulk-upload (multi, async via RabbitMQ), ZIP support, file validation, secure storage (shared volume), batch job tracking, GET /invoices/{id}/file, GET /invoices/{id}/status, database migration for batch_jobs and file columns
- ✅ Phase 21: File Upload UI (Frontend/FURKAN) — drag-and-drop upload zone, file picker, client-side validation, upload progress bar, single upload flow (progress → processing → result), bulk upload flow (batch tracking, per-file status, polling), "Sonucu Görüntüle" button navigation to verification page, Turkish UI text

### Phase Assignment
- **Assigned To**: FURKAN (Frontend/AI Developer)
- **Estimated Duration**: 3-4 days

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

Build the LLM extraction result verification interface — the critical "human-in-the-loop" step where users review, correct, and approve extracted invoice data. This page shows the original invoice document side-by-side with the extracted data, highlights low-confidence fields, allows inline editing, and supports verify/reject actions. This is where data quality is ensured before invoices enter the final system.

**This is the most important user-facing feature of the entire system** — it bridges LLM automation with human validation and is the primary interaction point for accountants using the system daily.

---

## AVAILABLE BACKEND ENDPOINTS (Already Implemented)

The following endpoints are available from previous phases. This phase is frontend-only — no backend changes needed unless noted.

### Invoice Data
- **GET /api/v1/invoices/{id}** — Full invoice details including: invoice_number, invoice_date, due_date, supplier_name, supplier_tax_number, supplier_address, buyer_name, buyer_tax_number, items[] (description, quantity, unit, unit_price, tax_rate, tax_amount, line_total), subtotal, tax_amount, total_amount, currency, notes, status, source_type, llm_provider, confidence_score, validation_issues[], created_at, updated_at
- **PUT /api/v1/invoices/{id}** — Update invoice fields (used for saving corrections)
- **PATCH /api/v1/invoices/{id}/status** — Update invoice status (PENDING → VERIFIED or REJECTED)

### Original Document
- **GET /api/v1/invoices/{id}/file** — Serves the original uploaded file (image/PDF/XML) with correct Content-Type header

### Re-validation (Python Service via Backend Proxy)
- **POST /api/v1/extraction/validate** — Accepts edited InvoiceData JSON, returns fresh ValidationResult with updated confidence_score and validation_issues. This endpoint proxies to the Python service's POST /validate (Phase 17). Use this to re-validate after user edits without re-running LLM extraction.

### Invoice Status Values
- **PROCESSING** — Extraction in progress (should not reach verification UI)
- **PENDING** — Awaiting human review (primary state for this UI)
- **VERIFIED** — Approved by user
- **REJECTED** — Rejected by user

---

## DETAILED REQUIREMENTS

### 1. Route and Navigation

**Page Route:** `/invoices/{id}/verify`

**Navigation Entry Points:**
- From Phase 21 upload UI: "Sonucu Görüntüle" button after successful extraction
- From Phase 12 invoice list: Click on any PENDING invoice row
- From Phase 12 invoice detail page: "Doğrula" (Verify) button for PENDING invoices
- Direct URL access (with auth check)

**Route Protection:**
- Must be authenticated (redirect to login if not)
- Must have access to the invoice's company (multi-tenant check)
- Invoice must exist (show 404 page if not found)
- If invoice status is already VERIFIED or REJECTED, show the page in read-only mode with a banner indicating the status

**Page Title:** "Fatura Doğrulama — {invoice_number}" (use the invoice number in the browser tab title)

### 2. Split-View Layout

The page must display a two-panel layout:

**Left Panel — Original Document Viewer (approximately 50% width):**
- Fetch the original document from GET /api/v1/invoices/{id}/file
- Display based on file type:
  - For images (JPEG, PNG): Render directly in an image viewer component
  - For PDFs: Use a PDF viewer (react-pdf or pdf.js-based component) with page navigation if multi-page
  - For XML (e-Invoice): Show a formatted, syntax-highlighted XML preview (or a rendered summary of key fields)
- **Zoom controls**: Zoom in (+), zoom out (-), fit-to-width, fit-to-page buttons
- **Pan support**: When zoomed in, the user can click-and-drag to pan around the document
- **Scroll sync hint**: The document panel scrolls independently from the data panel
- A small toolbar at the top of the panel with: zoom controls, current zoom percentage, file name, file type badge

**Right Panel — Extracted Data Form (approximately 50% width):**
- Displays all extracted invoice fields in an editable form layout
- Organized in logical sections (details below in Section 3)
- Each field shows its confidence indicator
- Inline editing enabled for all fields
- Action buttons at the bottom

**Divider:**
- A draggable divider between left and right panels that allows resizing (optional enhancement — if too complex, use fixed 50/50 split)
- On mobile/small screens: Stack panels vertically (document on top, data below) with a toggle to switch between them

### 3. Extracted Data Form — Field Layout

Organize the extracted fields into clear sections:

**Section A — Genel Bilgiler (General Information):**
- Fatura Numarası (invoice_number) — text input
- Fatura Tarihi (invoice_date) — date picker (format: DD.MM.YYYY display, YYYY-MM-DD storage)
- Vade Tarihi (due_date) — date picker (nullable)
- Para Birimi (currency) — select dropdown (TRY, USD, EUR, GBP)
- Kaynak (source_type) — read-only badge (LLM / E_INVOICE / MANUAL)
- LLM Sağlayıcı (llm_provider) — read-only badge (Gemini / GPT / Claude) — only show if source_type is LLM

**Section B — Tedarikçi Bilgileri (Supplier Information):**
- Tedarikçi Adı (supplier_name) — text input
- Vergi Numarası (supplier_tax_number) — text input with format hint (10 or 11 digits)
- Tedarikçi Adresi (supplier_address) — textarea (nullable)

**Section C — Alıcı Bilgileri (Buyer Information):**
- Alıcı Adı (buyer_name) — text input (nullable)
- Alıcı Vergi Numarası (buyer_tax_number) — text input (nullable)

**Section D — Fatura Kalemleri (Invoice Items):**
- A dynamic table/list showing all items
- Each item row contains: Açıklama (description), Miktar (quantity — number), Birim (unit — text), Birim Fiyat (unit_price — number), KDV Oranı (tax_rate — number/%), KDV Tutarı (tax_amount — calculated/editable number), Satır Toplamı (line_total — calculated/editable number)
- **Add item button**: "Kalem Ekle" — adds a new empty row
- **Remove item button**: Trash icon on each row to remove an item
- **Auto-calculation**: When quantity, unit_price, or tax_rate changes, auto-calculate tax_amount and line_total. Show a subtle animation when values update.
- Responsive: On small screens, display items as cards instead of a table

**Section E — Tutarlar (Amounts):**
- Ara Toplam (subtotal) — number input (KDV hariç)
- KDV Tutarı (tax_amount) — number input
- Genel Toplam (total_amount) — number input (KDV dahil)
- **Consistency check**: Show a warning indicator if subtotal + tax_amount does not equal total_amount (with a tolerance of 0.05 TRY). This is a local client-side check — the backend re-validation will do the full check.

**Section F — Notlar (Notes):**
- Notlar (notes) — textarea (nullable)

### 4. Confidence Score Display

**Overall Confidence Score:**
- Display prominently at the top of the right panel, next to the invoice number
- Show as a colored badge/indicator:
  - 90-100: Green badge — "Yüksek Güven" (High Confidence)
  - 70-89: Yellow/amber badge — "Orta Güven" (Medium Confidence)
  - 0-69: Red badge — "Düşük Güven" (Low Confidence)
- Show the numeric score (e.g., "85/100")

**Field-Level Confidence Indicators:**
- Each editable field should have a small colored dot or icon next to it indicating its confidence status
- The validation_issues array from the backend contains field-level issues with severity:
  - **CRITICAL** (red dot): This field has a serious issue (e.g., missing required field, format error)
  - **WARNING** (yellow dot): This field may need review (e.g., unusual value, minor inconsistency)
  - **INFO** (blue dot): Informational note (e.g., default value used)
  - **No issue** (green dot or no dot): Field looks good
- When the user hovers over or clicks a confidence indicator, show a tooltip/popover with the specific issue description from validation_issues
- Low-confidence fields (CRITICAL and WARNING) should have a subtle background highlight (light red or light yellow) to draw attention

### 5. LLM Provider Badge

- Show which LLM provider extracted this data: "Gemini", "GPT", or "Claude"
- Display as a small badge near the overall confidence score
- Use distinct colors or icons for each provider for quick visual identification
- Only display this badge when source_type is "LLM" — for E_INVOICE and MANUAL sources, show the corresponding badge instead

### 6. Inline Editing

- All data fields in the right panel are editable by default when the invoice status is PENDING
- Use controlled form inputs (React state or form library like react-hook-form)
- Track which fields have been modified by the user (dirty tracking)
- Modified fields should show a small "Değiştirildi" (Modified) indicator or a subtle visual change (e.g., a left border highlight)
- Allow resetting a field to its original extracted value (small "Geri Al" / undo icon per field)

**Form Validation (client-side):**
- invoice_number: required, non-empty
- invoice_date: required, valid date
- supplier_name: required, non-empty
- supplier_tax_number: if provided, must be 10 or 11 digits
- buyer_tax_number: if provided, must be 10 or 11 digits
- quantity, unit_price, tax_rate, tax_amount, line_total: must be non-negative numbers
- subtotal, tax_amount, total_amount: must be non-negative numbers
- Show inline validation error messages in Turkish

### 7. Re-validation After Edits

When the user modifies fields and wants to check the updated data quality before final approval:

- Provide a "Yeniden Doğrula" (Re-validate) button
- When clicked, collect the current form data into the InvoiceData JSON format
- Send POST /api/v1/extraction/validate with the edited data
- Show a loading spinner on the button while waiting
- On response: Update the confidence score display and field-level indicators with the new validation results
- This allows users to fix issues and see their confidence score improve in real-time before final verification

### 8. Action Buttons

Place action buttons at the bottom of the right panel (or in a sticky footer bar):

**Primary Action — "Onayla ve Kaydet" (Verify and Save):**
- Saves all modified fields via PUT /api/v1/invoices/{id}
- Then updates status to VERIFIED via PATCH /api/v1/invoices/{id}/status
- Show a confirmation dialog before executing: "Bu faturayı onaylamak istediğinize emin misiniz? Onaylanan faturalar düzenleme için kilitlenir."
- On success: Show a success toast notification, navigate to invoice list or next pending invoice
- Button color: Green/primary

**Secondary Action — "Reddet" (Reject):**
- Show a dialog asking for a rejection reason (optional text input)
- Updates status to REJECTED via PATCH /api/v1/invoices/{id}/status
- On success: Show a notification, navigate to invoice list
- Button color: Red/destructive

**Tertiary Action — "Kaydet" (Save Draft):**
- Saves all modified fields via PUT /api/v1/invoices/{id} WITHOUT changing status
- Keeps the invoice in PENDING status for later review
- Useful when the user wants to save partial corrections and return later
- Show a success toast on completion
- Button style: Outline/secondary

**Navigation — "İptal" (Cancel):**
- Navigate back to the previous page (invoice list or detail)
- If there are unsaved changes, show a confirmation dialog: "Kaydedilmemiş değişiklikler var. Sayfadan ayrılmak istediğinize emin misiniz?"

### 9. Keyboard Shortcuts

Implement keyboard shortcuts for efficient workflow (accountants processing many invoices):

- **Tab**: Move focus to the next editable field
- **Shift+Tab**: Move focus to the previous editable field
- **Ctrl+Enter** (or Cmd+Enter on Mac): Trigger "Onayla ve Kaydet" (Verify and Save)
- **Ctrl+S** (or Cmd+S): Trigger "Kaydet" (Save Draft)
- **Escape**: Cancel / close any open dialog

Display a small keyboard shortcut hint at the bottom of the page or in a help tooltip: "Kısayollar: Tab (sonraki alan), Ctrl+Enter (onayla), Ctrl+S (kaydet)"

### 10. Correction Tracking for Prompt Optimization

When the user makes corrections to extracted data, this feedback is valuable for future prompt improvement. Implement a mechanism to track changes:

- When saving (Verify or Save Draft), compare the original extracted values with the user's edited values
- Build a "corrections" object that records: { field_name, original_value, corrected_value } for each modified field
- Include this corrections object in the PUT request body as an additional field (the backend should have an `extraction_corrections` JSONB column or a separate tracking table — if not, note this as a database migration requirement)
- This data will be used in a future phase (Phase 30) for template learning and prompt optimization

**Important**: If the backend does not yet have a column or endpoint to accept correction data, document this in the result file as a required database migration. Do NOT skip the frontend implementation — store the corrections in the request payload, and the backend team will add support.
---
### 10.1 Failed Extraction — Manual Entry Redirect

When a user navigates to an invoice that has status FAILED (extraction failed, typically ALL_PROVIDERS_FAILED):

- The verification page should NOT show the normal side-by-side verification layout (there is no extracted data to verify)
- Instead, show a dedicated "manual entry" state:
  1. Display the original document on the left side (same document viewer as normal verification)
  2. On the right side, show an empty invoice form (same fields as the verification form, but all blank or with partial data if available)
  3. Show an info banner at the top: "Otomatik veri çıkarım başarısız oldu. Lütfen belgeyi inceleyerek verileri manuel olarak girin." (Automatic extraction failed. Please review the document and enter data manually.)
  4. The user fills in all fields manually while viewing the document
  5. "Kaydet ve Doğrula" (Save and Verify) button saves the data and sets status to VERIFIED
  6. "Taslak Kaydet" (Save Draft) saves the data and sets status to PENDING (for another user to verify later)

This ensures that even when the LLM pipeline completely fails, users have a clear workflow to process the invoice — preserving the core value of the system (document viewer + data entry side by side).
---

### 11. Read-Only Mode

When the invoice status is VERIFIED or REJECTED:

- Show all fields as read-only (disabled inputs or plain text display)
- Hide the action buttons (Verify, Reject, Save Draft)
- Show a status banner at the top:
  - VERIFIED: Green banner — "Bu fatura onaylanmıştır" with the verification date and user
  - REJECTED: Red banner — "Bu fatura reddedilmiştir" with the rejection date, user, and reason (if available)
- Still show the original document in the left panel for reference
- Still show confidence score and validation indicators for informational purposes

### 12. Loading and Error States

**Initial Loading:**
- Show skeleton loaders for both panels while data is being fetched
- Fetch invoice data and document file in parallel (Promise.all or TanStack Query parallel queries)

**Document Loading:**
- Show a loading spinner in the left panel while the document is being loaded
- If the document fails to load: Show an error message with a "Tekrar Dene" (Retry) button
- If no document is attached to the invoice: Show a placeholder message — "Orijinal belge bulunamadı"

**Save Errors:**
- If PUT /api/v1/invoices/{id} fails: Show an error toast with the error message, keep the form data intact
- If PATCH /api/v1/invoices/{id}/status fails: Show an error toast, do NOT navigate away
- Network errors: Show a generic error toast — "Bağlantı hatası. Lütfen tekrar deneyin."

**Validation Errors (from backend):**
- If the backend returns 400/422 with field-specific errors, map them to the corresponding form fields and show inline error messages

### 13. Responsive Design

- **Desktop (>= 1024px)**: Side-by-side split view (50/50 or resizable)
- **Tablet (768px - 1023px)**: Side-by-side with narrower panels, or toggle between panels
- **Mobile (< 768px)**: Stacked layout — show a tab switcher at the top: "Belge" (Document) | "Veriler" (Data) — only one panel visible at a time

### 14. Unsaved Changes Guard

Implement a navigation guard to prevent accidental data loss:

- Track form dirty state (whether any field has been modified since last save or page load)
- On browser back button, sidebar navigation click, or any route change attempt:
  - If dirty: Show a confirmation dialog — "Kaydedilmemiş değişiklikler var. Sayfadan ayrılmak istediğinize emin misiniz?"
  - If not dirty: Navigate normally
- Also handle browser tab close / page refresh with beforeunload event

---

## TESTING REQUIREMENTS

### 1. Component Tests

Write tests using Jest and React Testing Library:

- Split-view layout renders both panels
- Image document renders correctly in viewer
- PDF document renders correctly
- Form fields are populated from API data
- Confidence score badge shows correct color based on score range
- Field-level validation indicators display correctly
- Inline editing works (type in field, verify form state updates)
- Modified field tracking works (shows "Değiştirildi" indicator)
- Re-validation button sends correct payload and updates display
- Verify button flow: confirmation dialog → API call → navigation
- Reject button flow: reason dialog → API call → navigation
- Save Draft button: saves without status change
- Cancel with dirty form shows confirmation dialog
- Read-only mode for VERIFIED/REJECTED invoices
- Item add/remove in invoice items section
- Auto-calculation of tax_amount and line_total
- Amount consistency warning displays correctly
- Keyboard shortcuts work (Tab navigation, Ctrl+Enter)
- Responsive layout: verify mobile tab switcher works
- Loading skeleton displays during data fetch
- Error states display correctly (document load failure, save failure)

### 2. API Integration Tests

- Mock GET /api/v1/invoices/{id} and verify form population
- Mock GET /api/v1/invoices/{id}/file and verify document display
- Mock PUT /api/v1/invoices/{id} and verify save payload includes corrections
- Mock PATCH /api/v1/invoices/{id}/status with VERIFIED and REJECTED
- Mock POST /api/v1/extraction/validate and verify re-validation flow
- Test error responses (404, 400, 500) and verify error handling

---

## RESULT FILE

When this phase is complete, create a result file at:

**`docs/FURKAN/step_results/faz_22.0_result.md`**

The result file must contain:

### 1. Execution Summary
- Phase number, assigned developer, start/end dates
- Execution status (COMPLETED / PARTIAL / BLOCKED)
- Total time spent

### 2. Completed Tasks Checklist
Go through each requirement (1-14) and mark as completed or not:
- [ ] Route and navigation
- [ ] Split-view layout (document viewer + data form)
- [ ] Extracted data form with all sections (A-F)
- [ ] Confidence score display (overall + field-level)
- [ ] LLM provider badge
- [ ] Inline editing with dirty tracking
- [ ] Re-validation after edits
- [ ] Action buttons (Verify, Reject, Save Draft, Cancel)
- [ ] Keyboard shortcuts
- [ ] Correction tracking
- [ ] Read-only mode for verified/rejected invoices
- [ ] Loading and error states
- [ ] Responsive design
- [ ] Unsaved changes guard

### 3. Files Created/Modified
List every file with its full path and a brief description of what it does.

### 4. Component Architecture
Describe the component hierarchy:
- Page component
- Layout components (split view, panels)
- Form components (sections, fields)
- Viewer components (image, PDF, XML)
- UI components (confidence badge, provider badge, action bar)
- Custom hooks (useInvoiceVerification, useFormDirtyTracking, etc.)

### 5. State Management
- What state is managed by TanStack Query (server state)
- What state is managed by local React state or Zustand (form state, UI state)
- How dirty tracking works
- How correction tracking works

### 6. Screenshots / UI Description
For each key state, describe or screenshot:
- Default verification view (document + data side by side)
- Field with low confidence highlighted
- Modified field with "Değiştirildi" indicator
- Re-validation in progress
- Verify confirmation dialog
- Reject reason dialog
- Read-only mode (verified invoice)
- Mobile responsive view
- Loading skeleton state
- Error state (document load failure)

### 7. Test Results
- Unit/component test output summary
- Number of tests passed/failed

### 8. Database Changes
- List any new migration files created (e.g., extraction_corrections column)
- Or confirm "No database changes needed for this phase"
- If a migration is needed but could not be created (backend team responsibility), document the requirement clearly

### 9. Issues Encountered
Problems discovered during implementation and their solutions

### 10. Performance Notes
- Document load time observations
- Form re-render optimization notes (if applicable)
- Any lazy loading or code splitting applied

### 11. Accessibility Notes
- Screen reader support for confidence indicators
- Keyboard navigation completeness
- ARIA labels on custom components
- Color contrast compliance for confidence badges

### 12. Next Steps
- What Phase 23 (Advanced Filtering) needs from this phase
- What Phase 26 (Dashboard) might reference from this page
- Any UI improvements identified for future iteration
- Any backend changes needed that were deferred

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 10**: App layout, sidebar navigation, Shadcn/ui setup, dark/light mode
- **Phase 11**: Auth store, Axios interceptor, protected routes, token refresh
- **Phase 12**: Invoice CRUD UI patterns (TanStack Query usage, form patterns, status badges)
- **Phase 7**: Invoice CRUD API (GET, PUT, PATCH endpoints)
- **Phase 17**: Validation pipeline — provides confidence_score, validation_issues in invoice response, and POST /validate endpoint for re-validation
- **Phase 20**: File upload backend — provides GET /invoices/{id}/file endpoint for serving original documents
- **Phase 21**: Upload UI — provides the "Sonucu Görüntüle" navigation that links to this page

### Required By
- **Phase 23**: Advanced Filtering — may filter by "needs review" status, linking to this verification page
- **Phase 26**: Dashboard — may show metrics about verification throughput
- **Phase 30**: Template Learning — uses correction data tracked by this phase for prompt optimization
---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Verification page loads at `/invoices/{id}/verify` without errors
- [ ] Split-view layout renders: document on left, extracted data on right
- [ ] Image viewer (JPEG, PNG) displays with zoom and pan controls
- [ ] PDF viewer renders multi-page PDFs correctly
- [ ] All extracted fields grouped into sections with Turkish labels
- [ ] Overall confidence score badge shows correct color (green ≥90, yellow 70-89, red <70)
- [ ] Field-level confidence indicators appear as tooltips with validation issue text
- [ ] Low-confidence fields have highlighted background
- [ ] LLM provider badge renders correctly (Gemini / GPT / Claude / XML)
- [ ] Inline editing activates on field click/focus
- [ ] Modified fields show "Değiştirildi" indicator
- [ ] Reset to original value button works on modified fields
- [ ] "Yeniden Doğrula" button calls POST /validate and updates confidence score
- [ ] "Onayla ve Kaydet" shows confirmation dialog, saves, sets status to VERIFIED
- [ ] "Reddet" shows reason input dialog, saves, sets status to REJECTED
- [ ] "Kaydet" saves draft without changing status
- [ ] Invoice items table supports adding new rows
- [ ] Invoice items table supports removing rows
- [ ] Line item auto-calculation works (quantity × unit_price, tax calculation)
- [ ] Amount consistency warning shows when subtotal + tax ≠ total
- [ ] Keyboard shortcut: Tab navigates between fields
- [ ] Keyboard shortcut: Ctrl+Enter triggers verify
- [ ] Keyboard shortcut: Ctrl+S saves draft
- [ ] Keyboard shortcut: Escape cancels editing
- [ ] Correction tracking object captures { field_name, original_value, corrected_value }
- [ ] Corrections are included in PUT request payload
- [ ] Read-only mode for VERIFIED invoices (no edit controls visible)
- [ ] Read-only mode for REJECTED invoices (no edit controls visible)
- [ ] Responsive layout works on desktop (≥1024px)
- [ ] Responsive layout works on tablet (768-1023px)
- [ ] Responsive layout works on mobile (<768px)
- [ ] Unsaved changes guard: browser prompt on navigation with dirty form
- [ ] Loading skeleton displays while data is fetching
- [ ] Error state displays on API failure (404, 500)
- [ ] All user-facing text is in Turkish
- [ ] Dark mode renders correctly for all components
- [ ] `npm run build` completes without errors
- [ ] `npm run lint` passes without errors
- [ ] All component tests pass
- [ ] Result file created at docs/FURKAN/step_results/faz_22.0_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ The verification page is accessible at `/invoices/{id}/verify`
2. ✅ Split-view layout shows original document on the left and extracted data on the right
3. ✅ Document viewer supports images (JPEG, PNG) and PDFs with zoom and pan
4. ✅ All extracted fields are displayed in organized sections with Turkish labels
5. ✅ Overall confidence score is displayed with correct color coding (green/yellow/red)
6. ✅ Field-level confidence indicators show tooltips with validation issue descriptions
7. ✅ Low-confidence fields are visually highlighted (background color)
8. ✅ LLM provider badge is displayed correctly (Gemini/GPT/Claude)
9. ✅ Inline editing works on all fields with form validation
10. ✅ Modified fields show a "Değiştirildi" indicator and can be reset to original value
11. ✅ "Yeniden Doğrula" re-validation updates confidence score after edits
12. ✅ "Onayla ve Kaydet" saves changes and sets status to VERIFIED with confirmation dialog
13. ✅ "Reddet" rejects the invoice with optional reason
14. ✅ "Kaydet" saves draft without changing status
15. ✅ Invoice items table supports add/remove rows with auto-calculation
16. ✅ Amount consistency check (subtotal + tax = total) shows warning locally
17. ✅ Keyboard shortcuts work (Tab, Ctrl+Enter, Ctrl+S, Escape)
18. ✅ Correction tracking captures original vs edited values for each modified field
19. ✅ Read-only mode works for VERIFIED and REJECTED invoices
20. ✅ Responsive design works on desktop, tablet, and mobile
21. ✅ Unsaved changes guard prevents accidental navigation with dirty form
22. ✅ Loading skeletons and error states are handled gracefully
23. ✅ All user-facing text is in Turkish
24. ✅ All component tests pass
25. ✅ Result file is created at docs/FURKAN/step_results/faz_22.0_result.md

---

## IMPORTANT NOTES

1. **No Backend Changes Expected**: This phase is frontend-only. All required endpoints should already exist from Phases 7, 17, and 20. If you discover a missing endpoint or field, document it in the result file rather than modifying backend code.

2. **Correction Tracking Storage**: If the backend does not have an `extraction_corrections` column or endpoint to accept correction data, add it to the PUT request body anyway. The backend can be updated in a small follow-up task. Document this as a required database migration in the result file.

3. **PDF Viewer Library**: Use a well-maintained library for PDF rendering. Recommended options: `react-pdf` (uses pdf.js), `@react-pdf-viewer/core`, or embed via `<iframe>` with pdf.js. Choose based on bundle size and feature needs. Document the chosen library and rationale.

4. **Image Viewer**: For image zoom/pan, consider libraries like `react-zoom-pan-pinch` or implement with CSS transform + event handlers. Keep it lightweight.

5. **Turkish UI**: ALL user-facing text must be in Turkish. Error messages, button labels, status text, tooltips, confirmation dialogs, keyboard shortcut hints — everything. English is only for variable names, code comments, and the result file.

6. **Form Performance**: The invoice items table could have many rows (10-50+ items). Ensure the form does not re-render excessively when editing a single field. Consider using `react-hook-form` with field arrays for efficient form management, or optimize with React.memo and callback memoization.

7. **Dark Mode Support**: The verification page must work correctly in both light and dark mode (already set up in Phase 10). Ensure confidence score colors and field highlights are visible in both themes.

8. **Accountant Workflow**: Remember that the primary users are accountants who may process dozens of invoices per day. The UI should prioritize speed and efficiency: fast field navigation (Tab), quick verification (Ctrl+Enter), clear visual signals for fields that need attention.

9. **Amount Formatting**: Display monetary values with Turkish locale formatting (1.234,56) in the UI, but store and send them as standard decimal numbers (1234.56) in API requests. Use a formatting utility that handles this conversion.

10. **Date Formatting**: Display dates in Turkish format (GG.AA.YYYY) in the UI, but use ISO format (YYYY-MM-DD) for API communication. The date picker component should handle this conversion.

11. **File Type Detection**: The document viewer should detect the file type from the Content-Type header of the GET /invoices/{id}/file response and render accordingly. Do not rely solely on file extension.

12. **No Code in This Prompt**: This prompt describes requirements only. The implementation should be done by the developer using the Antigravity IDE based on these specifications.

---

**Phase 22 Completion Target**: A fully functional, responsive, and accessible invoice verification interface with split-view document comparison, comprehensive inline editing with dirty tracking, real-time confidence visualization, re-validation capability, and efficient keyboard-driven workflow — enabling accountants to quickly review, correct, and approve LLM-extracted invoice data.

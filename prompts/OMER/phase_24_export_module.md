# PHASE 24: EXPORT MODULE — XLSX AND CSV DATA EXPORT

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-23 Completed)
- ✅ Phase 0-3: Docker environment, CI/CD, Hexagonal Architecture, Database schema (companies, users, invoices, invoice_items, categories, audit_logs, batch_jobs)
- ✅ Phase 4-6: JWT Auth, RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN — EXPORT_DATA permission defined in Phase 5), Company & User Management
- ✅ Phase 7: Invoice CRUD API — listing with pagination/sorting, status workflow, category management
- ✅ Phase 8-9: Audit Log (action_type includes EXPORT), Duplication Control
- ✅ Phase 10-12: Frontend — Layout, Auth, Invoice list table with TanStack Query
- ✅ Phase 13-19: Python extraction pipeline — LLM integration, fallback chain, validation, XML parser, RabbitMQ
- ✅ Phase 20-22: File Upload (backend + frontend), Verification UI
- ✅ Phase 23-A (ÖMER): Advanced filtering API — JPA Specifications with all filter parameters (status, dateFrom/dateTo, amountMin/amountMax, supplierName, categoryId, currency, sourceType, llmProvider, confidenceMin/Max, search, createdByUserId, createdFrom/To), GET /invoices/suppliers, GET /invoices/filter-options
- ✅ Phase 23-B (FURKAN): Frontend filter panel — search bar, collapsible filters, date range, multi-selects, amount range, confidence slider, URL-based filter state, active filter chips

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer — Backend + Frontend export UI)
- **Estimated Duration**: 2-3 days

### Key Dependencies from Previous Phases
- **Phase 5 (RBAC)**: `EXPORT_DATA` permission is already defined — ADMIN, MANAGER, ACCOUNTANT can export. INTERN cannot.
- **Phase 8 (Audit Log)**: `EXPORT` action_type is already defined — every export action must be logged.
- **Phase 23-A (Filtering)**: The InvoiceSpecification class and all filter parameters are available. Export must use the SAME specification/filter logic to export exactly what the user sees.

---

## OBJECTIVE

Implement a data export module that allows users to download filtered invoice data in XLSX (Microsoft Excel) and CSV formats. The export must respect the currently active filters from Phase 23 (export what you see), support large datasets via streaming (to prevent OutOfMemory errors), produce professionally formatted XLSX files with headers and styling, and log every export action for audit compliance. The frontend needs an export button integrated into the invoice list page with a format selection dialog.

---

## DETAILED REQUIREMENTS

### 1. Export Architecture — Strategy Pattern

**Purpose**: Design the export system with the Strategy pattern to easily support new formats in Phase 25 (Logo, Mikro, Netsis, Luca accounting software formats).

**Create an export framework with these components:**

**ExportFormat enum:**
- XLSX — Microsoft Excel format
- CSV — Comma-separated values
- (Phase 25 will add: LOGO, MIKRO, NETSIS, LUCA)

**InvoiceExporter interface (Strategy):**
- Package: `application/export/InvoiceExporter.java`
- Method: `export(List<InvoiceExportData> invoices, OutputStream outputStream)` — writes export data to the output stream
- Method: `getFormat()` — returns the ExportFormat this exporter handles
- Method: `getContentType()` — returns the HTTP Content-Type for the response (e.g., "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" for XLSX)
- Method: `getFileExtension()` — returns the file extension (e.g., "xlsx", "csv")

**ExportService:**
- Package: `application/export/ExportService.java`
- Holds a registry of all available InvoiceExporter implementations (injected via Spring)
- Method: `export(ExportFormat format, Specification<InvoiceJpaEntity> spec, OutputStream outputStream)` — resolves the correct exporter and delegates
- Method: `getSupportedFormats()` — returns list of available export formats
- Orchestrates: fetch filtered data → transform to ExportData → delegate to exporter → write to stream

**InvoiceExportData DTO:**
- Package: `application/export/dto/InvoiceExportData.java`
- A flattened DTO specifically designed for export (not the same as the API response DTO)
- Contains all fields that appear in the exported file (see Section 3 for field list)

### 2. Export Endpoint

**GET /api/v1/invoices/export**

**Query Parameters:**
- `format` (required) — "xlsx" or "csv"
- ALL filter parameters from Phase 23-A (dateFrom, dateTo, status, categoryId, amountMin, amountMax, currency, sourceType, llmProvider, confidenceMin, confidenceMax, supplierName, search, createdByUserId, createdFrom, createdTo)
- `includeItems` (optional, boolean, default false) — whether to include invoice line items in the export. When true, each item gets its own row (parent invoice fields repeated). When false, only invoice-level summary is exported.

**Response:**
- Content-Type: Appropriate MIME type for the format
- Content-Disposition: `attachment; filename="faturalar_YYYY-MM-DD_HHmmss.xlsx"` (or .csv)
- Body: The file content streamed directly to the response

**RBAC:**
- Requires `EXPORT_DATA` permission — ADMIN, MANAGER, ACCOUNTANT can export. INTERN gets 403 Forbidden.
- Company-scoped: only exports invoices from the authenticated user's company

**Validation:**
- `format` must be a supported format (return 400 if not)
- If no invoices match the filters, return an empty file with only headers (not a 404)

**Rate Limiting (simple):**
- Maximum 10 export requests per user per minute (prevent abuse)
- Return 429 Too Many Requests if exceeded

### 3. Export Data Fields

The exported file must include these columns (in this order):

**Invoice-Level Columns:**

| # | Column Header (Turkish) | Column Header (English) | Source Field | Notes |
|---|------------------------|------------------------|--------------|-------|
| 1 | Fatura No | Invoice Number | invoice_number | |
| 2 | Fatura Tarihi | Invoice Date | invoice_date | Format: DD.MM.YYYY |
| 3 | Vade Tarihi | Due Date | due_date | Format: DD.MM.YYYY or empty |
| 4 | Tedarikçi Adı | Supplier Name | supplier_name | |
| 5 | Tedarikçi VKN/TCKN | Supplier Tax Number | supplier_tax_number | |
| 6 | Tedarikçi Vergi Dairesi | Supplier Tax Office | supplier_tax_office | |
| 7 | Alıcı Adı | Buyer Name | buyer_name | From company table or invoice |
| 8 | Ara Toplam | Subtotal | subtotal | Number format: #,##0.00 |
| 9 | KDV Tutarı | Tax Amount | tax_amount | Number format: #,##0.00 |
| 10 | Genel Toplam | Total Amount | total_amount | Number format: #,##0.00 |
| 11 | Para Birimi | Currency | currency | TRY, USD, EUR, GBP |
| 12 | Durum | Status | status | Turkish: Beklemede/Onaylandı/Reddedildi |
| 13 | Kaynak | Source | source_type | Turkish: LLM Çıkarım/e-Fatura/Manuel |
| 14 | Kategori | Category | category.name | Category name or empty |
| 15 | Güven Skoru | Confidence Score | confidence_score | Only for LLM source, format: 85.5 |
| 16 | LLM Sağlayıcı | LLM Provider | llm_provider | Only for LLM source |
| 17 | Oluşturan | Created By | created_by_user.full_name | |
| 18 | Oluşturma Tarihi | Created Date | created_at | Format: DD.MM.YYYY HH:mm |
| 19 | Notlar | Notes | notes | |

**Item-Level Columns (when includeItems=true, additional columns):**

| # | Column Header (Turkish) | Column Header (English) | Source Field |
|---|------------------------|------------------------|--------------|
| 20 | Kalem Açıklama | Item Description | item.description |
| 21 | Miktar | Quantity | item.quantity |
| 22 | Birim | Unit | item.unit |
| 23 | Birim Fiyat | Unit Price | item.unit_price |
| 24 | KDV Oranı (%) | Tax Rate (%) | item.tax_rate |
| 25 | Kalem KDV | Item Tax | item.tax_amount |
| 26 | Kalem Toplam | Item Total | item.total_amount |

**When includeItems=true:**
- Each invoice item gets its own row
- Invoice-level fields (columns 1-19) are repeated for each item row
- An invoice with 3 items produces 3 rows (all with the same invoice number, different item details)
- If an invoice has no items, it still appears as a single row with empty item columns

### 4. XLSX Exporter Implementation

**Library**: Apache POI (SXSSFWorkbook for streaming)

**File**: `infrastructure/export/XlsxInvoiceExporter.java`

**XLSX Specific Features:**

**a) Streaming Workbook (SXSSFWorkbook):**
- Use SXSSFWorkbook instead of XSSFWorkbook for memory efficiency
- Set row access window size to 100 (keeps only 100 rows in memory at a time)
- This allows exporting 100,000+ invoices without OutOfMemory errors

**b) Sheet Configuration:**
- Sheet name: "Faturalar" (Invoices)
- Freeze the header row (first row always visible when scrolling)
- Auto-size columns (or set reasonable fixed widths since SXSSFWorkbook has limited auto-size)

**c) Header Row Styling:**
- Bold font
- Background color: dark blue (#1F3864) with white text
- Bottom border
- Center alignment for number columns
- Font: Calibri 11pt

**d) Data Row Formatting:**
- Alternating row colors (white and light gray #F2F2F2) for readability
- Date cells: Use Excel date format "DD.MM.YYYY"
- Number cells: Use Excel number format "#,##0.00" (not text!)
- Currency cells: Number format with currency symbol
- Text cells: Left aligned
- Null values: Leave cell empty (not "null" text)

**e) Status Translation:**
- PENDING → "Beklemede"
- VERIFIED → "Onaylandı"
- REJECTED → "Reddedildi"
- PROCESSING → "İşleniyor"

**f) Source Type Translation:**
- LLM → "LLM Çıkarım"
- E_INVOICE → "e-Fatura"
- MANUAL → "Manuel"

**g) Summary Row (at the end):**
- After all data rows, add a summary row:
  - Column 1: "TOPLAM" (bold)
  - Subtotal column: SUM formula of all subtotals
  - Tax column: SUM formula of all tax amounts
  - Total column: SUM formula of all totals
  - Bold font, top border to separate from data

**h) Metadata Row (optional, at the very top before headers):**
- Row 0: "Fatura Dışa Aktarım Raporu" (bold, merged across all columns)
- Row 1: "Oluşturulma Tarihi: DD.MM.YYYY HH:mm | Filtreler: [active filter summary] | Toplam: X fatura"
- Row 2: Empty (spacer)
- Row 3: Header row
- Row 4+: Data rows

### 5. CSV Exporter Implementation

**Library**: OpenCSV or manual CSV writing

**File**: `infrastructure/export/CsvInvoiceExporter.java`

**CSV Specific Features:**

**a) Encoding:**
- UTF-8 with BOM (Byte Order Mark) — this is CRITICAL for Turkish characters to display correctly in Microsoft Excel. Without BOM, Excel may show garbled characters for ç, ğ, ı, ö, ş, ü.
- BOM bytes: 0xEF, 0xBB, 0xBF (write these first before any CSV content)

**b) Delimiter:**
- Use semicolon (;) as the default delimiter — Turkish Excel installations typically use semicolon because the comma is the decimal separator in Turkish locale
- Add a query parameter `delimiter` (optional, default ";") — allow "comma" for international users

**c) Header Row:**
- Turkish column headers (same as XLSX)

**d) Data Formatting:**
- Dates: DD.MM.YYYY format
- Numbers: Use dot as decimal separator (1234.56) NOT Turkish comma format — CSV parsers handle this better
- Text with semicolons or newlines: Wrap in double quotes
- Text with double quotes: Escape with double-double quotes ("")
- Null values: Empty field (;;)

**e) No Summary Row:**
- CSV files do not include the summary row (that's an XLSX feature)

**f) Status and Source Type:**
- Same Turkish translations as XLSX

### 6. Streaming Architecture for Large Datasets

**Critical**: The export must handle large datasets (10,000+ invoices) without loading everything into memory at once.

**Approach — Paginated Fetching with Streaming Write:**

1. Build the Specification from filter parameters (reuse Phase 23-A InvoiceSpecification)
2. Do NOT call `findAll(spec)` — this loads everything into memory
3. Instead, use paginated fetching in a loop:
   - Fetch page 0 (size 500)
   - Write rows to the output stream
   - Fetch page 1 (size 500)
   - Write rows to the output stream
   - Continue until all pages are processed
4. For XLSX: SXSSFWorkbook handles memory management automatically (row access window)
5. For CSV: Write each page's rows to the OutputStream immediately, then move to the next page
6. Set the HTTP response to streaming mode (no Content-Length header, use chunked transfer encoding)

**Alternative Approach — Spring Data Stream:**
- If Spring Data supports streaming (`Stream<InvoiceJpaEntity>` with `@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "500"))`), this can be used instead of manual pagination
- Wrap in a `@Transactional(readOnly = true)` to maintain the cursor
- Choose whichever approach is simpler and more reliable — document the decision

### 7. Audit Logging

Every export action must be logged in the audit_logs table:

- action_type: "EXPORT"
- entity_type: "INVOICE"
- entity_id: null (batch export, not a single entity)
- description: "Exported {count} invoices in {format} format"
- metadata (JSONB): { format: "XLSX", filters: {active filters summary}, invoiceCount: 142, includeItems: false, fileSize: 245760 }
- user_id: Current authenticated user
- ip_address: From request

### 8. Error Handling

- **No matching invoices**: Return an empty file with only headers (not an error). Include a note in the metadata row (XLSX) or as a comment.
- **Export timeout**: If export takes longer than 120 seconds, abort and return 504 Gateway Timeout
- **Disk/memory issues**: Catch OutOfMemoryError and IOException, return 500 with a clear error message
- **Invalid format**: Return 400 Bad Request with message "Desteklenmeyen dışa aktarım formatı: {format}. Desteklenen formatlar: xlsx, csv"
- **Permission denied**: Return 403 Forbidden
- **Rate limit exceeded**: Return 429 Too Many Requests with Retry-After header

### 9. Configuration — Environment Variables

- `EXPORT_MAX_RECORDS`: Maximum number of invoices per export (default: 50000). If the filter matches more, return 400 with message "Dışa aktarım limiti aşıldı. Lütfen filtrelerinizi daraltın. (Maksimum: 50.000 fatura)"
- `EXPORT_PAGE_SIZE`: Page size for paginated fetching (default: 500)
- `EXPORT_TIMEOUT_SECONDS`: Maximum export duration (default: 120)
- `EXPORT_RATE_LIMIT_PER_MINUTE`: Max exports per user per minute (default: 10)
- `EXPORT_CSV_DELIMITER`: Default CSV delimiter (default: ";")

### 10. Logging

**INFO:**
- Export started (user, format, filter summary, estimated count)
- Export completed (user, format, actual count, file size, duration)

**WARNING:**
- Large export detected (> 10,000 records)
- Slow export (> 30 seconds)

**ERROR:**
- Export failed (exception details)
- OutOfMemory during export
- Stream write failure

---

## FRONTEND REQUIREMENTS

### 11. Export Button and Dialog

Add an export button to the invoice list page (alongside the existing filter panel from Phase 23-B).

**Export Button Location:**
- In the action bar above the invoice table, next to the filter controls
- Button label: "Dışa Aktar" with a download icon
- Button is disabled if the user doesn't have EXPORT_DATA permission (INTERN role)
- Button shows a tooltip for INTERN: "Dışa aktarım yetkiniz bulunmamaktadır"

**Export Dialog (opens when button is clicked):**
- A modal dialog with:
  - Title: "Fatura Verilerini Dışa Aktar"
  - Format selection: Radio buttons or toggle between "Excel (XLSX)" and "CSV"
  - Show which format is recommended: "Önerilen: Excel (XLSX) — formatlı, renkli başlıklar içerir"
  - Checkbox: "Fatura kalemlerini dahil et" (Include invoice items) — default unchecked
  - Info text: "Mevcut filtrelerinize uyan {count} fatura dışa aktarılacaktır" — show the current result count from the invoice list
  - If count > 10000: Show a warning — "Büyük veri seti. İndirme birkaç dakika sürebilir."
  - If count === 0: Disable the export button and show — "Dışa aktarılacak fatura bulunamadı"
  - "İndir" (Download) button — primary action
  - "İptal" (Cancel) button — secondary action

**Download Flow:**
1. User clicks "İndir"
2. Show a loading spinner on the button, text changes to "Hazırlanıyor..."
3. Make a GET request to /api/v1/invoices/export with format and ALL currently active filter parameters (from URL query params / filter state)
4. Receive the file as a blob
5. Trigger browser download (create an anchor element with blob URL, click, revoke)
6. On success: Show a success toast — "Dosya indirildi: faturalar_2026-02-11.xlsx"
7. Close the dialog
8. On error: Show an error toast with the error message, keep dialog open

**Keyboard Shortcut:**
- Ctrl+E (or Cmd+E on Mac): Open export dialog (when on the invoice list page)

### 12. Frontend Technical Details

- Use the existing Axios instance (with auth interceptor) for the export request
- Set `responseType: 'blob'` on the Axios request for file download
- Extract filename from Content-Disposition header if available, otherwise generate: `faturalar_YYYY-MM-DD_HHmmss.{ext}`
- Pass all active filter parameters from the current URL/filter state to the export endpoint
- The export request should NOT go through TanStack Query (it's a one-time download, not cached data)

---

## TESTING REQUIREMENTS

### 1. Backend Unit Tests

- XlsxInvoiceExporter: Generates valid XLSX with correct headers, data rows, formatting, summary row
- CsvInvoiceExporter: Generates valid CSV with UTF-8 BOM, correct delimiters, Turkish characters preserved
- ExportService: Resolves correct exporter by format, handles unknown format
- InvoiceExportData mapper: Correctly maps entity to export DTO with Turkish translations
- Status and source type translations
- Date and number formatting
- Null field handling (empty cells, not "null" text)

### 2. Backend Integration Tests

- Export with no filters → all company invoices exported
- Export with filters → only matching invoices exported (verify count matches list count)
- Export with includeItems=true → item rows included, invoice fields repeated
- Export with includeItems=false → only invoice summary rows
- XLSX format → valid XLSX file parseable by Apache POI
- CSV format → valid CSV with correct encoding (Turkish characters)
- Empty result set → file with only headers, no error
- RBAC: ADMIN, MANAGER, ACCOUNTANT can export → 200
- RBAC: INTERN cannot export → 403
- Multi-tenant: export only includes current user's company data
- Rate limiting: 11th request in a minute → 429
- Large dataset (1000+ rows) → completes without OutOfMemory
- Audit log entry created after export

### 3. Frontend Tests

- Export button visible for ADMIN/MANAGER/ACCOUNTANT
- Export button disabled for INTERN with tooltip
- Export dialog opens and shows correct filter count
- Format selection works (XLSX/CSV toggle)
- Include items checkbox works
- Download flow triggers correct API call with all active filters
- Success toast shows after download
- Error handling shows error toast
- Ctrl+E keyboard shortcut opens dialog

---

## RESULT FILE

When this phase is complete, create a result file at:

**`docs/OMER/step_results/faz_24.0_result.md`**

The result file must contain:

### 1. Execution Summary
- Phase number, assigned developer, start/end dates
- Execution status (COMPLETED / PARTIAL / BLOCKED)
- Total time spent

### 2. Completed Tasks Checklist
- [ ] ExportFormat enum and InvoiceExporter interface (Strategy pattern)
- [ ] XlsxInvoiceExporter with SXSSFWorkbook streaming
- [ ] CsvInvoiceExporter with UTF-8 BOM and semicolon delimiter
- [ ] ExportService with exporter registry
- [ ] InvoiceExportData DTO with field mapping
- [ ] GET /api/v1/invoices/export endpoint
- [ ] Filter integration (reuses Phase 23-A specifications)
- [ ] includeItems support (item-level rows)
- [ ] Streaming architecture for large datasets
- [ ] XLSX formatting (header styling, alternating rows, summary, metadata)
- [ ] CSV formatting (BOM, delimiter, escaping)
- [ ] Turkish translations (status, source type, headers)
- [ ] Audit logging on export
- [ ] RBAC enforcement (EXPORT_DATA permission)
- [ ] Rate limiting
- [ ] Error handling (empty results, timeout, permission denied)
- [ ] Frontend export button and dialog
- [ ] Frontend download flow (blob download)
- [ ] Backend unit tests
- [ ] Backend integration tests
- [ ] Frontend component tests

### 3. Files Created/Modified
List every file with full path and description.

### 4. API Documentation

**GET /api/v1/invoices/export**

Full parameter list, example requests, example responses (headers), error codes.

### 5. Export Strategy Architecture
- Class diagram or description of the Strategy pattern implementation
- How new formats (Phase 25) can be added by implementing InvoiceExporter

### 6. XLSX Sample Description
- Describe or screenshot the XLSX output structure
- Header styling
- Data formatting
- Summary row

### 7. Streaming Performance
- Memory usage observations during large export
- Export duration for various dataset sizes
- SXSSFWorkbook row window configuration

### 8. Database Changes
- Confirm no schema changes needed (audit_logs EXPORT type already exists)
- Or list any migration if needed

### 9. Test Results
- Unit test output summary
- Integration test output summary
- Frontend test output summary

### 10. Issues Encountered
Problems and their solutions

### 11. Next Steps
- What Phase 25 (Accounting Software Export) needs from this phase
- How to add a new ExportMapper (implementation guide for Phase 25)
- Any improvements identified

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 5**: RBAC — EXPORT_DATA permission defined
- **Phase 7**: Invoice CRUD API — InvoiceJpaEntity, InvoiceJpaRepository
- **Phase 8**: Audit Log — EXPORT action_type, audit service
- **Phase 23-A**: InvoiceSpecification class, all filter parameters, JpaSpecificationExecutor on repository
- **Phase 23-B**: Frontend filter panel (URL-based filter state that export will read)

### Required By
- **Phase 25**: Accounting Software Export — will add LOGO, MIKRO, NETSIS, LUCA formats by implementing InvoiceExporter interface
- **Phase 26**: Dashboard — may include "export report" functionality
---

## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] GET /api/v1/invoices/export?format=xlsx returns a downloadable .xlsx file
- [ ] GET /api/v1/invoices/export?format=csv returns a downloadable .csv file
- [ ] XLSX file opens correctly in Microsoft Excel
- [ ] CSV file opens correctly in Excel with Turkish characters preserved
- [ ] CSV uses UTF-8 BOM and semicolon delimiter
- [ ] Export respects active filters (same filters as invoice list)
- [ ] Export with no filters exports all company invoices
- [ ] XLSX has styled header row (bold, colored background)
- [ ] XLSX has alternating row colors
- [ ] XLSX has number formatting for amounts (2 decimal places)
- [ ] XLSX has date formatting (DD.MM.YYYY)
- [ ] XLSX has a summary row at the bottom
- [ ] `includeItems=true` exports line-item detail rows
- [ ] Status values are translated to Turkish (BEKLEMEDE, DOĞRULANMIŞ, REDDEDİLMİŞ)
- [ ] Source type values are translated to Turkish
- [ ] Empty result set produces a file with headers only (no error)
- [ ] Export of 10,000+ invoices works without OutOfMemory (streaming)
- [ ] RBAC: ADMIN, MANAGER, ACCOUNTANT can export
- [ ] RBAC: INTERN gets 403 Forbidden on export
- [ ] Multi-tenant: export only contains current company's invoices
- [ ] Audit log entry created for each export action
- [ ] Rate limiting: more than 10 exports/minute returns 429
- [ ] Frontend export button visible on invoice list page
- [ ] Frontend format selection dialog works
- [ ] Frontend triggers browser file download after export
- [ ] Strategy pattern: InvoiceExporter interface with XLSX and CSV implementations
- [ ] All unit and integration tests pass
- [ ] Result file created at docs/OMER/step_results/faz_24.0_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ GET /api/v1/invoices/export?format=xlsx generates a valid, downloadable XLSX file
2. ✅ GET /api/v1/invoices/export?format=csv generates a valid, downloadable CSV file with Turkish characters
3. ✅ Export respects ALL active filters from Phase 23 (exports only what matches)
4. ✅ XLSX file has professional formatting: styled headers, alternating row colors, number formatting, summary row
5. ✅ CSV file has UTF-8 BOM and semicolon delimiter for Turkish Excel compatibility
6. ✅ includeItems=true exports item-level detail rows
7. ✅ Streaming export handles 10,000+ invoices without OutOfMemory
8. ✅ Status and source type values are translated to Turkish in the export
9. ✅ Dates are formatted as DD.MM.YYYY, numbers as #,##0.00
10. ✅ RBAC: Only ADMIN/MANAGER/ACCOUNTANT can export (INTERN gets 403)
11. ✅ Multi-tenant: Export only contains invoices from the user's company
12. ✅ Audit log entry is created for each export action
13. ✅ Rate limiting prevents abuse (10 exports/minute/user)
14. ✅ Empty filter results produce a file with headers only (no error)
15. ✅ Strategy pattern allows easy addition of new formats (Phase 25 ready)
16. ✅ Frontend export button and dialog work correctly
17. ✅ Frontend download flow triggers browser file download
18. ✅ Frontend reads current active filters and passes to export endpoint
19. ✅ All backend and frontend tests pass
20. ✅ Result file is created at docs/OMER/step_results/faz_24.0_result.md

---

## IMPORTANT NOTES

1. **Strategy Pattern for Phase 25**: The export architecture MUST use Strategy pattern. Phase 25 will add 4 more formats (Logo, Mikro, Netsis, Luca) by simply creating new InvoiceExporter implementations. Do NOT hardcode XLSX/CSV logic — make it pluggable.

2. **Reuse Phase 23-A Specifications**: The export endpoint must accept the SAME filter parameters as GET /invoices and build the Specification using the same InvoiceSpecification class. Do not duplicate filter logic. Extract the specification building into a shared service method if not already done.

3. **SXSSFWorkbook is Essential**: Do NOT use XSSFWorkbook for XLSX — it loads the entire workbook into memory. SXSSFWorkbook writes rows to disk automatically and only keeps a configurable window in memory. This is critical for large exports.

4. **UTF-8 BOM for CSV**: Without the BOM (0xEF 0xBB 0xBF), Microsoft Excel on Windows will NOT display Turkish characters (ç, ğ, ı, ö, ş, ü) correctly when opening the CSV. This is a known Excel quirk. Always write the BOM as the first 3 bytes of the CSV output.

5. **Semicolon Delimiter**: Turkish locale Excel uses semicolon as the list separator (because comma is the decimal separator). Using semicolon as the CSV delimiter makes the file open correctly in Turkish Excel without import wizard. International users can override with the `delimiter` parameter.

6. **Frontend Filter State Access**: The frontend export dialog must read the current active filters from the URL query parameters (set by Phase 23-B). This is the single source of truth for filters. Pass all params to the export endpoint.

7. **No Filename with Turkish Characters**: The Content-Disposition filename should use ASCII-safe characters only. Use: `faturalar_2026-02-11_143025.xlsx` (not `Faturalar_Şubat.xlsx`). Some browsers and OS have issues with non-ASCII filenames in Content-Disposition.

8. **Excel Number Formatting**: Use proper Excel cell styles for number/date cells. Do NOT write numbers as formatted strings (e.g., "1.234,56") — this prevents Excel formulas and sorting from working correctly. Write the raw number (1234.56) with an Excel number format style ("#,##0.00").

9. **Large Export Warning**: If the filtered result count exceeds 10,000, the frontend should show a warning about export duration. If it exceeds EXPORT_MAX_RECORDS (50,000), the backend should reject the request.

10. **Content-Disposition Header**: Use both inline and attachment approaches. For XLSX/CSV export, always use `attachment` to trigger download. Set the filename with the date for easy identification.

---

**Phase 24 Completion Target**: A production-ready export module with XLSX and CSV support, professional formatting, streaming for large datasets, full filter integration, audit logging, RBAC enforcement, and a clean frontend download experience — built on a Strategy pattern that's ready for Phase 25's accounting software format extensions.

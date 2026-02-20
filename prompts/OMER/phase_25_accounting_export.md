# PHASE 25: ACCOUNTING SOFTWARE FORMAT EXPORT — LOGO, MİKRO, NETSİS, LUCA

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 — LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-24 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth, RBAC, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend — Layout, Auth, Invoice list with pagination/sorting
- ✅ Phase 13-19: Python extraction pipeline — LLM integration, fallback chain, validation, XML parser, RabbitMQ
- ✅ Phase 20-22: File Upload (backend + frontend), Verification UI
- ✅ Phase 23: Advanced Filtering — Backend JPA Specifications + Frontend filter panel with URL-based state
- ✅ Phase 24 (ÖMER): Export Module — Strategy pattern (InvoiceExporter interface, ExportFormat enum, ExportService with exporter registry), XLSX exporter (Apache POI SXSSFWorkbook streaming, professional formatting), CSV exporter (UTF-8 BOM, semicolon delimiter), GET /api/v1/invoices/export?format=xlsx|csv with all Phase 23 filter parameters, includeItems support, streaming for large datasets, audit logging, RBAC (EXPORT_DATA permission), rate limiting, InvoiceExportData DTO, frontend export button and download dialog

### What Phase 24 Delivers (Architecture for This Phase)

**Strategy Pattern Already in Place:**
- `ExportFormat` enum — currently has XLSX, CSV. This phase adds LOGO, MIKRO, NETSIS, LUCA.
- `InvoiceExporter` interface — methods: `export(data, outputStream)`, `getFormat()`, `getContentType()`, `getFileExtension()`
- `ExportService` — auto-discovers all `InvoiceExporter` implementations via Spring injection, resolves by format name
- `InvoiceExportData` DTO — flattened invoice data ready for export
- `GET /api/v1/invoices/export?format={format}` — same endpoint, just new format values

**To add a new format, you only need to:**
1. Add the format name to `ExportFormat` enum
2. Create a new class implementing `InvoiceExporter`
3. Annotate it with `@Component` (Spring auto-discovers it)
4. The endpoint and service automatically support the new format — zero changes needed

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer — Backend + Frontend update)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Implement four accounting software format exporters (Logo, Mikro, Netsis, Luca) that produce import-compatible files for Turkey's most popular accounting software packages. Each exporter implements the InvoiceExporter interface from Phase 24, producing files that accountants can directly import into their respective software without manual data entry. The frontend export dialog is updated to include the new format options.

**This feature directly addresses the core value proposition**: accountants can upload invoices, let the system extract data via LLM, verify it, and then export directly to their accounting software — eliminating the entire manual data entry workflow.

---

## TURKISH ACCOUNTING SOFTWARE LANDSCAPE

### Why These Four Software?

These are the most widely used accounting/ERP software in Turkey, covering the vast majority of accounting offices:

1. **Logo** (Logo Yazılım) — Turkey's largest ERP/accounting software provider. Logo Tiger, Logo Go, Logo Mind brands. Used by medium-to-large businesses.
2. **Mikro** (Mikro Yazılım) — Widely used by small-to-medium accounting offices. Mikro İşletme, Mikro Ticari, Mikro Erp brands.
3. **Netsis** (Logo Netsis) — Enterprise-level ERP system, now part of Logo group. Used by larger organizations.
4. **Luca** (Luca Muhasebe) — Cloud-based modern accounting software popular among newer/smaller accounting offices.

### Import File Format Patterns

Turkish accounting software typically accepts invoice data via:
- **XML files** with software-specific schemas (most common for Logo, Netsis)
- **CSV/TXT files** with fixed column layouts and specific delimiter/encoding requirements (Mikro, Luca)
- **Excel (XLS/XLSX) files** with predefined column templates (all software support this as fallback)

**Important**: The exact import formats of these software are proprietary and may vary by version. This implementation creates **best-effort compatible files** based on widely documented formats. The format structure should be configurable so that users or administrators can adjust field mappings if needed.

---

## DETAILED REQUIREMENTS

### 1. ExportFormat Enum Update

Add four new values to the existing ExportFormat enum:

- `LOGO` — Logo accounting software format
- `MIKRO` — Mikro accounting software format
- `NETSIS` — Netsis ERP format
- `LUCA` — Luca cloud accounting format

### 2. Accounting Export Configuration

Create a configuration system that stores the field mapping and format specifications for each accounting software. This makes it easy to adjust mappings without code changes.

**File**: `infrastructure/export/accounting/AccountingExportConfig.java`

**Configuration per software includes:**
- File format (XML, CSV, XLSX)
- File encoding (UTF-8, Windows-1254/ISO-8859-9 for Turkish)
- Delimiter (for CSV formats)
- Date format (DD.MM.YYYY, DD/MM/YYYY, YYYYMMDD, etc.)
- Number format (decimal separator, thousands separator)
- Column/field mapping (which invoice field maps to which software field)
- Header row (whether the file includes column headers)
- Required fields (which fields must have values for a valid import)

**Make these configurable via application properties or a YAML file**:
`application.yml` → `export.accounting.logo.*`, `export.accounting.mikro.*`, etc.

### 3. Logo Export Format

**File**: `infrastructure/export/accounting/LogoInvoiceExporter.java`

**Logo import format — XML-based:**

Logo software (Tiger/Go/Mind) accepts invoice data in a specific XML format for its "Transfer İşlemleri" (Transfer Operations) module.

**Logo XML Structure:**

The exported file should follow this general structure (adapt based on the actual Logo import specification):

- Root element: `<INVOICES>` containing multiple `<INVOICE>` elements
- Each invoice contains header fields and line items
- Key field mappings:

| Invoice Field | Logo XML Element | Notes |
|--------------|-----------------|-------|
| invoice_number | `<NUMBER>` | Fatura No |
| invoice_date | `<DATE>` | Format: DD.MM.YYYY |
| due_date | `<PAYMENT_DATE>` | Vade Tarihi |
| supplier_name | `<ARP_CODE>` or `<AUXIL_CODE>` | Cari hesap kodu — may need lookup |
| supplier_tax_number | `<TAX_NR>` | VKN/TCKN |
| subtotal | `<TOTAL>` | Ara Toplam |
| tax_amount | `<TOTAL_VAT>` | KDV Toplam |
| total_amount | `<GROSS_TOTAL>` | Genel Toplam |
| currency | `<CURR_TRANS>` | 0=TRY, 1=USD, 20=EUR, 10=GBP |
| notes | `<NOTES1>` | Açıklama |

**Line Item Mapping:**

| Item Field | Logo XML Element | Notes |
|-----------|-----------------|-------|
| description | `<DESCRIPTION>` | Açıklama |
| quantity | `<QUANTITY>` | Miktar |
| unit_price | `<PRICE>` | Birim Fiyat |
| tax_rate | `<VAT_RATE>` | KDV Oranı |
| tax_amount | `<VAT_AMOUNT>` | KDV Tutarı |
| total_amount | `<TOTAL>` | Satır Toplam |
| unit | `<UNIT_CODE>` | Birim (ADET, KG, etc.) |

**File extension**: `.xml`
**Content-Type**: `application/xml`
**Encoding**: UTF-8

### 4. Mikro Export Format

**File**: `infrastructure/export/accounting/MikroInvoiceExporter.java`

**Mikro import format — CSV/TXT-based:**

Mikro software accepts invoice data via delimited text files for its "Dış Kaynaklardan Veri Aktarımı" (External Data Import) module.

**Mikro CSV Structure:**

- Delimiter: pipe character (`|`) — Mikro commonly uses pipe
- Encoding: Windows-1254 (Turkish ANSI) — critical for Mikro to read Turkish characters
- No header row (Mikro expects data starting from line 1)
- One row per invoice item (invoice header fields repeated per line)

**Column layout (pipe-delimited):**

| Position | Field | Source | Notes |
|----------|-------|--------|-------|
| 1 | Fatura Tipi | Fixed | "1" = Alış Faturası (Purchase Invoice) |
| 2 | Fatura No | invoice_number | |
| 3 | Fatura Tarihi | invoice_date | Format: DD.MM.YYYY |
| 4 | Vade Tarihi | due_date | Format: DD.MM.YYYY |
| 5 | Cari Hesap Kodu | supplier_tax_number | VKN used as account code |
| 6 | Cari Hesap Adı | supplier_name | |
| 7 | Stok Kodu | item.product_code | Or description if no code |
| 8 | Stok Adı | item.description | |
| 9 | Miktar | item.quantity | Decimal: dot separator |
| 10 | Birim | item.unit | |
| 11 | Birim Fiyat | item.unit_price | Decimal: dot separator |
| 12 | KDV Oranı | item.tax_rate | |
| 13 | KDV Tutarı | item.tax_amount | |
| 14 | Satır Toplam | item.total_amount | |
| 15 | Para Birimi | currency | TL, USD, EUR, GBP |
| 16 | Açıklama | notes | |

**File extension**: `.txt`
**Content-Type**: `text/plain; charset=windows-1254`
**Encoding**: Windows-1254 (ISO-8859-9 / Turkish ANSI)

### 5. Netsis Export Format

**File**: `infrastructure/export/accounting/NetsisInvoiceExporter.java`

**Netsis import format — XML-based:**

Netsis (now part of Logo group) accepts invoice data in XML format through its integration module.

**Netsis XML Structure:**

- Root element: `<FATURA_LISTESI>` containing `<FATURA>` elements
- Turkish element names (Netsis uses Turkish naming convention)
- Key field mappings:

| Invoice Field | Netsis XML Element | Notes |
|--------------|-------------------|-------|
| invoice_number | `<FATURA_NO>` | |
| invoice_date | `<FATURA_TARIHI>` | Format: YYYY-MM-DD |
| due_date | `<VADE_TARIHI>` | |
| supplier_name | `<CARI_UNVAN>` | |
| supplier_tax_number | `<VERGI_NO>` | |
| supplier_tax_office | `<VERGI_DAIRESI>` | |
| subtotal | `<ARA_TOPLAM>` | |
| tax_amount | `<KDV_TOPLAM>` | |
| total_amount | `<GENEL_TOPLAM>` | |
| currency | `<DOVIZ_CINSI>` | TL, USD, EUR, GBP |
| notes | `<ACIKLAMA>` | |

**Line Item Mapping:**

| Item Field | Netsis XML Element | Notes |
|-----------|-------------------|-------|
| description | `<STOK_ADI>` | |
| quantity | `<MIKTAR>` | |
| unit | `<BIRIM>` | |
| unit_price | `<BIRIM_FIYAT>` | |
| tax_rate | `<KDV_ORANI>` | |
| tax_amount | `<KDV_TUTARI>` | |
| total_amount | `<TUTAR>` | |
| product_code | `<STOK_KODU>` | |

**File extension**: `.xml`
**Content-Type**: `application/xml`
**Encoding**: UTF-8

### 6. Luca Export Format

**File**: `infrastructure/export/accounting/LucaInvoiceExporter.java`

**Luca import format — XLSX-based:**

Luca cloud accounting software accepts invoice data via Excel templates. Luca provides a standard import template with predefined column headers.

**Luca XLSX Structure:**

- Use Apache POI XSSFWorkbook (not streaming — Luca imports are typically small)
- Sheet name: "Faturalar"
- Row 1: Header row with specific column names (Luca expects exact Turkish headers)
- Data starts from row 2

**Column layout:**

| Column | Header (Turkish) | Source | Notes |
|--------|-----------------|--------|-------|
| A | Fatura No | invoice_number | |
| B | Fatura Tarihi | invoice_date | Format: DD.MM.YYYY |
| C | Vade Tarihi | due_date | |
| D | Fatura Tipi | Fixed | "Alış" (Purchase) |
| E | Cari Hesap | supplier_name | |
| F | Vergi No | supplier_tax_number | |
| G | Vergi Dairesi | supplier_tax_office | |
| H | Kalem Açıklaması | item.description | |
| I | Miktar | item.quantity | |
| J | Birim | item.unit | |
| K | Birim Fiyat | item.unit_price | |
| L | KDV Oranı (%) | item.tax_rate | |
| M | KDV Tutarı | item.tax_amount | |
| N | Satır Toplam | item.total_amount | |
| O | Para Birimi | currency | |
| P | Açıklama | notes | |

**File extension**: `.xlsx`
**Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
**Encoding**: UTF-8

**Luca Header Row Styling:**
- Bold, background color: light blue (#DAEEF3)
- Simple formatting (Luca's import is less tolerant of complex Excel formatting)

### 7. Common Accounting Export Utilities

**File**: `infrastructure/export/accounting/AccountingExportUtils.java`

Create a shared utility class with common helpers:

- `formatDateForAccounting(LocalDate date, String pattern)` — formats dates for different software
- `formatNumberForAccounting(BigDecimal number, boolean useDotDecimal)` — formats numbers
- `translateCurrency(Currency currency, String targetFormat)` — converts currency enum to software-specific codes (e.g., TRY → "TL", "0", "949")
- `translateUnit(String unit)` — maps standard units to software-specific codes
- `sanitizeText(String text, int maxLength)` — trims and cleans text for safe import
- `encodeForWindows1254(String text)` — charset conversion for Mikro

### 8. Export Format Metadata Endpoint Update

Update the existing **GET /api/v1/invoices/export/formats** (or add it if not present in Phase 24):

Returns all available export formats with metadata:

Response:
```
[
  {
    "format": "XLSX",
    "label": "Excel (XLSX)",
    "description": "Microsoft Excel formatında dışa aktarım",
    "category": "GENERAL",
    "fileExtension": "xlsx",
    "icon": "file-spreadsheet"
  },
  {
    "format": "CSV",
    "label": "CSV",
    "description": "Virgülle ayrılmış değerler formatı",
    "category": "GENERAL",
    "fileExtension": "csv",
    "icon": "file-text"
  },
  {
    "format": "LOGO",
    "label": "Logo",
    "description": "Logo Tiger/Go/Mind muhasebe yazılımı formatı (XML)",
    "category": "ACCOUNTING",
    "fileExtension": "xml",
    "icon": "building"
  },
  {
    "format": "MIKRO",
    "label": "Mikro",
    "description": "Mikro muhasebe yazılımı formatı (TXT)",
    "category": "ACCOUNTING",
    "fileExtension": "txt",
    "icon": "building"
  },
  {
    "format": "NETSIS",
    "label": "Netsis",
    "description": "Netsis ERP muhasebe yazılımı formatı (XML)",
    "category": "ACCOUNTING",
    "fileExtension": "xml",
    "icon": "building"
  },
  {
    "format": "LUCA",
    "label": "Luca",
    "description": "Luca bulut muhasebe yazılımı formatı (XLSX)",
    "category": "ACCOUNTING",
    "fileExtension": "xlsx",
    "icon": "cloud"
  }
]
```

The `category` field distinguishes between general-purpose formats (XLSX/CSV) and accounting software formats. The frontend uses this to group formats in the UI.

### 9. Verified-Only Export for Accounting Formats

**Important Business Rule**: Accounting software imports should only contain VERIFIED invoices. Exporting PENDING or REJECTED invoices to accounting software is a data integrity risk.

When the format is LOGO, MIKRO, NETSIS, or LUCA:
- Automatically add a filter: `status = VERIFIED` (regardless of what the user selected)
- If the user explicitly filtered by a different status, show a warning in the response or override silently
- Log a warning if non-VERIFIED invoices were filtered out
- If no VERIFIED invoices match the filters, return an empty file with a metadata note

For XLSX and CSV formats, do NOT apply this restriction — they are general-purpose exports.

### 10. Frontend — Updated Export Dialog

Update the Phase 24 export dialog to include the new accounting software formats:

**Updated Dialog Layout:**

The dialog should now group formats into two sections:

**Section 1: "Genel Formatlar" (General Formats)**
- Excel (XLSX) — radio button, recommended badge
- CSV — radio button

**Section 2: "Muhasebe Yazılımları" (Accounting Software)**
- Logo — radio button, with logo/icon
- Mikro — radio button, with logo/icon
- Netsis — radio button, with logo/icon
- Luca — radio button, with logo/icon

**Accounting Format Selection Behavior:**
- When an accounting format is selected, show an info banner: "Muhasebe yazılımına aktarım sadece onaylanmış (doğrulanmış) faturaları içerir."
- The "Fatura kalemlerini dahil et" checkbox should be hidden (accounting formats always include items)
- Show the selected software's file format: "Dosya formatı: XML" or "Dosya formatı: TXT (Pipe-delimited)"
- Show a tooltip on each accounting format explaining its compatibility: "Logo Tiger, Logo Go ve Logo Mind yazılımlarına aktarım için uygundur"

**Format Options Loaded from API:**
- On dialog open, fetch GET /api/v1/invoices/export/formats
- Dynamically populate the format list from the API response
- Group by `category` field (GENERAL vs ACCOUNTING)

### 11. Sample Export Validation

For each accounting format, create a set of sample test data and validate the output:

- Create at least 3 test invoices with varying data (different currencies, multiple items, null fields)
- Export in each format
- Manually verify the output structure matches the expected schema
- Document the sample outputs in the result file

### 12. Logging

**INFO:**
- Accounting export started (format, user, filter summary, estimated count)
- Accounting export completed (format, count, file size, duration)
- Verified-only filter applied for accounting format (original count vs filtered count)

**WARNING:**
- Non-verified invoices excluded from accounting export (count of excluded)
- Field mapping issue (field could not be mapped for target format)
- Encoding conversion warning (character not representable in target encoding)

**ERROR:**
- Export generation failure per format
- XML generation error
- Encoding conversion failure

---

## TESTING REQUIREMENTS

### 1. Unit Tests

Per exporter (Logo, Mikro, Netsis, Luca):
- Generates valid output file with correct structure
- Field mappings are correct (invoice fields → target format fields)
- Date formatting matches target software requirements
- Number formatting matches target software requirements
- Currency code translation is correct
- Turkish characters are preserved (especially for Mikro Windows-1254 encoding)
- Null/empty fields are handled gracefully
- Multiple invoices with multiple items export correctly
- Empty invoice set produces valid empty file (headers only or empty root element)

### 2. Integration Tests

- GET /api/v1/invoices/export?format=logo → valid XML file
- GET /api/v1/invoices/export?format=mikro → valid pipe-delimited TXT file in Windows-1254
- GET /api/v1/invoices/export?format=netsis → valid XML file
- GET /api/v1/invoices/export?format=luca → valid XLSX file
- Accounting formats only export VERIFIED invoices (auto-filter applied)
- Filter parameters work with accounting formats
- RBAC: EXPORT_DATA permission required
- Audit log entry created for each accounting export
- Export format metadata endpoint returns all 6 formats
- Unknown format → 400 error

### 3. Frontend Tests

- Export dialog shows grouped format options (General + Accounting)
- Selecting accounting format shows info banner about verified-only
- Include items checkbox hidden for accounting formats
- Format options loaded from API
- Download flow works for each accounting format

---

## RESULT FILE

When this phase is complete, create a result file at:

**`docs/OMER/step_results/faz_25.0_result.md`**

The result file must contain:

### 1. Execution Summary
- Phase number, assigned developer, start/end dates
- Execution status (COMPLETED / PARTIAL / BLOCKED)
- Total time spent

### 2. Completed Tasks Checklist
- [ ] ExportFormat enum updated with LOGO, MIKRO, NETSIS, LUCA
- [ ] AccountingExportConfig — configurable field mappings
- [ ] AccountingExportUtils — shared utilities
- [ ] LogoInvoiceExporter — XML output
- [ ] MikroInvoiceExporter — pipe-delimited TXT, Windows-1254 encoding
- [ ] NetsisInvoiceExporter — XML output
- [ ] LucaInvoiceExporter — XLSX output
- [ ] Verified-only auto-filter for accounting formats
- [ ] Export format metadata endpoint (GET /export/formats)
- [ ] Frontend export dialog updated with grouped format options
- [ ] Unit tests for all 4 exporters
- [ ] Integration tests
- [ ] Frontend tests
- [ ] Sample export validation

### 3. Files Created/Modified
List every file with full path and description.

### 4. Format Specification Documentation
For each accounting format, document:
- File structure (columns/elements, order, types)
- Encoding and delimiter
- Date/number formatting rules
- Field mapping table
- Known limitations or compatibility notes

### 5. Sample Export Outputs
For each format, include a sample output snippet (first few rows/elements) demonstrating the format structure.

### 6. Test Results
- Unit test output summary per exporter
- Integration test output summary
- Frontend test output summary

### 7. Database Changes
- Confirm no schema changes needed
- Or list any migration if needed

### 8. Configuration
- Document all new application.yml properties
- Document how to customize field mappings

### 9. Issues Encountered
Problems and their solutions

### 10. Compatibility Notes
- Known software version compatibility (e.g., "tested with Logo Tiger 3.x format")
- Limitations and workarounds
- Recommendations for users

### 11. Next Steps
- What Phase 26 (Dashboard) might need from export statistics
- Future format additions (how to add new software)
- Any format refinements based on real-world testing

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 24**: Export Strategy pattern infrastructure (InvoiceExporter interface, ExportFormat enum, ExportService, GET /export endpoint, InvoiceExportData DTO, frontend export dialog)
- **Phase 23-A**: Filter specifications (reused for filtered accounting export)
- **Phase 8**: Audit logging (EXPORT action type)

### Required By
- **Phase 26**: Dashboard — may show export statistics
- **Phase 42**: Documentation — accounting export user guide

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] GET /api/v1/invoices/export?format=logo generates valid Logo XML
- [ ] GET /api/v1/invoices/export?format=mikro generates valid Mikro TXT with Windows-1254 encoding
- [ ] GET /api/v1/invoices/export?format=netsis generates valid Netsis XML
- [ ] GET /api/v1/invoices/export?format=luca generates valid Luca XLSX
- [ ] All accounting formats include invoice line items (always includeItems=true)
- [ ] Accounting formats auto-filter to VERIFIED invoices only
- [ ] Turkish characters preserved in all formats (test: ÇĞİÖŞÜçğıöşü)
- [ ] Mikro TXT: Windows-1254 encoding verified (open in Turkish locale)
- [ ] Logo XML: field mappings match Logo import spec
- [ ] Netsis XML: field mappings match Netsis import spec
- [ ] Luca XLSX: column structure matches Luca import template
- [ ] Date formatting correct per software expectations
- [ ] Number formatting correct per software expectations
- [ ] Currency codes mapped correctly per software
- [ ] Filter parameters from Phase 23 work with accounting formats
- [ ] GET /api/v1/invoices/export/formats returns all 6 formats grouped by category
- [ ] Frontend dialog shows General and Accounting format groups
- [ ] Frontend shows info banner: "Sadece doğrulanmış faturalar" for accounting formats
- [ ] Each format implements InvoiceExporter interface (Open/Closed principle)
- [ ] Adding a hypothetical new format only requires one new class
- [ ] RBAC and audit logging work for all new formats
- [ ] Sample export files generated and validated
- [ ] All unit and integration tests pass
- [ ] Result file created at docs/OMER/step_results/faz_25.0_result.md

---
## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ GET /api/v1/invoices/export?format=logo generates a valid Logo-compatible XML file
2. ✅ GET /api/v1/invoices/export?format=mikro generates a valid Mikro-compatible TXT file with Windows-1254 encoding
3. ✅ GET /api/v1/invoices/export?format=netsis generates a valid Netsis-compatible XML file
4. ✅ GET /api/v1/invoices/export?format=luca generates a valid Luca-compatible XLSX file
5. ✅ All accounting formats include invoice line items (always includeItems=true)
6. ✅ Accounting formats only export VERIFIED invoices (auto-filter)
7. ✅ Turkish characters are preserved in all formats (including Mikro's Windows-1254)
8. ✅ Field mappings are correct for each software
9. ✅ Date and number formatting matches each software's expectations
10. ✅ Currency codes are translated correctly for each software
11. ✅ Filter parameters from Phase 23 work with accounting formats
12. ✅ Export format metadata endpoint returns all 6 formats with categories
13. ✅ Frontend dialog groups formats into General and Accounting sections
14. ✅ Frontend shows verified-only info banner for accounting formats
15. ✅ Adding a new format requires only a new InvoiceExporter implementation (Open/Closed principle verified)
16. ✅ RBAC and audit logging work for all new formats
17. ✅ All unit and integration tests pass
18. ✅ Sample exports validated for each format
19. ✅ Result file is created at docs/OMER/step_results/faz_25.0_result.md

---

## IMPORTANT NOTES

1. **Format Accuracy Disclaimer**: The exact import formats for Logo, Mikro, Netsis, and Luca may vary by software version and configuration. This implementation provides a **best-effort compatible format** based on commonly documented specifications. Users may need to adjust field mappings for their specific software version. The configuration system (Section 2) enables this without code changes.

2. **Research the Actual Formats**: Before implementing, search for the latest import format documentation for each software. Look for "Logo veri aktarım XML formatı", "Mikro dış kaynaklardan veri aktarımı", "Netsis XML entegrasyon", "Luca toplu fatura yükleme" — these searches should yield format specifications or community guides.

3. **Windows-1254 for Mikro is Critical**: Mikro software running on Turkish Windows expects Windows-1254 (also known as ISO-8859-9 Turkish) encoding. UTF-8 files will show garbled Turkish characters in Mikro. Use Java's `Charset.forName("windows-1254")` or `new OutputStreamWriter(outputStream, "Windows-1254")`.

4. **Open/Closed Principle Verification**: After implementing all 4 exporters, verify that you can add a hypothetical 5th exporter by ONLY creating a new `@Component` class implementing `InvoiceExporter`. No changes should be needed in ExportService, ExportController, or the frontend (if it loads formats from API).

5. **Verified-Only is a Business Rule, Not a Bug**: Accounting software imports create real financial records. Importing unverified (PENDING) or rejected invoices could cause accounting errors. This is intentional and should be clearly communicated to the user.

6. **Invoice Type Assumption**: All exported invoices are treated as "Alış Faturası" (Purchase Invoice / Incoming Invoice). Sales invoices (Satış Faturası) are out of scope for this system — the system processes received invoices that need to be entered into the accounting records.

7. **Cari Hesap (Account Code) Mapping**: Accounting software typically requires a "Cari Hesap Kodu" (current account code) to match invoices to supplier accounts. Since this system doesn't manage account charts, use the supplier_tax_number as a substitute. Accountants will need to match this to their actual account codes in the software. Document this as a known limitation.

8. **No Code in This Prompt**: This prompt describes requirements only. Implementation should be done by the developer using the Antigravity IDE.

---

**Phase 25 Completion Target**: Four production-ready accounting software format exporters that seamlessly plug into the Phase 24 export framework via Strategy pattern, producing import-compatible files for Logo (XML), Mikro (TXT/pipe-delimited/Windows-1254), Netsis (XML), and Luca (XLSX) — completing the full export pipeline from LLM extraction to accounting software integration.

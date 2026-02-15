# PHASE 18: E-FATURA (E-INVOICE) XML PARSER

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 - LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-17 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend
- ✅ Phase 13: Python FastAPI service setup
- ✅ Phase 14: Image preprocessing pipeline (Pillow + PyMuPDF)
- ✅ Phase 15: Gemini 3 Flash integration — base provider interface, Gemini client, extraction prompt, response parser, InvoiceData model, ExtractionResult model, custom errors
- ✅ Phase 16: LLM Fallback Chain — GPT-5.2 + Claude Haiku 4.5 providers, fallback chain manager (Gemini → GPT → Claude), provider health tracking, prompt adaptation, response normalization
- ✅ Phase 17: Response Validation & Confidence Score — 5-category validation pipeline (field completeness, format, math consistency, value range, cross-field logic), weighted confidence score (0-100), suggested status (AUTO_VERIFIED / NEEDS_REVIEW / LOW_CONFIDENCE), POST /validate endpoint, configurable thresholds

### What Previous Phases Delivered (Key for This Phase)
- **InvoiceData Pydantic model** (Phase 15): The common data model all extraction paths must output — invoice_number, invoice_date, due_date, supplier_name, supplier_tax_number, supplier_address, buyer_name, buyer_tax_number, items[], subtotal, tax_amount, total_amount, currency, notes
- **Validation pipeline** (Phase 17): Accepts InvoiceData and returns ValidationResult with confidence_score. XML-parsed data should naturally score 95-100 since it's structured, not LLM-guessed.
- **ExtractionResult model** (Phase 15-16): Contains invoice_data, provider name, processing duration, validation_result. For XML parsing, provider will be "XML_PARSER" (not an LLM).
- **Database columns** (Phase 3): invoices.source_type has constraint IN ('LLM', 'E_INVOICE', 'MANUAL'). For XML-parsed invoices, source_type = 'E_INVOICE'. Also: e_invoice_uuid, e_invoice_ettn columns exist.

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 2-3 days
- **Dependency**: Phase 13 (FastAPI service structure). Note: Phase 18 depends on Phase 13, NOT on Phase 15-17. It can technically run in parallel with the LLM phases. However, since Phases 15-17 are now complete, Phase 18 should reuse the InvoiceData model and validation pipeline.

---

## OBJECTIVE

Build a parser for Turkish GİB (Gelir İdaresi Başkanlığı) e-Invoice XML files that follow the UBL-TR (Universal Business Language — Turkey) standard. The parser extracts all invoice fields from the structured XML and maps them to the same InvoiceData model used by LLM extraction. Since e-Invoice data is already structured (no LLM guessing involved), it should produce very high confidence scores and bypass LLM API calls entirely, saving cost and time.

---

## WHY A SEPARATE XML PARSER

E-Invoices are fundamentally different from scanned invoice images:

- **Images** → Need LLM to "read" and interpret → uncertain, needs validation
- **XML files** → Already contain structured data → deterministic, highly reliable

Sending an e-Invoice XML to an LLM would be wasteful — the data is already there in the XML tags. The parser simply extracts and maps it.

**Benefits:**
- Zero LLM API cost for e-Invoice processing
- Near-instant processing (no API latency)
- Near-perfect accuracy (data comes from the source system)
- Confidence scores of 95-100 automatically

---

## GİB E-FATURA UBL-TR FORMAT

### What is UBL-TR?

UBL-TR is Turkey's adaptation of the OASIS UBL 2.1 (Universal Business Language) standard. GİB (Turkey's Revenue Administration) mandates this format for all e-Invoices in Turkey. Every e-Invoice registered through GİB's system follows this XML schema.

### XML Structure Overview

A GİB e-Invoice XML file has a root element of "Invoice" within the UBL namespace. The key sections relevant for data extraction are:

**Header Information:**
- Invoice ID (fatura numarası) — in the "ID" element
- Issue Date (fatura tarihi) — in the "IssueDate" element
- Due Date (vade tarihi) — in the "PaymentMeans/PaymentDueDate" or "PaymentTerms/PaymentDueDate" element
- Invoice UUID (GİB UUID) — in the "UUID" element
- Invoice Type Code — in the "InvoiceTypeCode" element (SATIS, IADE, TEVKIFAT, ISTISNA, OZELMATRAH, IHRACKAYITLI)
- Currency — in the "DocumentCurrencyCode" element

**Supplier (Satıcı) Information:**
- Located in "AccountingSupplierParty/Party" section
- Name: "PartyName/Name" or "PartyIdentification" elements
- Tax Number: "PartyIdentification/ID" with schemeID="VKN" or "TCKN"
- Tax Office: "PartyTaxScheme/TaxScheme/Name"
- Address: "PostalAddress" sub-elements (StreetName, CitySubdivisionName, CityName, Country)

**Buyer (Alıcı) Information:**
- Located in "AccountingCustomerParty/Party" section
- Same structure as supplier

**Line Items (Fatura Kalemleri):**
- Located in "InvoiceLine" elements (repeating)
- Each line has: "ID" (line number), "InvoicedQuantity" (miktar + unitCode), "LineExtensionAmount" (satır toplamı), "Item/Name" (açıklama), "Price/PriceAmount" (birim fiyat)
- Tax per line: "TaxTotal/TaxSubtotal" with TaxAmount and Percent

**Totals:**
- Located in "LegalMonetaryTotal" section
- LineExtensionAmount = subtotal (KDV hariç ara toplam)
- TaxExclusiveAmount = subtotal
- TaxInclusiveAmount = grand total (KDV dahil genel toplam)
- PayableAmount = payable amount
- Tax totals in "TaxTotal" section with TaxAmount

### UBL-TR Namespace

The XML uses these namespaces (may vary slightly between versions):
- Default namespace: "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
- Common types: "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" (prefix: cbc)
- Aggregate types: "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" (prefix: cac)

The parser must handle namespace prefixes correctly. Different e-Invoice generators may use different prefix names but the namespace URIs remain the same.

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add the XML parser module to the existing extraction-service:

```
extraction-service/app/
├── services/
│   ├── preprocessing/          ← Phase 14
│   ├── llm/                    ← Phase 15-16
│   ├── validation/             ← Phase 17
│   └── parsers/
│       ├── __init__.py
│       ├── xml_parser.py             # Main e-Invoice XML parser
│       ├── ubl_field_extractor.py    # UBL-TR field extraction logic
│       └── file_type_detector.py     # Detect whether file is image or XML
├── models/
│   ├── invoice_data.py         ← Phase 15 (shared)
│   ├── extraction.py           ← Phase 15-16 (shared)
│   └── validation.py           ← Phase 17 (shared)
└── ...
```

### 2. File Type Detection

Create a file type detector that determines whether an uploaded file should be routed to the LLM extraction pipeline (images) or the XML parser.

**Detection Logic:**
- Check file extension first: .xml → XML parser path
- Check MIME type: application/xml, text/xml → XML parser path
- Check magic bytes: XML files start with "<?xml" or "<" (with potential BOM)
- For image formats (.jpg, .jpeg, .png, .pdf, .tiff, .webp, .bmp) → LLM extraction path
- For ambiguous cases: attempt to parse as XML first; if it fails, route to LLM

**Additional Check for e-Invoice:**
- Not all XML files are e-Invoices. After detecting XML, verify it's a UBL-TR e-Invoice by checking:
  - Root element is "Invoice" in the UBL namespace
  - Contains "cbc:ID" and "cbc:IssueDate" elements
- If the XML is not a valid UBL-TR invoice, return an error explaining it's not a recognized e-Invoice format

### 3. XML Parser Core

Use Python's `lxml` library (preferred for namespace handling and XPath support) or `xml.etree.ElementTree` (standard library, no extra dependency).

**Recommendation:** Use `lxml` for better namespace and XPath support. If lxml is not already in requirements.txt, add it.

**Parsing Approach:**
- Parse the XML file into an element tree
- Define namespace mappings for XPath queries
- Extract each field using XPath expressions targeting the correct UBL-TR paths
- Map extracted values to the InvoiceData Pydantic model (same model as LLM extraction)

### 4. Field Extraction Mapping

Map UBL-TR XML elements to InvoiceData fields:

| InvoiceData Field | UBL-TR XML Path | Notes |
|---|---|---|
| invoice_number | /Invoice/cbc:ID | Direct text content |
| invoice_date | /Invoice/cbc:IssueDate | Format: YYYY-MM-DD (already ISO) |
| due_date | /Invoice/cac:PaymentMeans/cbc:PaymentDueDate OR /Invoice/cac:PaymentTerms/cbc:PaymentDueDate | May not be present |
| supplier_name | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name | Also check PartyIdentification |
| supplier_tax_number | /Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyIdentification/cbc:ID[@schemeID='VKN' or @schemeID='TCKN'] | Check schemeID attribute |
| supplier_address | Combine: StreetName + CitySubdivisionName + CityName from PostalAddress | Concatenate address parts |
| buyer_name | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyName/cbc:Name | Same structure as supplier |
| buyer_tax_number | /Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID[@schemeID='VKN' or @schemeID='TCKN'] | Same logic as supplier |
| items[] | /Invoice/cac:InvoiceLine (repeating) | See item mapping below |
| subtotal | /Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount | Attribute: currencyID |
| tax_amount | /Invoice/cac:TaxTotal/cbc:TaxAmount | Total tax |
| total_amount | /Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount OR cbc:PayableAmount | Grand total |
| currency | /Invoice/cbc:DocumentCurrencyCode | TRY, USD, EUR etc. |
| notes | /Invoice/cbc:Note | May have multiple Note elements, concatenate |

**Item Mapping (for each InvoiceLine):**

| InvoiceData Item Field | UBL-TR XML Path (relative to InvoiceLine) | Notes |
|---|---|---|
| description | cac:Item/cbc:Name | Product/service name |
| quantity | cbc:InvoicedQuantity | Text content is the number |
| unit | cbc:InvoicedQuantity/@unitCode | Attribute: unitCode (C62=adet, KGM=kg, LTR=lt, etc.) |
| unit_price | cac:Price/cbc:PriceAmount | Price per unit |
| tax_rate | cac:TaxTotal/cac:TaxSubtotal/cbc:Percent | KDV rate percentage |
| tax_amount | cac:TaxTotal/cac:TaxSubtotal/cbc:TaxAmount | Tax for this line |
| line_total | cbc:LineExtensionAmount | Line total (usually pre-tax) |

**Unit Code Translation:**
Map UBL unit codes to Turkish display names:
- C62 → "Adet" (piece)
- KGM → "Kg" (kilogram)
- LTR → "Lt" (liter)
- MTR → "Metre"
- MTK → "m²"
- MTQ → "m³"
- TNE → "Ton"
- HUR → "Saat" (hour)
- DAY → "Gün" (day)
- MON → "Ay" (month)
- ANN → "Yıl" (year)
- If unknown code → use the raw code as-is

### 5. E-Invoice Specific Fields

Extract additional e-Invoice specific data that doesn't exist in LLM-extracted invoices:

- **e_invoice_uuid**: From /Invoice/cbc:UUID — GİB's unique identifier
- **e_invoice_ettn**: Same as UUID in most cases (ETTN = Elektronik Takip Numarası)
- **invoice_type_code**: From /Invoice/cbc:InvoiceTypeCode (SATIS, IADE, etc.)
- **profile_id**: From /Invoice/cbc:ProfileID (TICARIFATURA, EARSIVFATURA, etc.)

These fields map to the existing database columns: invoices.e_invoice_uuid and invoices.e_invoice_ettn (added in Phase 3).

### 6. Extraction Result for XML

When returning the result from XML parsing, the ExtractionResult should be populated as follows:

- **invoice_data**: The parsed InvoiceData object
- **provider**: "XML_PARSER" (not a real LLM provider — indicates XML parsing was used)
- **processing_duration_ms**: Time taken to parse (typically < 50ms)
- **prompt_version**: null or "N/A" (no prompt used)
- **raw_response**: The original XML content (or a truncated version for audit)
- **source_type**: "E_INVOICE" (maps to invoices.source_type in database)
- **validation_result**: Run the Phase 17 validation pipeline on the parsed data. E-Invoice data should score 95-100 naturally.

### 7. Validation Integration

After parsing the XML, run the parsed InvoiceData through the Phase 17 validation pipeline (the same one used for LLM results).

**Expected behavior for e-Invoice data:**
- Field completeness: Should score very high (e-Invoices have all mandatory fields)
- Format validation: Should pass (dates already in ISO format, tax numbers from official source)
- Math consistency: Should be near-perfect (values come from the source system)
- Value range: Should pass (official invoices have valid amounts)
- Cross-field logic: Should pass

**If an e-Invoice scores below 90:** This is unexpected and likely indicates a parsing error or a malformed XML file. Log a warning with details.

### 8. Updated Extraction Service — Smart Routing

Modify the main extraction service (extraction_service.py) to include file type detection and smart routing:

**Updated Flow:**
1. Receive a file
2. Detect file type (image vs XML)
3. **If XML:** Route to XML parser → parse → validate → return result with source_type="E_INVOICE"
4. **If Image:** Route to preprocessing → fallback chain LLM extraction → validate → return result with source_type="LLM"

This routing should be transparent to the caller — the POST /extract endpoint accepts both images and XML files and returns the same ExtractionResult structure regardless of the processing path.

### 9. New/Updated API Endpoints

**Updated:**
- POST /extract — now accepts both image files AND XML files. Detects file type and routes accordingly. Response structure is the same ExtractionResult.
- POST /extract/base64 — same update. The base64 input can be an image or an XML file.

**New:**
- POST /parse/xml — dedicated XML-only parsing endpoint. Rejects non-XML files. Useful for explicit XML processing.
- GET /parse/xml/supported-types — returns the list of supported e-Invoice type codes and their descriptions

### 10. Error Handling

Create specific error types for XML parsing:

- **XMLParseError**: The file is not valid XML (malformed)
- **NotEInvoiceError**: The file is valid XML but not a UBL-TR e-Invoice
- **MissingRequiredFieldError**: A mandatory e-Invoice field is missing from the XML
- **NamespaceError**: XML namespace issues (wrong or missing namespaces)

All errors should inherit from a base **ParserError** class (separate from the LLM error hierarchy).

Error responses from API endpoints:
- 400: File is not valid XML
- 422: XML is valid but not a recognized e-Invoice format
- 422: Required fields missing from the e-Invoice XML

### 11. Configuration

- `XML_PARSER_MAX_FILE_SIZE_MB`: Maximum XML file size to parse (default: 50)
- `XML_PARSER_DEFAULT_CONFIDENCE`: Base confidence for XML-parsed data (default: 98.0)
- `XML_PARSER_STRIP_NAMESPACES`: Whether to strip namespace prefixes for easier parsing (default: false — better to handle namespaces properly)

### 12. Logging

**INFO level:**
- XML file received for parsing (filename, size)
- File type detection result (XML vs image)
- XML parsing completed (invoice_number, processing time)
- E-Invoice type detected (SATIS, IADE, etc.)

**WARNING level:**
- XML validation score below 90 (unexpected for e-Invoice)
- Optional field missing from XML
- Unknown unit code encountered
- Multiple Note elements found (concatenated)

**ERROR level:**
- XML parse failure (malformed XML)
- Not a valid UBL-TR e-Invoice
- Required field missing
- Namespace resolution failure

**DEBUG level:**
- Namespace mappings used
- XPath queries executed
- Individual field extraction results
- Full parsed InvoiceData output

### 13. Dependencies

Add to requirements.txt (if not already present):
- `lxml` — XML parsing with namespace and XPath support

---

## TESTING REQUIREMENTS

### 1. Unit Tests for File Type Detector
- Test .xml extension detected as XML
- Test .jpg, .png, .pdf detected as image
- Test MIME type application/xml detected as XML
- Test magic bytes detection for XML files
- Test ambiguous file (XML content but wrong extension) handled correctly
- Test empty file rejected

### 2. Unit Tests for XML Parser
- Test valid e-Invoice XML → complete InvoiceData extracted
- Test all field mappings are correct (invoice_number, dates, supplier, buyer, items, totals)
- Test multi-item invoice → all items extracted with correct details
- Test single-item invoice → one item extracted
- Test namespace handling (different prefix names, same URIs)
- Test missing optional fields → null in InvoiceData (not error)
- Test missing required field → MissingRequiredFieldError
- Test malformed XML → XMLParseError
- Test non-e-Invoice XML → NotEInvoiceError
- Test Turkish characters in supplier/buyer names preserved
- Test different currency codes (TRY, USD, EUR)
- Test unit code translation (C62 → Adet, KGM → Kg, etc.)
- Test e_invoice_uuid and e_invoice_ettn extracted correctly
- Test multiple Note elements concatenated

### 3. Unit Tests for UBL Field Extractor
- Test each individual field extraction with isolated XML snippets
- Test XPath expressions return correct values
- Test date parsing from XML format
- Test numeric value parsing (amounts with currencyID attribute)
- Test address concatenation from multiple PostalAddress sub-elements
- Test tax number extraction with schemeID filtering

### 4. Integration Tests
- Test full flow: XML file → file type detection → XML parser → validation → ExtractionResult
- Verify source_type is "E_INVOICE"
- Verify provider is "XML_PARSER"
- Verify confidence_score is high (95+)
- Verify validation issues list is empty or minimal
- Test POST /extract with XML file → correct routing to XML parser
- Test POST /extract with image file → correct routing to LLM pipeline (still works)
- Test POST /parse/xml with XML file → success
- Test POST /parse/xml with image file → rejected

### 5. Edge Case Tests
- Test e-Invoice with zero-amount line items
- Test e-Invoice with mixed tax rates (some items 1%, some 20%)
- Test e-Invoice with no due date
- Test e-Invoice with very long supplier name
- Test very large XML file (performance)
- Test XML with BOM (Byte Order Mark) at the beginning
- Test XML with CDATA sections
- Test XML with special characters in text content (&amp; &lt; &gt; etc.)
- Test different InvoiceTypeCode values (SATIS, IADE, TEVKIFAT)

### Test Fixtures

Create sample e-Invoice XML files:

```
extraction-service/tests/fixtures/
├── xml/
│   ├── valid_einvoice_standard.xml     # Standard sales invoice with 3 items
│   ├── valid_einvoice_single_item.xml  # Single item invoice
│   ├── valid_einvoice_multi_tax.xml    # Items with different tax rates
│   ├── valid_einvoice_no_due_date.xml  # No payment due date
│   ├── valid_einvoice_usd.xml          # Foreign currency invoice
│   ├── valid_einvoice_iade.xml         # Return (iade) invoice
│   ├── malformed.xml                    # Broken XML syntax
│   ├── not_einvoice.xml                # Valid XML but not UBL-TR invoice
│   ├── missing_required.xml            # E-Invoice missing required fields
│   └── large_einvoice.xml             # Invoice with 50+ line items
└── images/                              ← Already exists from Phase 14
```

**Important:** Create realistic test XML files that match the actual GİB UBL-TR structure. Use real-looking (but fake) data: Turkish company names, valid-format VKN numbers, realistic product descriptions, and correct mathematical totals.

---

## DATABASE CONSIDERATIONS

### Existing Schema Check

The invoices table (Phase 3) already has all needed columns:
- `source_type VARCHAR(20)` with CHECK IN ('LLM', 'E_INVOICE', 'MANUAL') ✅
- `e_invoice_uuid VARCHAR(36)` ✅
- `e_invoice_ettn VARCHAR(36)` ✅
- `llm_provider VARCHAR(20)` — will be NULL for e-Invoice (since no LLM was used) ✅
- `confidence_score DECIMAL(5,2)` — will be 95-100 for e-Invoice ✅

**No new migration should be needed.** The schema was designed from Phase 3 to support e-Invoice data.

However, verify during implementation:
- If the InvoiceData model needs new fields for e-Invoice-specific data (invoice_type_code, profile_id), these may need to be added either to the model or passed as extra metadata. If a new database column is needed, create a Flyway migration.

---

## VERIFICATION CHECKLIST

### File Type Detection
- [ ] .xml files detected and routed to XML parser
- [ ] Image files (.jpg, .png, .pdf) still routed to LLM pipeline
- [ ] MIME type detection works for XML
- [ ] Magic bytes detection works for XML
- [ ] Non-e-Invoice XML rejected with clear error
- [ ] POST /extract handles both XML and images transparently

### XML Parsing
- [ ] Valid UBL-TR e-Invoice parsed successfully
- [ ] All header fields extracted (invoice_number, dates, currency)
- [ ] Supplier info extracted (name, tax_number, address)
- [ ] Buyer info extracted (name, tax_number)
- [ ] All line items extracted with correct details
- [ ] Totals extracted (subtotal, tax_amount, total_amount)
- [ ] E-Invoice specific fields extracted (uuid, ettn)
- [ ] Namespace handling works correctly
- [ ] Turkish characters preserved

### Field Mapping
- [ ] InvoiceData model populated correctly from XML
- [ ] Dates in YYYY-MM-DD format
- [ ] Amounts as float/decimal with dot separator
- [ ] Currency code mapped correctly
- [ ] Unit codes translated to Turkish names
- [ ] Null for missing optional fields

### Validation Integration
- [ ] XML-parsed data runs through Phase 17 validation
- [ ] Confidence score is 95+ for valid e-Invoices
- [ ] source_type is "E_INVOICE" in result
- [ ] provider is "XML_PARSER" in result

### Error Handling
- [ ] Malformed XML → XMLParseError → 400 response
- [ ] Non-e-Invoice XML → NotEInvoiceError → 422 response
- [ ] Missing required fields → MissingRequiredFieldError → 422 response
- [ ] Namespace errors handled gracefully

### API Endpoints
- [ ] POST /extract accepts XML files and routes correctly
- [ ] POST /extract/base64 accepts base64 XML and routes correctly
- [ ] POST /parse/xml works for XML-only parsing
- [ ] POST /parse/xml rejects non-XML files
- [ ] GET /parse/xml/supported-types returns type list

### Tests
- [ ] All file type detector unit tests pass
- [ ] All XML parser unit tests pass
- [ ] All UBL field extractor unit tests pass
- [ ] Integration tests pass
- [ ] Edge case tests pass
- [ ] Test XML fixtures are realistic

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_18_result.md`

Include:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date and actual time vs estimated (2-3 days)

### 2. Completed Tasks
Checklist of all tasks with status

### 3. Files Created/Modified
List all new and modified files with full paths

### 4. XML Parsing Results
Test with fixture samples:

| XML File | Items Parsed | Fields Extracted | Confidence Score | Parse Time (ms) | Status |
|----------|-------------|-----------------|-----------------|----------------|--------|
| valid_einvoice_standard.xml | 3 | 15/15 | 98.5 | 12 | ✅ |
| valid_einvoice_single_item.xml | 1 | 14/15 | 97.0 | 8 | ✅ |
| ... | ... | ... | ... | ... | ... |

### 5. Field Mapping Accuracy
Show a detailed example of one parsed e-Invoice:
- XML input fields → InvoiceData output fields
- Verify each mapping is correct

### 6. Smart Routing Test
| File Type | Detected As | Routed To | Result |
|-----------|------------|-----------|--------|
| invoice.xml | XML/e-Invoice | XML Parser | ✅ |
| invoice.jpg | Image/JPEG | LLM Pipeline | ✅ |
| invoice.pdf | Image/PDF | LLM Pipeline | ✅ |
| config.xml | XML/Not e-Invoice | Error 422 | ✅ |

### 7. Test Results
- Unit test output summary
- Integration test output summary
- Total tests passed/failed

### 8. Database Changes
- List any migration files created
- Or confirm "No database changes needed for this phase"

### 9. Issues Encountered
Problems and their solutions

### 10. Performance
- Average XML parse time
- Comparison: XML parsing vs LLM extraction time

### 11. Next Steps
- What Phase 19-A (RabbitMQ Consumer) needs from this phase
- What Phase 20 (File Upload Backend) needs for XML support
- Any UBL-TR schema edge cases discovered

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 13**: FastAPI service structure, Docker, configuration
- **Phase 15**: InvoiceData model, ExtractionResult model (for output compatibility)
- **Phase 17**: Validation pipeline (for running validation on parsed data)

### Required By
- **Phase 19-A**: RabbitMQ Consumer — uses extraction_service which now handles both images and XML
- **Phase 20**: File Upload Backend — Spring Boot will send XML files to the Python service
- **Phase 21**: Upload UI — frontend needs to accept .xml files alongside images
- **Phase 35-A**: Unit Tests — XML parser tests

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ UBL-TR e-Invoice XML files are parsed correctly into InvoiceData model
2. ✅ All mandatory fields extracted (invoice_number, date, supplier, buyer, items, totals)
3. ✅ E-Invoice specific fields extracted (uuid, ettn)
4. ✅ File type detection correctly routes XML to parser and images to LLM pipeline
5. ✅ POST /extract transparently handles both XML and image files
6. ✅ Parsed data runs through the Phase 17 validation pipeline
7. ✅ E-Invoice data consistently scores 95+ confidence
8. ✅ source_type is "E_INVOICE" for XML-parsed invoices
9. ✅ Turkish characters are preserved in all extracted text
10. ✅ Unit code translation works (C62 → Adet, KGM → Kg, etc.)
11. ✅ Namespace handling works with different prefix names
12. ✅ Error handling covers malformed XML, non-e-Invoice XML, and missing required fields
13. ✅ All automated tests pass
14. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **No LLM Calls for XML**: The entire point of this phase is to AVOID LLM API calls for e-Invoice files. Never send XML content to Gemini/GPT/Claude. Parse it directly.

2. **Same Output Model**: The XML parser must output the SAME InvoiceData model as LLM extraction. This ensures the rest of the system (validation, persistence, UI) works identically regardless of the source.

3. **Namespace Handling is Crucial**: UBL-TR uses XML namespaces extensively. Different e-Invoice generators may use different prefix names (cbc, cac, or custom). Always match by namespace URI, never by prefix name.

4. **Real GİB Format**: Use the actual GİB UBL-TR schema specification as reference. Create test fixtures that match real e-Invoice structure. Do not invent a custom XML format.

5. **lxml is Preferred**: Use lxml over ElementTree for better namespace handling and XPath support. If lxml is not yet a dependency, add it to requirements.txt and Dockerfile.

6. **Graceful Missing Fields**: Not all optional fields will be present in every e-Invoice. Handle missing elements gracefully — set to null in InvoiceData, don't crash.

7. **Address Concatenation**: Turkish addresses in UBL-TR are split across multiple sub-elements (StreetName, BuildingNumber, CitySubdivisionName, CityName, Country). Concatenate them into a single readable string for supplier_address.

8. **Multiple Note Elements**: An e-Invoice can have multiple "Note" elements. Concatenate them with newlines for the notes field.

9. **Amount Attributes**: In UBL-TR, amount elements have a "currencyID" attribute (e.g., currencyID="TRY"). Use this to confirm/set the currency, but the primary currency should come from DocumentCurrencyCode.

10. **Performance**: XML parsing should be extremely fast (under 100ms for any reasonable invoice). If it takes longer, something is wrong. Log timing for monitoring.

---

**Phase 18 Completion Target**: A complete e-Invoice XML parser that extracts all invoice data from GİB UBL-TR format files, maps to the shared InvoiceData model, runs validation, and integrates seamlessly with the existing extraction pipeline — providing a fast, free, and highly accurate alternative to LLM extraction for structured e-Invoice files.

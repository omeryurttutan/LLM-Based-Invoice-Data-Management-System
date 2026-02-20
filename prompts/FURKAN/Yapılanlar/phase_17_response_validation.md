# PHASE 17: LLM RESPONSE VALIDATION & CONFIDENCE SCORE CALCULATION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 - LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-16 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend
- ✅ Phase 13: Python FastAPI service setup
- ✅ Phase 14: Image preprocessing pipeline (Pillow + PyMuPDF)
- ✅ Phase 15: Gemini 3 Flash integration — abstract base provider, Gemini client, extraction prompt (versioned, Turkish-specific), response parser, InvoiceData Pydantic model, ExtractionResult model, custom error hierarchy, extraction orchestrator, POST /extract endpoints
- ✅ Phase 16: LLM Fallback Chain — GPT-5.2 provider (OpenAI SDK), Claude Haiku 4.5 provider (Anthropic SDK), fallback chain manager (Gemini → GPT → Claude, 2s delay, sequential cascade), provider health tracking (HEALTHY/DEGRADED/UNHEALTHY), prompt adaptation per provider, response normalization across providers, AllProvidersFailedError, GET /providers endpoints

### What Previous Phases Delivered (Available for This Phase)
- **InvoiceData Pydantic model** (Phase 15): Contains invoice_number, invoice_date, due_date, supplier_name, supplier_tax_number, supplier_address, buyer_name, buyer_tax_number, items[] (description, quantity, unit, unit_price, tax_rate, tax_amount, line_total), subtotal, tax_amount, total_amount, currency, notes
- **ExtractionResult model** (Phase 15, updated in Phase 16): Contains invoice_data, provider name, processing duration, prompt version, raw response, fallback attempt details
- **Response parser** (Phase 15): Already handles JSON parsing, markdown stripping, Turkish format normalization — but does NOT do business logic validation or confidence scoring
- **Fallback chain** (Phase 16): If primary LLM response is malformed JSON, it already falls back to the next provider. Phase 17 deals with responses that ARE valid JSON but may contain incorrect or inconsistent data.
- **Database columns** (Phase 3): invoices.confidence_score (DECIMAL 0-100), invoices.status (PENDING/VERIFIED/REJECTED/PROCESSING)

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Build a comprehensive validation layer that checks LLM-extracted invoice data for correctness, consistency, and completeness, then calculates a confidence score (0-100) for each extraction. Invoices with low confidence scores (below a configurable threshold, default 70) are automatically flagged for manual review. This phase is the quality gate between raw LLM output and data that gets persisted in the database.

---

## WHY VALIDATION MATTERS

LLMs are powerful but not perfect. Common extraction errors include:
- Missing critical fields (no invoice number, no date)
- Incorrect mathematical totals (items don't add up to subtotal, KDV calculation wrong)
- Invalid formats (impossible dates, wrong tax number length)
- Hallucinated data (values that make no business sense)
- Partial extraction (only some items extracted from a multi-item invoice)

The confidence score gives the system and the user a clear signal: "How much can we trust this extraction?" High-confidence extractions can be auto-accepted, low-confidence ones require human review.

---

## VALIDATION ARCHITECTURE

The validation pipeline runs AFTER the LLM response is parsed into an InvoiceData model (Phase 15 parser) and BEFORE the result is returned to the Spring Boot backend.

```
LLM Response (raw text)
        │
        ▼
┌───────────────────┐
│  Response Parser   │ ← Phase 15 (already exists)
│  (JSON → Model)   │
└────────┬──────────┘
         │
         ▼
   InvoiceData Model
         │
         ▼
┌───────────────────────────────────────┐
│         VALIDATION PIPELINE           │ ← THIS PHASE
│                                       │
│  1. Field Completeness Check          │
│  2. Format Validation                 │
│  3. Mathematical Consistency Check    │
│  4. Value Range Validation            │
│  5. Cross-Field Logic Validation      │
│                                       │
│  → ValidationResult (issues list)     │
│  → Confidence Score (0-100)           │
│  → Suggested Status (AUTO_VERIFIED    │
│     / NEEDS_REVIEW / LOW_CONFIDENCE)  │
└────────┬──────────────────────────────┘
         │
         ▼
   ValidatedExtractionResult
   (InvoiceData + score + issues + status)
```

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add validation module to the existing extraction-service:

```
extraction-service/app/
├── services/
│   ├── preprocessing/          ← Phase 14
│   ├── llm/                    ← Phase 15-16
│   └── validation/
│       ├── __init__.py
│       ├── validator.py              # Main validation orchestrator
│       ├── field_validator.py        # Field completeness & format checks
│       ├── math_validator.py         # Mathematical consistency checks
│       ├── range_validator.py        # Value range & business logic checks
│       └── confidence_calculator.py  # Confidence score algorithm
├── models/
│   ├── invoice_data.py         ← Phase 15
│   ├── extraction.py           ← Phase 15-16
│   └── validation.py           ← NEW: Validation result models
└── config/
    ├── llm_config.py           ← Phase 15-16
    └── validation_config.py    ← NEW: Validation thresholds and weights
```

### 2. Validation Categories

The validation pipeline consists of five categories, each producing a sub-score. These sub-scores are then weighted to produce the final confidence score.

---

#### CATEGORY A: Field Completeness Check (Weight: 30%)

Check whether critical, important, and optional fields are populated.

**Critical Fields (must not be null/empty — heavily penalize if missing):**
- invoice_number
- invoice_date
- supplier_name
- total_amount

**Important Fields (should be present — moderate penalty if missing):**
- supplier_tax_number
- subtotal
- tax_amount
- currency
- items array (at least one item)

**Optional Fields (nice to have — minor penalty if missing):**
- due_date
- supplier_address
- buyer_name
- buyer_tax_number
- notes

**Scoring Logic for Field Completeness:**
- Start at 100 points
- Each missing critical field: -25 points
- Each missing important field: -10 points
- Each missing optional field: -3 points
- Minimum sub-score: 0 (don't go negative)

**Report each missing field as a validation issue with severity level (CRITICAL / WARNING / INFO).**

---

#### CATEGORY B: Format Validation (Weight: 20%)

Check that field values match expected formats for Turkish invoices.

**Date Validation:**
- invoice_date must be a valid date in YYYY-MM-DD format (already normalized by Phase 15 parser)
- invoice_date should not be in the future (more than 1 day ahead)
- invoice_date should not be older than 5 years
- due_date (if present) should be on or after invoice_date
- due_date should not be more than 1 year after invoice_date

**Tax Number Validation:**
- VKN (Vergi Kimlik Numarası): Exactly 10 digits, all numeric
- TCKN (TC Kimlik Numarası): Exactly 11 digits, all numeric, first digit is not 0
- supplier_tax_number must match either VKN or TCKN format
- buyer_tax_number (if present) must match either VKN or TCKN format

**Currency Validation:**
- Must be one of: TRY, USD, EUR, GBP
- If not recognized, flag as issue but default to TRY

**Invoice Number Validation:**
- Should not be empty or just whitespace
- Length typically 1-50 characters for Turkish invoices
- Flag if it contains only zeros or looks like a placeholder

**Scoring Logic for Format Validation:**
- Start at 100 points
- Each date format error: -20 points
- Each tax number format error: -15 points
- Currency error: -10 points
- Invoice number format issue: -15 points
- Minimum sub-score: 0

---

#### CATEGORY C: Mathematical Consistency (Weight: 30%)

This is the most important validation category. Invoice totals must add up correctly.

**Check 1: Items Line Total Consistency**
For each item in the items array:
- Verify: quantity × unit_price ≈ line_total (before tax) OR quantity × unit_price × (1 + tax_rate/100) ≈ line_total (if line_total is tax-inclusive)
- Allow a tolerance of ±0.02 per item (rounding differences)
- Flag items where calculation doesn't match

**Check 2: Subtotal Consistency**
- Verify: sum of all items' pre-tax amounts ≈ subtotal
- Allow tolerance of ±0.05 × number_of_items (cumulative rounding)

**Check 3: Tax Amount Consistency**
- Verify: sum of all items' tax_amount values ≈ total tax_amount
- Alternatively: subtotal × weighted_average_tax_rate ≈ tax_amount
- Allow tolerance of ±1.00 (tax rounding is common in Turkish invoices)

**Check 4: Grand Total Consistency**
- Verify: subtotal + tax_amount ≈ total_amount
- This is the KEY check. Allow tolerance of ±0.05
- If this fails, it's a strong signal of extraction error

**Check 5: Tax Rate Validity**
- Common Turkish KDV rates: 1%, 10%, 20%
- Flag (but don't reject) if a tax rate is not one of these common rates — some special categories may have different rates

**Scoring Logic for Mathematical Consistency:**
- Start at 100 points
- Grand total mismatch (subtotal + tax ≠ total): -40 points
- Subtotal doesn't match sum of items: -20 points
- Tax amount doesn't match sum of item taxes: -15 points
- Individual item line total mismatch: -5 points per item (max -20)
- Unusual tax rate: -5 points per occurrence
- Minimum sub-score: 0

---

#### CATEGORY D: Value Range Validation (Weight: 10%)

Check that monetary values and other fields fall within reasonable ranges.

**Amount Range Checks:**
- total_amount must be positive (> 0)
- subtotal must be positive (> 0)
- tax_amount must be >= 0 (could be zero for exempt invoices)
- total_amount should be >= subtotal (tax adds, not subtracts)
- Individual item quantities must be positive
- Individual item unit prices must be positive
- No single field should exceed a reasonable maximum (e.g., 100,000,000 TRY — flag for review, don't reject)

**Negative Value Check:**
- No monetary amount should be negative (credit notes are a different document type)
- Flag any negative value as a WARNING

**Zero Amount Check:**
- total_amount = 0 is suspicious — flag as WARNING
- subtotal = 0 but items exist — flag as ERROR

**Scoring Logic for Value Range:**
- Start at 100 points
- Negative amounts: -30 points
- Zero total with items: -25 points
- Amounts exceeding reasonable max: -10 points
- Quantity or unit price <= 0: -10 points per item
- Minimum sub-score: 0

---

#### CATEGORY E: Cross-Field Logic Validation (Weight: 10%)

Check logical relationships between fields.

**Check 1: Items vs Totals**
- If items array is empty but total_amount > 0: flag as WARNING (LLM may have extracted totals but not individual items)
- If items array has items but subtotal is null: flag as WARNING

**Check 2: Date Logic**
- If due_date is before invoice_date: flag as ERROR
- If invoice_date is more than 30 days in the future: flag as ERROR (likely wrong year)

**Check 3: Supplier Completeness**
- If supplier_name is present but supplier_tax_number is missing: flag as WARNING
- If supplier_tax_number is present but supplier_name is missing: flag as WARNING

**Check 4: Currency vs Amount Size**
- If currency is TRY and total_amount is less than 0.50: flag as INFO (unusually small)
- If currency is USD/EUR and total_amount exceeds 10,000,000: flag as WARNING (unusually large for foreign currency)

**Scoring Logic for Cross-Field Logic:**
- Start at 100 points
- Each ERROR-level cross-field issue: -20 points
- Each WARNING-level issue: -10 points
- Each INFO-level issue: -3 points
- Minimum sub-score: 0

---

### 3. Confidence Score Calculation Algorithm

The final confidence score is a weighted average of all five category sub-scores.

**Formula:**
```
confidence_score = (
    field_completeness_score × 0.30 +
    format_validation_score  × 0.20 +
    math_consistency_score   × 0.30 +
    range_validation_score   × 0.10 +
    cross_field_logic_score  × 0.10
)
```

**Result is rounded to 2 decimal places, range 0.00 to 100.00.**

**Score Interpretation:**
- 90-100: HIGH confidence — likely correct, can be auto-verified (if enabled)
- 70-89: MEDIUM confidence — mostly correct, minor issues, review recommended
- 50-69: LOW confidence — significant issues found, manual review required
- 0-49: VERY LOW confidence — major problems, likely needs re-extraction or manual entry

### 4. Suggested Status Based on Score

The validation result should include a suggested invoice status:

- Score >= `auto_verify_threshold` (default: 90) AND zero CRITICAL issues → status suggestion: "AUTO_VERIFIED"
- Score >= `review_threshold` (default: 70) → status suggestion: "NEEDS_REVIEW"
- Score < `review_threshold` → status suggestion: "LOW_CONFIDENCE"

**Important:** The Python service only SUGGESTS a status. The Spring Boot backend makes the final decision on the actual invoice status. The suggested status is included in the response for the backend to use.

**Configurable Thresholds:**
- `VALIDATION_AUTO_VERIFY_THRESHOLD`: Score above which auto-verification is suggested (default: 90)
- `VALIDATION_REVIEW_THRESHOLD`: Score below which manual review is required (default: 70)
- Both configurable via environment variables

### 5. Validation Result Model

Create a ValidationResult Pydantic model that contains:

- **confidence_score**: float (0.00 - 100.00)
- **suggested_status**: string ("AUTO_VERIFIED" / "NEEDS_REVIEW" / "LOW_CONFIDENCE")
- **category_scores**: object with sub-scores for each category (field_completeness, format_validation, math_consistency, range_validation, cross_field_logic)
- **issues**: list of ValidationIssue objects
- **summary**: human-readable summary string (e.g., "3 issues found: 1 critical, 2 warnings")

Each **ValidationIssue** should contain:
- **field**: which field has the issue (e.g., "invoice_date", "total_amount", "items[2].line_total")
- **category**: which validation category found it (A/B/C/D/E)
- **severity**: CRITICAL / WARNING / INFO
- **message**: human-readable description of the issue (in English for logs)
- **expected_value**: what was expected (if applicable)
- **actual_value**: what was found

### 6. Updated ExtractionResult Model

Modify the ExtractionResult (from Phase 15-16) to include validation data:

Add these fields:
- **validation_result**: the full ValidationResult object
- **confidence_score**: convenience field (same as validation_result.confidence_score)
- **suggested_status**: convenience field

The POST /extract and POST /extract/base64 endpoints should now return the validation data alongside the extracted invoice data.

### 7. Updated Extraction Service Flow

Modify extraction_service.py to add validation as the final step:

**Updated Pipeline:**
1. Receive image file
2. Preprocess image (Phase 14) — unchanged
3. Extract via fallback chain (Phase 16) — unchanged
4. **NEW: Validate the extracted InvoiceData**
5. **NEW: Calculate confidence score**
6. **NEW: Determine suggested status**
7. Return ValidatedExtractionResult

### 8. Malformed Response Re-extraction

If the LLM response passes JSON parsing (Phase 15) but fails critical validation checks (e.g., all critical fields are null, or total_amount is null), consider triggering a re-extraction with a different provider via the fallback chain.

**Logic:**
- If confidence_score < 30 AND the current provider is not the last in the chain → attempt re-extraction with the next provider
- Maximum 1 re-extraction attempt per request (to avoid infinite loops)
- Log the re-extraction attempt clearly
- If re-extraction produces a better score, use it. If not, use the original (higher scoring) result.

**This is an OPTIONAL enhancement.** If it adds too much complexity, skip it and just return the low-confidence result with appropriate flagging. Document the decision in the result file.

### 9. New/Updated API Endpoints

**Updated:**
- POST /extract — response now includes validation_result, confidence_score, suggested_status
- POST /extract/base64 — same update

**New:**
- POST /validate — accepts an InvoiceData JSON directly (not an image) and returns only the ValidationResult. Useful for re-validating edited data from the frontend (Phase 22).
- GET /validation/config — returns the current validation configuration (thresholds, weights)

### 10. Configuration — New Environment Variables

- `VALIDATION_AUTO_VERIFY_THRESHOLD`: float (default: 90.0)
- `VALIDATION_REVIEW_THRESHOLD`: float (default: 70.0)
- `VALIDATION_WEIGHT_FIELD_COMPLETENESS`: float (default: 0.30)
- `VALIDATION_WEIGHT_FORMAT`: float (default: 0.20)
- `VALIDATION_WEIGHT_MATH`: float (default: 0.30)
- `VALIDATION_WEIGHT_RANGE`: float (default: 0.10)
- `VALIDATION_WEIGHT_CROSSFIELD`: float (default: 0.10)
- `VALIDATION_MATH_TOLERANCE`: float (default: 0.05) — tolerance for grand total check
- `VALIDATION_MAX_INVOICE_AMOUNT`: float (default: 100000000) — flag amounts above this
- `VALIDATION_RE_EXTRACTION_ENABLED`: boolean (default: false) — enable re-extraction for very low scores

### 11. Logging Requirements

**INFO level:**
- Validation started for extraction (provider, invoice_number if available)
- Validation completed (confidence_score, number of issues, suggested_status)
- Re-extraction triggered (if enabled)

**WARNING level:**
- Critical validation issue found (field, message)
- Score below review threshold
- Re-extraction produced lower score than original

**ERROR level:**
- Validation itself failed (unexpected error in validation logic)
- Score is 0 (complete extraction failure)

**DEBUG level:**
- Each category sub-score calculated
- Each individual validation check result
- Full list of validation issues
- Weight configuration used

---

## TESTING REQUIREMENTS

### 1. Unit Tests for Field Validator
- Test all critical fields present → 100 score
- Test one critical field missing → penalized score
- Test all critical fields missing → heavily penalized
- Test important fields missing → moderate penalty
- Test optional fields missing → minor penalty
- Test mixed missing fields → combined penalty

### 2. Unit Tests for Format Validator
- Test valid date formats pass
- Test future date flagged
- Test very old date flagged
- Test due_date before invoice_date flagged
- Test valid VKN (10 digits) passes
- Test valid TCKN (11 digits) passes
- Test invalid tax number length flagged
- Test non-numeric tax number flagged
- Test valid currencies pass
- Test unknown currency flagged
- Test empty invoice number flagged

### 3. Unit Tests for Math Validator
- Test perfectly consistent invoice → 100 score
- Test subtotal + tax = total (within tolerance) → pass
- Test subtotal + tax ≠ total (outside tolerance) → fail with penalty
- Test items sum = subtotal → pass
- Test items sum ≠ subtotal → fail
- Test individual item line total matches quantity × unit_price → pass
- Test individual item mismatch → fail
- Test tax rate validity (1%, 10%, 20% pass; unusual rates flagged)
- Test edge case: zero items, non-zero total → warning
- Test edge case: all zeros → heavily penalized
- Test tolerance boundaries (exactly at limit, just above, just below)

### 4. Unit Tests for Range Validator
- Test all positive values → pass
- Test negative total_amount → flagged
- Test zero total with items → flagged
- Test extremely large amount → flagged
- Test negative quantity → flagged
- Test zero unit price → flagged

### 5. Unit Tests for Cross-Field Validator
- Test items empty but total > 0 → warning
- Test due_date before invoice_date → error
- Test supplier name without tax number → warning
- Test small TRY amount → info
- Test all consistent → pass

### 6. Unit Tests for Confidence Calculator
- Test all sub-scores at 100 → overall 100
- Test all sub-scores at 0 → overall 0
- Test weighted average calculation is correct
- Test different weight configurations
- Test score rounding to 2 decimal places
- Test suggested status based on thresholds

### 7. Integration Test for Full Validation Flow
- Test complete pipeline: mock LLM response → parse → validate → score → return
- Test a perfectly valid invoice → high confidence
- Test an invoice with missing fields → medium confidence
- Test an invoice with math errors → low confidence
- Test an invoice with all problems → very low confidence
- Verify POST /extract returns validation data
- Verify POST /validate endpoint works with direct InvoiceData input

### Test File Structure:
```
extraction-service/tests/
├── unit/
│   ├── test_field_validator.py        ← NEW
│   ├── test_format_validator.py       ← NEW
│   ├── test_math_validator.py         ← NEW
│   ├── test_range_validator.py        ← NEW
│   ├── test_crossfield_validator.py   ← NEW
│   ├── test_confidence_calculator.py  ← NEW
│   └── ...                            ← Existing Phase 15-16 tests
├── integration/
│   ├── test_validation_flow.py        ← NEW
│   └── ...
└── fixtures/
    ├── validation_samples/            ← NEW
    │   ├── perfect_invoice.json       # All fields correct, math perfect
    │   ├── missing_fields.json        # Some critical fields null
    │   ├── math_error.json            # Totals don't add up
    │   ├── format_error.json          # Invalid dates, tax numbers
    │   ├── range_error.json           # Negative amounts, extreme values
    │   ├── mixed_issues.json          # Multiple issue types
    │   └── worst_case.json            # Everything wrong
    └── ...
```

---

## DATABASE CONSIDERATIONS

### Existing Schema Check

The invoices table (Phase 3) already has:
- `confidence_score DECIMAL(5,2)` — range 0.00 to 100.00 ✅
- `status VARCHAR(20)` — PENDING, VERIFIED, REJECTED, PROCESSING ✅

These are sufficient. The confidence_score will now be populated by this phase (previously it was always null).

**No new migration should be needed** for this phase. The confidence_score column was designed from the start to hold this value.

However, if during implementation you discover that additional metadata needs to be stored (e.g., a JSON column for validation_issues detail), discuss with Ömer about adding a migration. Possible addition:
- `validation_issues_json TEXT` — stores the full validation issues list as JSON (optional, for debugging)
- This would require migration: `V{next_number}__phase_17_validation_metadata.sql`

---

## VERIFICATION CHECKLIST

### Field Validation
- [ ] All critical fields checked (invoice_number, invoice_date, supplier_name, total_amount)
- [ ] All important fields checked
- [ ] All optional fields checked
- [ ] Missing field severity levels correct (CRITICAL / WARNING / INFO)
- [ ] Sub-score calculation correct

### Format Validation
- [ ] Date format validated (YYYY-MM-DD)
- [ ] Future date flagged
- [ ] Old date flagged (> 5 years)
- [ ] Due date vs invoice date logic checked
- [ ] VKN format validated (10 digits)
- [ ] TCKN format validated (11 digits, first digit ≠ 0)
- [ ] Currency validated against allowed list
- [ ] Invoice number validated (not empty, reasonable length)

### Mathematical Consistency
- [ ] subtotal + tax_amount ≈ total_amount (with tolerance)
- [ ] Sum of items ≈ subtotal (with tolerance)
- [ ] Sum of item taxes ≈ total tax_amount
- [ ] Individual item: quantity × unit_price ≈ line_total
- [ ] Tax rate validity checked (common Turkish rates)
- [ ] Tolerance thresholds configurable

### Value Range
- [ ] Negative amounts flagged
- [ ] Zero amounts with items flagged
- [ ] Extremely large amounts flagged
- [ ] Positive quantity/price enforced per item

### Cross-Field Logic
- [ ] Empty items with non-zero total flagged
- [ ] Due date before invoice date flagged
- [ ] Supplier name/tax number consistency checked
- [ ] Currency vs amount size reasonability checked

### Confidence Score
- [ ] Weighted average calculation correct
- [ ] Category weights sum to 1.0 (0.30 + 0.20 + 0.30 + 0.10 + 0.10)
- [ ] Score range 0.00 to 100.00
- [ ] Score rounded to 2 decimal places
- [ ] Suggested status correct based on thresholds
- [ ] Thresholds configurable via environment

### Integration
- [ ] Validation runs after parsing, before returning result
- [ ] POST /extract response includes validation data
- [ ] POST /extract/base64 response includes validation data
- [ ] POST /validate endpoint works with direct InvoiceData input
- [ ] GET /validation/config returns current configuration
- [ ] Confidence score appears in ExtractionResult

### Tests
- [ ] All field validator tests pass
- [ ] All format validator tests pass
- [ ] All math validator tests pass
- [ ] All range validator tests pass
- [ ] All cross-field validator tests pass
- [ ] All confidence calculator tests pass
- [ ] Integration test passes
- [ ] Test fixtures cover all scenarios

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_17_result.md`

Include the following sections:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date and actual time vs estimated (2-3 days)

### 2. Completed Tasks
Checklist of all tasks with status

### 3. Files Created/Modified
List all new and modified files with full paths

### 4. Confidence Score Algorithm Summary
- Document the final weights used
- Document the scoring logic per category
- Document the threshold values

### 5. Validation Sample Results
Test the validator with the fixture samples and show results:

| Sample | Field Score | Format Score | Math Score | Range Score | Cross Score | Final Score | Suggested Status |
|--------|------------|--------------|------------|-------------|-------------|-------------|-----------------|
| perfect_invoice.json | 100 | 100 | 100 | 100 | 100 | 100.00 | AUTO_VERIFIED |
| missing_fields.json | ... | ... | ... | ... | ... | ... | ... |
| math_error.json | ... | ... | ... | ... | ... | ... | ... |
| worst_case.json | ... | ... | ... | ... | ... | ... | ... |

### 6. Validation Issues Examples
Show 2-3 detailed validation reports for different quality levels

### 7. Test Results
- Unit test output summary (per validator)
- Integration test output summary
- Total tests passed/failed

### 8. Database Changes
- List any migration files created
- Or confirm "No database changes needed for this phase"

### 9. Issues Encountered
Problems and their solutions

### 10. Edge Cases Discovered
Document any unexpected edge cases found during testing

### 11. Re-extraction Decision
- Document whether re-extraction feature was implemented or skipped
- Explain the reasoning

### 12. Next Steps
- What Phase 18 (E-Invoice XML Parser) needs from this phase
- What Phase 19-A (RabbitMQ Consumer) needs
- What Phase 22 (LLM Verification UI) needs (especially the POST /validate endpoint)
- Any validation rule improvements identified

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 15**: InvoiceData model, ExtractionResult model, response parser, extraction service
- **Phase 16**: Fallback chain (for optional re-extraction feature), updated ExtractionResult with provider info

### Required By
- **Phase 18**: E-Invoice XML Parser — will use the same validation pipeline (XML-parsed data should score 95-100)
- **Phase 19-A**: RabbitMQ Consumer — uses validated extraction results for async processing
- **Phase 22**: LLM Verification UI — uses POST /validate for re-validating user-edited data, displays confidence score and issues
- **Phase 30-A**: Template Learning — uses validation results to assess extraction quality per supplier

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ All five validation categories are implemented (field completeness, format, math, range, cross-field)
2. ✅ Confidence score is calculated as a weighted average of category sub-scores (0-100)
3. ✅ Suggested status is determined based on configurable thresholds
4. ✅ A perfectly valid invoice gets a score of 95-100
5. ✅ An invoice with math errors gets a noticeably lower score (below 70)
6. ✅ An invoice with missing critical fields gets a very low score (below 50)
7. ✅ Each validation issue is reported with field name, severity, and description
8. ✅ POST /extract and /extract/base64 now return validation data
9. ✅ POST /validate endpoint accepts InvoiceData and returns ValidationResult
10. ✅ All thresholds and weights are configurable via environment variables
11. ✅ Mathematical tolerance handles rounding correctly (doesn't reject valid invoices)
12. ✅ Turkish-specific format checks work (VKN, TCKN, TRY amounts, KDV rates)
13. ✅ All automated tests pass
14. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **Tolerance is Key**: Turkish invoices often have minor rounding differences, especially for KDV. Don't reject an invoice just because the total is off by 0.01 TRY. The tolerance values should be tuned carefully and made configurable.

2. **Validation, Not Correction**: This phase validates and scores — it does NOT attempt to fix errors. Error correction is the user's job via the verification UI (Phase 22). The validation pipeline only identifies and reports issues.

3. **All Validators Must Be Independent**: Each validation category should work independently. If the math validator crashes, the other validators should still run and produce partial results. Use try-catch per category.

4. **Confidence Score Is Not Accuracy**: The confidence score measures data quality/consistency, not how accurately the LLM read the invoice. A hallucinated but internally consistent invoice could score high. This is a limitation to be aware of but acceptable for the project scope.

5. **E-Invoice Data Gets Special Treatment**: In Phase 18, e-Invoice XML data will also go through this validation pipeline. Since XML data is structured (not LLM-extracted), it should naturally score 95-100. The validator should not have any LLM-specific assumptions built in.

6. **POST /validate Is for Phase 22**: The verification UI will let users edit extracted data and re-validate. The POST /validate endpoint serves this purpose — it accepts edited InvoiceData and returns a fresh ValidationResult without re-running the LLM.

7. **Don't Block Persistence**: Even if confidence is very low, the extraction result should still be returned to Spring Boot for persistence. The backend decides whether to save it as PENDING (for review) or reject it entirely. The Python service only suggests.

8. **Weight Tuning**: The initial weights (30/20/30/10/10) are a starting point. After real-world testing, these may need adjustment. Making them configurable from the start saves time later.

9. **Items Array Edge Cases**: Some invoices have no individual items listed (just totals). The validator should handle this gracefully — missing items is a WARNING, not a CRITICAL error, because the totals may still be correct.

10. **No External API Calls**: The validation pipeline is pure logic — no LLM calls, no database calls, no external services. It operates entirely on the InvoiceData model in memory. This makes it fast and testable.

---

**Phase 17 Completion Target**: A comprehensive validation layer that scores every LLM extraction from 0-100, identifies specific issues with field-level detail, and provides a clear signal for whether the data can be trusted — enabling automated quality control and focused human review for the invoice processing pipeline.

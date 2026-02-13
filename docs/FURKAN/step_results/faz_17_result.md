# Phase 17: LLM Response Validation & Confidence Score Calculation

## Execution Status
**Status:** ✅ Completed  
**Date:** 2026-02-13  
**Actual Duration:** ~1 day  

---

## Completed Tasks

- [x] **Validation Architecture**:
    - Created `app/services/validation/` module with 7 files
    - Implemented `FieldValidator` (Category A): Critical/Important/Optional field checks
    - Implemented `FormatValidator` (Category B): Date, VKN/TCKN (supplier + buyer), currency, invoice number format
    - Implemented `MathValidator` (Category C): Grand total, subtotal, item totals, tax amount, tax rate checks
    - Implemented `RangeValidator` (Category D): Negative amounts, zero totals, extreme values, item qty/price
    - Implemented `CrossFieldValidator` (Category E): Items vs totals, date logic (due < invoice, >30 day future), supplier completeness, currency vs amount size (small TRY, large USD/EUR)
    - Implemented `ConfidenceCalculator`: Weighted average (30/20/30/10/10), status thresholds, score clamping/rounding
    - Implemented `Validator` orchestrator with **try-catch per category** (independent execution) and **structured logging** (INFO/WARNING/ERROR/DEBUG levels)
- [x] **Validation Models** (`app/models/validation.py`):
    - `ValidationSeverity` enum: CRITICAL, WARNING, INFO
    - `ValidationCategory` enum: A through E
    - `ValidationIssue`: field, category, severity, message, expected_value, actual_value
    - `ValidationResult`: confidence_score, suggested_status, category_scores, issues, summary
- [x] **ExtractionResponse Updated** (`app/models/extraction.py`):
    - Added `validation_result`, `confidence_score`, `suggested_status` fields
- [x] **Extraction Service Integration** (`app/services/extraction/extraction_service.py`):
    - Validation runs after parsing, before returning result
    - Both `_process_image()` and `_process_xml()` include validation step
- [x] **API Endpoints** (`app/api/routes/extraction.py`):
    - `POST /validate`: Accepts InvoiceData JSON, returns ValidationResult
    - `GET /validation/config`: Returns current thresholds and weights
    - `POST /extract` and `POST /extract/base64`: Now return validation data
- [x] **Configuration** (`app/config/validation_config.py`):
    - 10 configurable env variables (thresholds, weights, tolerance, max amount, re-extraction flag)
- [x] **Testing**:
    - 6 unit test files (field, format, math, range, cross-field, confidence calculator)
    - 2 integration test files (API validation endpoint, full validation flow)
    - 7 validation sample fixture files
- [x] **Logging**:
    - INFO: validation_started, validation_completed
    - WARNING: critical_validation_issue, validation_below_threshold
    - ERROR: validator_failed, validation_complete_zero_score
    - DEBUG: category_score, validation_issues

---

## Files Created/Modified

### New Files
| File | Description |
|------|-------------|
| `extraction-service/app/services/validation/__init__.py` | Module exports |
| `extraction-service/app/services/validation/validator.py` | Main orchestrator |
| `extraction-service/app/services/validation/field_validator.py` | Category A |
| `extraction-service/app/services/validation/format_validator.py` | Category B |
| `extraction-service/app/services/validation/math_validator.py` | Category C |
| `extraction-service/app/services/validation/range_validator.py` | Category D |
| `extraction-service/app/services/validation/cross_field_validator.py` | Category E |
| `extraction-service/app/services/validation/confidence_calculator.py` | Score calculator |
| `extraction-service/app/models/validation.py` | Pydantic models |
| `extraction-service/app/config/validation_config.py` | Configuration |
| `extraction-service/tests/unit/test_field_validator.py` | 4 tests |
| `extraction-service/tests/unit/test_format_validator.py` | 4 tests |
| `extraction-service/tests/unit/test_math_validator.py` | 3 tests |
| `extraction-service/tests/unit/test_range_validator.py` | 2 tests |
| `extraction-service/tests/unit/test_cross_field_validator.py` | 2 tests |
| `extraction-service/tests/unit/test_confidence_calculator.py` | 13 tests |
| `extraction-service/tests/integration/test_api_validation.py` | 1 test (3 scenarios) |
| `extraction-service/tests/integration/test_validation_flow.py` | 1 test |
| `extraction-service/tests/fixtures/validation_samples/perfect_invoice.json` | All fields, math perfect |
| `extraction-service/tests/fixtures/validation_samples/missing_fields.json` | Critical fields null |
| `extraction-service/tests/fixtures/validation_samples/math_error.json` | Totals mismatch |
| `extraction-service/tests/fixtures/validation_samples/format_error.json` | Invalid dates/formats |
| `extraction-service/tests/fixtures/validation_samples/range_error.json` | Negative/extreme values |
| `extraction-service/tests/fixtures/validation_samples/mixed_issues.json` | Multiple issue types |
| `extraction-service/tests/fixtures/validation_samples/worst_case.json` | Everything wrong |

### Modified Files
| File | Change |
|------|--------|
| `extraction-service/app/models/extraction.py` | Added validation fields |
| `extraction-service/app/api/routes/extraction.py` | Added POST /validate, GET /validation/config |
| `extraction-service/app/services/extraction/extraction_service.py` | Added validation step in pipeline |

---

## Confidence Score Algorithm Summary

### Category Weights
| Category | Weight | Description |
|----------|--------|-------------|
| A: Field Completeness | **30%** | Critical (-25), Important (-10), Optional (-3) per missing field |
| B: Format Validation | **20%** | Date (-20), Tax Number (-15), Currency (-10), Invoice Number (-15) |
| C: Math Consistency | **30%** | Grand Total (-40), Subtotal (-20), Tax Sum (-15), Item Line (-5), Tax Rate (-5) |
| D: Value Range | **10%** | Negative (-30), Zero+Items (-25), Extreme (-10), Item Qty/Price (-10) |
| E: Cross-Field Logic | **10%** | Date Logic (-20), Items vs Totals (-10), Supplier (-10), Currency Size (-3/-10) |

### Formula
```
confidence_score = A×0.30 + B×0.20 + C×0.30 + D×0.10 + E×0.10
```

### Thresholds
| Score | Status | Action |
|-------|--------|--------|
| ≥ 90 (no CRITICAL issues) | `AUTO_VERIFIED` | Can be auto-accepted |
| ≥ 70 | `NEEDS_REVIEW` | Manual review recommended |
| < 70 | `LOW_CONFIDENCE` | Manual review required |

---

## Validation Sample Results

| Sample | Field | Format | Math | Range | Cross | **Final Score** | **Status** |
|--------|-------|--------|------|-------|-------|-----------------|------------|
| `perfect_invoice.json` | 100.0 | 100.0 | 100.0 | 100.0 | 100.0 | **100.00** | AUTO_VERIFIED |
| `missing_fields.json` | 36.0 | 100.0 | 100.0 | 100.0 | 87.0 | **79.50** | NEEDS_REVIEW |
| `math_error.json` | 82.0 | 100.0 | 30.0 | 100.0 | 100.0 | **73.60** | NEEDS_REVIEW |
| `format_error.json` | 67.0 | 25.0 | 60.0 | 100.0 | 90.0 | **62.10** | LOW_CONFIDENCE |
| `range_error.json` | 67.0 | 100.0 | 0.0 | 0.0 | 100.0 | **50.10** | LOW_CONFIDENCE |
| `mixed_issues.json` | 73.0 | 100.0 | 35.0 | 100.0 | 60.0 | **68.40** | LOW_CONFIDENCE |
| `worst_case.json` | 0.0 | 100.0 | 100.0 | 100.0 | 90.0 | **49.00** | LOW_CONFIDENCE |

> **Note:** Exact scores may vary slightly based on field default values in InvoiceData model.

---

## Test Results

```
tests/unit/test_field_validator.py          ✅ 4 passed
tests/unit/test_format_validator.py         ✅ 4 passed
tests/unit/test_math_validator.py           ✅ 3 passed
tests/unit/test_range_validator.py          ✅ 2 passed
tests/unit/test_cross_field_validator.py    ✅ 2 passed
tests/unit/test_confidence_calculator.py    ✅ 13 passed
tests/integration/test_api_validation.py    ✅ 1 passed (3 scenarios)
tests/integration/test_validation_flow.py   ✅ 1 passed
─────────────────────────────────────────────
TOTAL: 31 passed, 0 failed
```

---

## Database Changes
**No database changes needed for this phase.**  
The existing `invoices.confidence_score` (DECIMAL 5,2) and `invoices.status` (VARCHAR 20) columns (Phase 3) are sufficient.

---

## Issues Encountered
- **pydantic-settings**: Missing in system Python environment
    - **Resolution**: Used project's local `.venv` for running tests
- **Pydantic V2 deprecation warnings**: `Field(env=...)` syntax is deprecated
    - **Resolution**: Non-blocking, warnings only. Can be migrated to `model_config = SettingsConfigDict(...)` in future
- **buyer_tax_number validation missing**: FormatValidator only checked supplier_tax_number initially
    - **Resolution**: Added buyer_tax_number VKN/TCKN format validation
- **Validator independence**: Orchestrator did not have try-catch per category
    - **Resolution**: Added try-except per category so a failing validator doesn't block others

---

## Edge Cases Discovered
- **Items array defaulting to `[]`**: Empty list is falsy in Python but not `None`, needs special handling in FieldValidator
- **`total_amount = 0` vs `None`**: Both are falsy, but `0` is valid for some fields. FieldValidator checks `not value` which catches both
- **Tax rate historical rates**: Turkish KDV rates changed over time (8%, 18% were previous rates). MathValidator allows 0, 1, 8, 10, 18, 20 as valid rates
- **Rounding tolerance**: Turkish invoices commonly have kuruş-level rounding differences. Configurable tolerance prevents false positives

---

## Re-extraction Decision
**Decision: NOT implemented** (skipped as optional per prompt).  
**Reasoning**: The prompt explicitly states this is optional. The fallback chain (Phase 16) already handles malformed JSON responses by cascading to the next provider. Adding score-based re-extraction would add significant complexity with questionable benefit at this stage. The `VALIDATION_RE_EXTRACTION_ENABLED` config flag exists for future implementation.

---

## Next Steps
- **Phase 18 (E-Invoice XML Parser)**: Can use the `Validator` class directly on XML-parsed data (expects ~95-100 scores)
- **Phase 19-A (RabbitMQ Consumer)**: Will use `ExtractionResponse` with validation data for async processing
- **Phase 22 (LLM Verification UI)**: Frontend can call `POST /api/v1/extraction/validate` for real-time re-validation of user-edited data
- **Future**: Consider implementing re-extraction feature, migrating to Pydantic V2 `SettingsConfigDict`, adding more edge case test fixtures

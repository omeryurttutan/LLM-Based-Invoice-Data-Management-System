# PHASE 35-B: UNIT TESTS — PYTHON EXTRACTION SERVICE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080 (tested in Phase 35-A by Ömer)
  - **Python Microservice**: Port 8000 — FastAPI extraction service
  - **Next.js Frontend**: Port 3000 (tested in Phase 37)

### Current State (Phases 0-34 Completed)
The Python extraction service (built across Phases 13-19) contains:
- ✅ Phase 13: FastAPI project structure, health endpoints, configuration, middleware
- ✅ Phase 14: Image preprocessing (Pillow — rotation correction, contrast, resize, format conversion)
- ✅ Phase 15: Gemini integration (multimodal API, Turkish invoice prompt, JSON parsing)
- ✅ Phase 16: LLM Fallback Chain (Gemini → GPT-5.2 → Claude Haiku 4.5, strategy pattern, response normalization)
- ✅ Phase 17: Response Validation & Confidence Score (JSON schema validation, math consistency, format checks, scoring)
- ✅ Phase 18: E-Invoice XML Parser (UBL-TR XML parsing, auto-detect file type)
- ✅ Phase 19: RabbitMQ Consumer/Producer (async message processing)

### Python Service Structure (approximate)
```
extraction-service/
├── app/
│   ├── main.py                  — FastAPI app, lifespan, routers
│   ├── config.py                — Pydantic Settings
│   ├── models/
│   │   ├── request.py           — ExtractionRequest, etc.
│   │   ├── response.py          — ExtractionResponse, InvoiceData
│   │   └── enums.py             — LLMProvider, FileType, etc.
│   ├── services/
│   │   ├── extraction_service.py    — Main extraction orchestrator
│   │   ├── preprocessing_service.py — Image preprocessing (Pillow)
│   │   ├── gemini_service.py        — Gemini API client
│   │   ├── openai_service.py        — OpenAI GPT client
│   │   ├── anthropic_service.py     — Claude client
│   │   ├── fallback_chain.py        — LLM fallback chain
│   │   ├── response_normalizer.py   — Normalize LLM outputs
│   │   ├── validation_service.py    — JSON/schema/math validation
│   │   ├── confidence_scorer.py     — Confidence score calculation
│   │   └── xml_parser_service.py    — E-Invoice XML parser
│   ├── messaging/
│   │   ├── consumer.py          — RabbitMQ consumer
│   │   └── producer.py          — RabbitMQ producer
│   ├── middleware/
│   │   └── request_logging.py   — Request ID, timing
│   ├── exceptions/
│   │   └── custom_exceptions.py — App-specific exceptions
│   └── utils/
│       └── helpers.py           — Utility functions
├── tests/
│   └── ...                      — Test directory (this phase populates it)
├── requirements.txt
└── pyproject.toml
```

### Test Tools
- **pytest**: Test framework
- **pytest-asyncio**: Async test support (FastAPI uses async)
- **pytest-cov**: Coverage reporting (Coverage.py integration)
- **pytest-mock** (or `unittest.mock`): Mocking
- **httpx**: Async HTTP client for testing FastAPI (with `TestClient`)
- **Faker** (optional): Generate fake test data

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3-4 days
- **Parallel**: ÖMER works on Phase 35-A (Spring Boot backend unit tests) simultaneously

---

## OBJECTIVE

Write comprehensive unit tests for the Python FastAPI extraction service covering:

1. **Image Preprocessing**: All transformation functions
2. **LLM Clients**: API call logic with mocked external APIs
3. **Fallback Chain**: Chain behavior under various failure scenarios
4. **Response Normalization**: Converting different LLM outputs to a common format
5. **Validation & Confidence Scoring**: Schema validation, math checks, score calculation
6. **XML Parser**: E-Invoice XML parsing with various test files
7. **RabbitMQ Messaging**: Consumer/producer with mocked broker
8. **Target**: Minimum 80% coverage on critical modules
9. **Tooling**: Coverage.py reports integrated into CI/CD

---

## DETAILED REQUIREMENTS

### 1. Test Infrastructure Setup

**1.1 Test Dependencies:**

Add to `requirements-dev.txt` (or `requirements.txt` with test extras):
- pytest
- pytest-asyncio
- pytest-cov
- pytest-mock
- httpx (for TestClient)
- freezegun (for time-dependent tests)
- Faker (optional)

**1.2 Test Configuration:**

Create `pyproject.toml` test section (or `pytest.ini` / `conftest.py`):
- Test discovery pattern: `tests/test_*.py`
- Async mode: auto
- Coverage source: `app/`
- Coverage report: HTML + XML + terminal
- Coverage minimum: 80% (fail if below)
- Exclude from coverage: `__init__.py`, config files, main.py app creation

**1.3 Test Directory Structure:**

```
tests/
├── conftest.py              — Shared fixtures, mocks, test client
├── fixtures/
│   ├── sample_invoices/     — Sample images for preprocessing tests
│   ├── sample_xml/          — Sample e-Invoice XML files
│   ├── llm_responses/       — Mock LLM response JSONs
│   │   ├── gemini_valid.json
│   │   ├── gpt_valid.json
│   │   ├── claude_valid.json
│   │   ├── malformed.json
│   │   └── incomplete.json
│   └── expected_outputs/    — Expected extraction results for comparison
├── unit/
│   ├── test_preprocessing.py
│   ├── test_gemini_service.py
│   ├── test_openai_service.py
│   ├── test_anthropic_service.py
│   ├── test_fallback_chain.py
│   ├── test_response_normalizer.py
│   ├── test_validation_service.py
│   ├── test_confidence_scorer.py
│   ├── test_xml_parser.py
│   ├── test_extraction_service.py
│   ├── test_consumer.py
│   ├── test_producer.py
│   └── test_helpers.py
└── test_api.py              — FastAPI endpoint tests (TestClient)
```

**1.4 Shared Fixtures (`conftest.py`):**

Create reusable pytest fixtures:
- `sample_invoice_data()`: A valid InvoiceData object with all fields populated
- `sample_extraction_request()`: A valid ExtractionRequest
- `mock_gemini_response()`: A typical Gemini API response (JSON string)
- `mock_gpt_response()`: A typical OpenAI API response
- `mock_claude_response()`: A typical Anthropic API response
- `sample_xml_content()`: A valid e-Invoice XML string
- `sample_image_bytes()`: A small test image (can be a 1x1 pixel PNG created programmatically)
- `test_client()`: FastAPI TestClient for endpoint testing

---

### 2. Image Preprocessing Tests (`test_preprocessing.py`)

Test the Pillow-based preprocessing pipeline from Phase 14.

**Tests:**

- **Rotation correction**: Given a skewed image, verify rotation is applied
- **Contrast enhancement**: Verify contrast adjustment is applied (compare pixel values before/after)
- **Brightness adjustment**: Verify brightness change
- **Resize**: Image larger than max dimension gets resized (maintain aspect ratio)
- **Resize skip**: Image smaller than max dimension is NOT resized
- **Format conversion**: Non-JPEG/PNG input converted to supported format
- **EXIF orientation**: Image with EXIF rotation data handled correctly
- **Grayscale handling**: Grayscale image processed without error
- **Invalid input**: None input, empty bytes, corrupt image data → appropriate exceptions
- **Pipeline end-to-end**: Full preprocessing pipeline produces valid output image
- **File size optimization**: Output image is within API size limits

**Mocking**: No external mocks needed — these are pure image transformations. Use small test images created with Pillow in fixtures.

---

### 3. LLM Client Tests

Test each LLM provider client independently. ALL external API calls must be mocked.

**3.1 Gemini Service Tests** (`test_gemini_service.py`):

- Successful extraction: mock API returns valid JSON → parsed correctly
- API timeout: mock raises timeout → service raises appropriate exception
- API error (500): mock returns error → service raises
- Rate limit (429): mock returns 429 → service raises rate limit exception
- Invalid JSON response: mock returns non-JSON → service raises parse error
- Turkish character handling: response with ç, ğ, ı, ö, ş, ü parsed correctly
- Image encoding: verify image is correctly base64 encoded in API call
- Prompt includes all required fields

**3.2 OpenAI Service Tests** (`test_openai_service.py`):

- Same test cases as Gemini adapted for OpenAI API format
- Vision API: verify image is correctly included in message content
- Response format differs from Gemini → verify correct parsing

**3.3 Anthropic Service Tests** (`test_anthropic_service.py`):

- Same test cases adapted for Anthropic API format
- Claude response format parsing
- Image included as base64 in user message

**Mocking pattern:**
```python
@pytest.fixture
def mock_gemini_client(mocker):
    mock = mocker.patch('app.services.gemini_service.genai')  # or whatever the SDK
    return mock

def test_successful_extraction(mock_gemini_client, sample_image_bytes):
    mock_gemini_client.generate_content.return_value = MockResponse(
        text='{"fatura_no": "FTR-001", ...}'
    )
    result = gemini_service.extract(sample_image_bytes)
    assert result.fatura_no == "FTR-001"
```

---

### 4. Fallback Chain Tests (`test_fallback_chain.py`)

Test the fallback mechanism from Phase 16.

**Tests:**

- **Primary succeeds**: Gemini returns result → GPT and Claude NOT called
- **Primary fails, secondary succeeds**: Gemini timeout → GPT called and succeeds → Claude NOT called
- **Primary and secondary fail, tertiary succeeds**: Gemini + GPT fail → Claude succeeds
- **All providers fail**: All three fail → raises final exception with all errors
- **Fallback trigger conditions**: Test each trigger (timeout, 5xx, 429, network error)
- **2-second delay between providers**: Verify delay is respected (use freezegun or mock sleep)
- **Provider order respected**: Verify chain order is Gemini → GPT → Claude
- **Provider tracking**: Verify the used provider name is returned in the result
- **Partial failure recovery**: First provider returns malformed JSON → falls through to next
- **Circuit breaker (if implemented)**: Provider marked as down skipped on subsequent calls

---

### 5. Response Normalizer Tests (`test_response_normalizer.py`)

Test normalizing different LLM output formats to a common InvoiceData model (Phase 16).

**Tests:**

- Gemini response format → normalized InvoiceData
- OpenAI response format → normalized InvoiceData
- Claude response format → normalized InvoiceData
- All three produce equivalent output for the same invoice
- Missing optional fields → set to None/null, no error
- Extra unexpected fields in LLM response → ignored
- Date format normalization: "12/02/2026", "2026-02-12", "12.02.2026" → consistent format
- Amount normalization: "1.234,56" (TR format), "1,234.56" (EN format) → Decimal
- Turkish character preservation in supplier/buyer names
- Empty response → appropriate error
- Nested items array correctly mapped

---

### 6. Validation Service Tests (`test_validation_service.py`)

Test the response validation from Phase 17.

**Tests:**

**Schema Validation:**
- Valid complete response passes
- Missing required field (fatura_no) fails
- Missing required field (tarih) fails
- Missing required field (genel_toplam) fails
- Extra fields do not cause failure

**Mathematical Consistency:**
- `subtotal + tax_amount == total_amount` → passes
- `subtotal + tax_amount != total_amount` → fails with tolerance check
- Item quantities × unit prices == item totals
- Sum of item totals == subtotal
- Tolerance: accept ±0.01 difference (rounding)

**Format Validation:**
- VKN format: 10 digits → valid
- TCKN format: 11 digits → valid
- Invalid tax number: letters, wrong length → fails
- Date format: valid Turkish date → passes
- Date format: invalid/future date → flags warning
- Amount: negative total → fails
- Amount: unreasonably large (> 10 billion) → flags warning

**Currency Validation:**
- TRY, USD, EUR → valid
- Unknown currency code → flags warning (doesn't fail)

---

### 7. Confidence Scorer Tests (`test_confidence_scorer.py`)

Test the confidence score calculation from Phase 17.

**Tests:**

- Perfect invoice (all fields present, math correct, formats valid) → score 95-100
- Missing 1 optional field → score reduced slightly (e.g., 90)
- Missing 1 required field → score reduced significantly (e.g., 60)
- Math inconsistency → score reduced (e.g., 70)
- Invalid tax number format → score reduced
- Multiple issues compound: missing fields + math error → low score (e.g., 40)
- Score is always 0-100 (never negative or > 100)
- Threshold check: score < 70 → flagged for manual review
- E-Invoice XML → score 95-100 (auto-high confidence)
- Empty/mostly-empty extraction → score near 0

---

### 8. XML Parser Tests (`test_xml_parser.py`)

Test the UBL-TR e-Invoice XML parser from Phase 18.

**Tests:**

- Valid e-Invoice XML: all fields extracted correctly
- Invoice number extraction
- Date extraction and format
- Supplier/buyer info extraction (name, tax number, address)
- Invoice items extraction (description, quantity, unit price, line total)
- Tax amounts extraction (KDV)
- Currency extraction
- Multiple items XML
- Single item XML
- Namespace handling (UBL-TR uses XML namespaces)
- Missing optional elements → null, no error
- Malformed XML → appropriate exception
- Empty XML → appropriate exception
- Non-invoice XML (wrong root element) → error
- Encoding: UTF-8 with Turkish characters

**Test Fixtures:**
Create sample XML files in `tests/fixtures/sample_xml/`:
- `valid_invoice.xml`: Complete e-Invoice with all fields
- `minimal_invoice.xml`: Only required fields
- `multi_item_invoice.xml`: Invoice with 5+ items
- `turkish_chars_invoice.xml`: Supplier/item names with ç, ğ, ı, ö, ş, ü
- `malformed.xml`: Invalid XML structure
- `wrong_schema.xml`: Valid XML but not an invoice

---

### 9. Extraction Service (Orchestrator) Tests (`test_extraction_service.py`)

Test the main extraction service that orchestrates the pipeline.

**Tests:**

- Image file → preprocessing → LLM extraction → validation → result
- XML file → XML parser → validation → result (LLM NOT called)
- File type auto-detection: JPEG → image pipeline, XML → XML pipeline
- Preprocessing failure → error response (does not reach LLM)
- LLM extraction failure (all providers) → error response
- Validation failure → result returned with low confidence (not rejected)
- End-to-end: valid image → complete InvoiceData with all fields

**Mocking**: Mock all sub-services (preprocessor, LLM chain, validator, XML parser).

---

### 10. RabbitMQ Consumer/Producer Tests (`test_consumer.py`, `test_producer.py`)

Test messaging logic from Phase 19. Mock the RabbitMQ connection.

**Consumer Tests:**
- Valid message received → extraction triggered
- Invalid message format → message rejected (nack)
- Extraction succeeds → result published to response queue
- Extraction fails → error message published to response queue
- Connection lost → reconnect attempted

**Producer Tests:**
- Result message published with correct routing key
- Message serialization (JSON format)
- Connection failure handling

---

### 11. FastAPI Endpoint Tests (`test_api.py`)

Test the REST API endpoints using FastAPI's `TestClient`.

**Tests:**

- `GET /health` → 200 with correct structure
- `GET /health/ready` → 200
- `GET /health/live` → 200
- `POST /api/v1/extraction/extract` with valid image → 200 with extraction result
- `POST /api/v1/extraction/extract` without file → 422 validation error
- `POST /api/v1/extraction/extract` with oversized file → 413
- `GET /api/v1/extraction/providers` → lists available providers
- Internal API key authentication (Phase 32): missing key → 401, wrong key → 401, correct key → succeeds
- Rate limiting (Phase 32): verify 429 after exceeding limit

**Mocking**: Mock the extraction service (don't call real LLM APIs).

---

### 12. Test Coverage Targets

| Module | Target | Rationale |
|---|---|---|
| services/validation_service.py | 90%+ | Critical business logic |
| services/confidence_scorer.py | 90%+ | Critical scoring logic |
| services/xml_parser_service.py | 90%+ | Parsing logic, many edge cases |
| services/fallback_chain.py | 85%+ | Complex chain logic |
| services/response_normalizer.py | 85%+ | Data transformation |
| services/preprocessing_service.py | 80%+ | Image processing |
| services/*_service.py (LLM clients) | 80%+ | API interaction |
| messaging/* | 75%+ | Async messaging |
| models/* | Excluded | Data classes |
| config.py | Excluded | Configuration |

**Overall target**: ≥ 80% coverage

---

### 13. CI/CD Integration

Update the GitHub Actions workflow (Phase 1) to:

- Run `pytest --cov=app --cov-report=xml --cov-report=html`
- Publish coverage report as a build artifact
- Fail if coverage < 80%
- Display test count and pass/fail in PR check

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_35b_result.md`

The result file must include:

1. Phase summary
2. Test infrastructure setup (dependencies, conftest, fixtures)
3. Test count by module
4. Total test count
5. Coverage report summary (per-module percentages)
6. Test execution time
7. Files created (list all test files with paths)
8. Test fixtures documentation (sample files created)
9. Mock LLM responses documentation
10. CI/CD changes
11. Tests that uncovered bugs (list any bugs found and fixed)
12. Issues encountered and solutions
13. Next steps (Phase 36 integration tests)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 13**: FastAPI project structure
- **Phase 14-18**: All extraction service code must be implemented
- **Phase 19**: RabbitMQ messaging
- **Phase 32**: Internal API key (for endpoint auth tests)

### Required By
- **Phase 36**: Integration Tests (builds on test fixtures and mocking patterns)
- **Phase 38**: Performance Optimization

---

## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] pytest configured with conftest.py and shared fixtures
- [ ] Coverage.py configured to generate HTML report
- [ ] Test fixture files created: sample images, XMLs, mock LLM responses
- [ ] Image preprocessing tests: resize, rotation correction, contrast adjustment
- [ ] Image preprocessing tests: unsupported format handling
- [ ] Image preprocessing tests: corrupted file handling
- [ ] Gemini client tests: successful extraction with mocked API
- [ ] Gemini client tests: timeout, 5xx, 429 error handling
- [ ] GPT client tests: successful extraction with mocked API
- [ ] GPT client tests: timeout, 5xx, 429 error handling
- [ ] Claude client tests: successful extraction with mocked API
- [ ] Claude client tests: timeout, 5xx, 429 error handling
- [ ] Fallback chain tests: primary succeeds → no fallback
- [ ] Fallback chain tests: primary fails → secondary succeeds
- [ ] Fallback chain tests: primary + secondary fail → tertiary succeeds
- [ ] Fallback chain tests: all three fail → AllProvidersFailedError
- [ ] Response normalizer tests: Gemini response → common format
- [ ] Response normalizer tests: GPT response → common format
- [ ] Response normalizer tests: Claude response → common format
- [ ] Validation service tests: valid invoice passes
- [ ] Validation service tests: missing required fields detected
- [ ] Validation service tests: math inconsistency detected (subtotal + tax ≠ total)
- [ ] Validation service tests: invalid date format detected
- [ ] Confidence scorer tests: high confidence scenario (≥90)
- [ ] Confidence scorer tests: medium confidence scenario (70-89)
- [ ] Confidence scorer tests: low confidence scenario (<70)
- [ ] XML parser tests: valid GİB e-Invoice XML parsed correctly
- [ ] XML parser tests: invalid XML returns error
- [ ] XML parser tests: multi-item invoice parsed correctly
- [ ] XML parser tests: Turkish characters preserved
- [ ] Extraction orchestrator tests: image path (LLM pipeline)
- [ ] Extraction orchestrator tests: XML path (parser, no LLM)
- [ ] RabbitMQ consumer tests: message received and processed
- [ ] RabbitMQ producer tests: result published correctly
- [ ] FastAPI endpoint tests: POST /extract returns correct response
- [ ] FastAPI endpoint tests: health endpoints return correct status
- [ ] Internal API key auth tests: valid key accepted, invalid key rejected
- [ ] Overall coverage ≥ 80%
- [ ] Total test count ≥ 100
- [ ] All tests pass with zero failures
- [ ] CI/CD pipeline runs Python tests and reports coverage
- [ ] Result file created at docs/FURKAN/step_results/faz_35b_result.md
---
## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ pytest and coverage tools configured and working
2. ✅ conftest.py with shared fixtures created
3. ✅ Test fixture files created (sample XMLs, mock LLM responses)
4. ✅ Image preprocessing: all transformations and edge cases tested
5. ✅ Each LLM client (Gemini, GPT, Claude) tested with mocked APIs
6. ✅ Fallback chain tested: all failure/success permutations
7. ✅ Response normalizer tested: all provider formats → common model
8. ✅ Validation service tested: schema, math, format checks
9. ✅ Confidence scorer tested: various score scenarios, boundaries
10. ✅ XML parser tested: valid, invalid, minimal, multi-item, Turkish chars
11. ✅ Extraction orchestrator tested: image path, XML path, error paths
12. ✅ RabbitMQ consumer/producer tested with mocked broker
13. ✅ FastAPI endpoints tested with TestClient
14. ✅ Internal API key auth tested
15. ✅ Overall coverage ≥ 80%
16. ✅ Total Python test count ≥ 100 tests
17. ✅ All tests pass (zero failures)
18. ✅ CI/CD pipeline runs Python tests and reports coverage
19. ✅ Result file is created at docs/FURKAN/step_results/faz_35b_result.md

---

## IMPORTANT NOTES

1. **NEVER Call Real LLM APIs in Tests**: All LLM API calls must be mocked. Real API calls cost money, are slow, are non-deterministic, and require credentials. Mock the HTTP client or the SDK methods.

2. **Deterministic Tests**: Tests must produce the same result every run. No random data, no time-dependent assertions (use freezegun for time), no network calls.

3. **Test Data Realism**: Use realistic Turkish invoice data in fixtures. Realistic supplier names (e.g., "ABC Teknoloji A.Ş."), realistic VKN (10 digits), realistic invoice numbers (e.g., "FTR2026000001"), realistic amounts.

4. **Async Tests**: Many FastAPI services are async. Use `@pytest.mark.asyncio` and `async def test_...` for async functions. Configure pytest-asyncio in `pyproject.toml`.

5. **XML Test Files**: Create proper UBL-TR XML test files. They don't need to be complete UBL schemas — but they should have the correct namespace declarations and element structure that the parser expects.

6. **Image Test Files**: For preprocessing tests, create minimal test images programmatically with Pillow in the test setup. Do NOT commit large image files to the repository.

7. **Fix Bugs Found During Testing**: If tests reveal bugs in the extraction service code, fix them. Document what was found and fixed — this adds value to the graduation project.

8. **Coordinate with Ömer**: Both Phase 35-A and 35-B run in parallel. Coordinate CI/CD changes so they don't conflict. Consider using separate GitHub Actions workflow files or separate steps in the same workflow.

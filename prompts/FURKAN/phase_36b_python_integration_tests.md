# PHASE 36-B: INTEGRATION TESTS — PYTHON EXTRACTION SERVICE & LLM PIPELINE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)

### Current State (Phases 0-35 Completed)
All features implemented. Phase 35-B added comprehensive unit tests for the Python service with pytest-cov coverage reporting, mock LLM responses, and sample fixtures.

### What Are Integration Tests (vs Phase 35-B Unit Tests)?
Phase 35-B tested individual functions in isolation with mocked dependencies. Integration tests verify that **multiple components work together**:
- Full extraction pipeline: preprocessing → LLM call → response parsing → validation → scoring
- RabbitMQ consumer receives a real message, processes it, and publishes a result
- FastAPI endpoints accept HTTP requests and return proper responses through the full stack
- LLM responses (mocked at the HTTP level, not function level) flow through the entire pipeline
- Test with real sample invoice images and XML files

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3-4 days
- **Parallel**: ÖMER works on Phase 36-A (Spring Boot integration tests) simultaneously

---

## OBJECTIVE

Write integration tests for the Python extraction service that verify:

1. **End-to-end extraction pipeline**: Image in → InvoiceData out (with mocked HTTP to LLM APIs)
2. **File type routing**: Image files go through LLM, XML files go through parser
3. **Fallback chain in integration**: Primary fails → secondary activated → result obtained
4. **RabbitMQ flow**: Message received → processed → result published
5. **FastAPI endpoint integration**: HTTP request → full processing → HTTP response
6. **Test data set**: Real-world-like invoice images and XMLs with expected outputs

---

## DETAILED REQUIREMENTS

### 1. Test Infrastructure Setup

**1.1 Test Dependencies (add to requirements-dev.txt):**

- pytest, pytest-asyncio, pytest-cov (from Phase 35-B)
- httpx (for TestClient — from Phase 35-B)
- respx (for mocking HTTP requests to LLM APIs at the transport level)
- testcontainers (for RabbitMQ container — `testcontainers[rabbitmq]`)
- aio-pika or pika (for RabbitMQ test interactions)

**Why `respx` instead of `unittest.mock`?**
In Phase 35-B, LLM service methods were mocked at the function level. For integration tests, we want the actual service code to run — but mock the HTTP transport layer. `respx` intercepts `httpx` requests, so the LLM client code runs fully but HTTP calls are intercepted and return mock responses. This tests serialization, headers, error handling, and retry logic as a whole.

**1.2 Test Directory Structure:**

```
tests/
├── conftest.py                        — From Phase 35-B (extended)
├── fixtures/                          — From Phase 35-B (extended)
│   ├── sample_invoices/
│   │   ├── standard_invoice.jpg       — Standard Turkish invoice image
│   │   ├── low_quality_invoice.jpg    — Blurry/low-res image
│   │   ├── rotated_invoice.jpg        — Skewed scan
│   │   ├── multi_page_invoice.pdf     — PDF with multiple pages
│   │   └── receipt_image.png          — Receipt (not invoice — edge case)
│   ├── sample_xml/                    — From Phase 35-B
│   │   ├── valid_einvoice.xml
│   │   ├── minimal_einvoice.xml
│   │   └── multi_item_einvoice.xml
│   ├── llm_responses/                 — From Phase 35-B (extended)
│   │   ├── gemini_complete.json       — Full successful Gemini response
│   │   ├── gpt_complete.json          — Full successful GPT response
│   │   ├── claude_complete.json       — Full successful Claude response
│   │   ├── gemini_partial.json        — Missing some fields
│   │   ├── gemini_math_error.json     — Math inconsistency
│   │   └── malformed_json.txt         — Non-JSON LLM output
│   └── expected_outputs/
│       ├── standard_invoice_expected.json
│       ├── valid_einvoice_expected.json
│       └── multi_item_expected.json
├── unit/                              — From Phase 35-B
│   └── ...
├── integration/                       — NEW (this phase)
│   ├── test_extraction_pipeline.py    — End-to-end pipeline
│   ├── test_file_routing.py           — Image vs XML routing
│   ├── test_fallback_integration.py   — Fallback chain end-to-end
│   ├── test_rabbitmq_flow.py          — RabbitMQ message flow
│   ├── test_api_integration.py        — FastAPI endpoints full stack
│   └── test_data_validation.py        — Test data set validation
└── test_api.py                        — From Phase 35-B (endpoint tests)
```

---

### 2. End-to-End Extraction Pipeline Tests (`test_extraction_pipeline.py`)

Test the complete pipeline: image → preprocessing → LLM → normalization → validation → scoring → InvoiceData.

**Setup**: Use `respx` to mock HTTP calls to LLM API endpoints. Let everything else (preprocessing, parsing, validation, scoring) run for real.

**Tests:**

**2.1 Happy Path — Standard Invoice Image:**
- Input: `standard_invoice.jpg`
- Mock: Gemini API returns `gemini_complete.json`
- Verify: All fields extracted, confidence score > 80, provider = GEMINI
- Compare with `standard_invoice_expected.json`

**2.2 Low Quality Image:**
- Input: `low_quality_invoice.jpg`
- Mock: Gemini returns response with some missing fields
- Verify: Extraction completes, confidence score is lower (e.g., 50-70), missing fields flagged

**2.3 Rotated/Skewed Image:**
- Input: `rotated_invoice.jpg`
- Mock: Gemini returns valid response (preprocessing fixed rotation)
- Verify: Preprocessing was applied (image was processed before LLM call)

**2.4 Math Inconsistency:**
- Mock: Gemini returns `gemini_math_error.json` (subtotal + tax ≠ total)
- Verify: Validation catches the math error, confidence score reduced

**2.5 Missing Required Fields:**
- Mock: Gemini returns `gemini_partial.json` (no invoice number)
- Verify: Validation flags missing fields, confidence score < 70

**2.6 Malformed LLM Response:**
- Mock: Gemini returns `malformed_json.txt` (not JSON)
- Verify: Fallback triggered → GPT called → succeeds

---

### 3. File Type Routing Tests (`test_file_routing.py`)

Test that the extraction service correctly routes files to the appropriate processor.

**Tests:**

- `.jpg` file → image preprocessing + LLM extraction pipeline
- `.jpeg` file → image preprocessing + LLM extraction pipeline
- `.png` file → image preprocessing + LLM extraction pipeline
- `.pdf` file → image preprocessing + LLM extraction pipeline (PDF treated as image)
- `.xml` file → XML parser (NO LLM call — verify LLM was NOT called)
- `.xml` e-Invoice → confidence score auto-set to 95-100
- Unknown extension → error response

Verify by checking: which service methods were called, whether LLM mock was hit, correct result source.

---

### 4. Fallback Chain Integration Tests (`test_fallback_integration.py`)

Test the fallback mechanism with HTTP-level mocks (not function-level mocks).

**Tests:**

**4.1 Primary Succeeds:**
- Mock: Gemini API → 200 with valid JSON
- Verify: GPT and Claude endpoints NOT called, provider = GEMINI

**4.2 Primary Times Out, Secondary Succeeds:**
- Mock: Gemini API → timeout (delay response > 30s or raise timeout), GPT → 200
- Verify: GPT called after Gemini failure, provider = GPT

**4.3 Primary 500, Secondary 429, Tertiary Succeeds:**
- Mock: Gemini → 500, GPT → 429, Claude → 200
- Verify: Claude used, provider = CLAUDE

**4.4 All Providers Fail:**
- Mock: Gemini → 500, GPT → 500, Claude → 500
- Verify: Error raised with details from all three failures

**4.5 Primary Returns Malformed JSON, Secondary Succeeds:**
- Mock: Gemini → 200 but response is not parseable JSON, GPT → 200 valid
- Verify: Parsing failure triggers fallback, GPT result used

**4.6 Primary Returns Valid JSON But Validation Fails Badly:**
- Mock: Gemini → 200 with empty fields (score < threshold)
- Note: This should NOT trigger fallback (validation happens after extraction). Verify fallback is NOT triggered — the low-quality result is returned with low confidence.

---

### 5. RabbitMQ Flow Integration Tests (`test_rabbitmq_flow.py`)

Test the full message flow with a real RabbitMQ container.

**Setup**: Use `testcontainers` to spin up a RabbitMQ instance.

**Tests:**

**5.1 Happy Path:**
- Publish extraction request message to the queue
- Consumer picks it up, processes extraction (with mocked LLM HTTP)
- Result message published to result queue
- Verify result message contains: status COMPLETED, invoice data, confidence score, provider name

**5.2 File Not Found:**
- Publish request referencing a non-existent file path
- Verify result message contains: status FAILED, error code FILE_NOT_FOUND

**5.3 All LLM Providers Fail:**
- Mock all LLM HTTP endpoints to fail
- Publish request
- Verify result message contains: status FAILED, error code ALL_PROVIDERS_FAILED

**5.4 Malformed Message:**
- Publish invalid JSON to the queue
- Verify message is rejected (nack'd) and moved to DLQ

**5.5 XML File Processing:**
- Publish request for an XML file
- Verify result message contains: status COMPLETED, source = XML_PARSER, confidence 95-100

**Note**: If RabbitMQ Testcontainers integration is too complex to set up, an acceptable alternative is to mock the pika connection at a high level but test the consumer logic end-to-end. Document the approach chosen.

---

### 6. FastAPI Endpoint Integration Tests (`test_api_integration.py`)

Test full-stack HTTP endpoint behavior (beyond the simple endpoint tests in Phase 35-B).

**Setup**: Use `httpx.AsyncClient` with the FastAPI app. Mock LLM HTTP calls with `respx`.

**Tests:**

**6.1 POST /api/v1/extraction/extract — Image:**
- Upload a real test image file
- Mock Gemini HTTP response
- Verify: 200, response body contains all invoice fields, confidence score, provider

**6.2 POST /api/v1/extraction/extract — XML:**
- Upload a real test XML file
- Verify: 200, XML parsed correctly, LLM NOT called

**6.3 POST /api/v1/extraction/extract — Fallback Triggered:**
- Mock Gemini → 500, GPT → 200
- Upload image
- Verify: 200, provider = GPT (fallback worked)

**6.4 POST /api/v1/extraction/extract — All Fail:**
- Mock all LLM endpoints → 500
- Upload image
- Verify: 500 or 422, error message describes failure

**6.5 POST /api/v1/extraction/extract — Invalid File Type:**
- Upload a .txt file
- Verify: 400 or 422, error message about unsupported format

**6.6 POST /api/v1/extraction/extract — Oversized File:**
- Upload a file > 20MB
- Verify: 413

**6.7 Authentication (Phase 32):**
- Request without X-Internal-API-Key → 401
- Request with wrong key → 401
- Request with correct key → processed

**6.8 Rate Limiting (Phase 32 — if implemented in Python):**
- Send requests beyond rate limit → 429

---

### 7. Test Data Set with Expected Outputs (`test_data_validation.py`)

Create a "golden test" approach: known inputs → known expected outputs.

**For each test fixture:**
1. Process the input through the full pipeline
2. Compare the output against the expected output JSON
3. Allow tolerance for fields that may vary (confidence score ±5, timestamps)

**Test Data Set:**

| Input File | Expected Output | Notes |
|---|---|---|
| standard_invoice.jpg | standard_invoice_expected.json | Complete Turkish invoice, all fields |
| valid_einvoice.xml | valid_einvoice_expected.json | UBL-TR e-Invoice |
| multi_item_einvoice.xml | multi_item_expected.json | 5+ line items |
| minimal_einvoice.xml | minimal_einvoice_expected.json | Only required fields |

**Comparison Logic:**
- String fields: exact match (case-insensitive for names)
- Numeric fields: tolerance ±0.01
- Date fields: exact match after normalization
- Array fields (items): same length, each item matches
- Confidence score: within expected range
- Fields that are null in expected → must be null in output

---

### 8. Performance Benchmarks (Lightweight)

While running integration tests, measure and document:

- Average preprocessing time per image
- Average LLM API response time (mocked — so this measures parsing + validation)
- Average XML parsing time
- Average total pipeline time (image path)
- Average total pipeline time (XML path)

Store these as baseline metrics in the result file. Phase 38 (Performance Optimization) can use them.

---

### 9. CI/CD Integration

Update GitHub Actions:

- Integration tests in a separate step: `pytest tests/integration/ -v --cov`
- May need Docker-in-Docker for RabbitMQ Testcontainers (or skip RabbitMQ tests in CI if too complex)
- Integration tests can run on merge to main (not every push) since they're slower

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_36b_result.md`

The result file must include:

1. Phase summary
2. Test infrastructure setup (respx, testcontainers, fixtures)
3. Test count by category (pipeline, routing, fallback, RabbitMQ, API, golden tests)
4. Total integration test count
5. Test execution time
6. Files created (all test and fixture files)
7. Golden test data set documentation (inputs → expected outputs)
8. Performance baseline metrics
9. CI/CD changes
10. Bugs found during integration testing
11. Issues encountered (async testing quirks, mocking difficulties, etc.)
12. Next steps (Phase 37 frontend/E2E tests)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 35-B**: Python unit tests (test fixtures, conftest, mock responses)
- **All extraction service phases (13-19)**: All code must be implemented

### Required By
- **Phase 38**: Performance Optimization (baseline metrics from this phase)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] respx (or equivalent) configured for HTTP-level LLM API mocking
- [ ] End-to-end pipeline test: image → preprocess → LLM (mocked) → validate → result
- [ ] End-to-end pipeline test: result contains all expected invoice fields
- [ ] File type routing: JPEG/PNG/PDF sent to LLM pipeline
- [ ] File type routing: XML sent to XML parser (no LLM call)
- [ ] Fallback chain integration: primary fails (HTTP 500) → secondary succeeds
- [ ] Fallback chain integration: primary + secondary fail → tertiary succeeds
- [ ] Fallback chain integration: all fail → AllProvidersFailedError with details
- [ ] RabbitMQ flow: extraction request message received → processed → result message published
- [ ] RabbitMQ flow: correlation ID preserved through the flow
- [ ] FastAPI /extract endpoint: returns correct response for image input
- [ ] FastAPI /extract endpoint: returns correct response for XML input
- [ ] FastAPI /extract endpoint: returns error response for unsupported file type
- [ ] Internal API key authentication: valid key returns 200, invalid key returns 401
- [ ] Golden test data: at least 4 fixture files with expected outputs
- [ ] Golden test data: all field comparisons pass within tolerance
- [ ] XML path: produces confidence score 95-100 without LLM call
- [ ] Performance baseline: average extraction time documented
- [ ] Total integration test count ≥ 40
- [ ] All integration tests pass
- [ ] CI/CD runs integration tests
- [ ] Result file created at docs/FURKAN/step_results/faz_36b_result.md
---
## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ respx configured for HTTP-level LLM API mocking
2. ✅ End-to-end pipeline test passes: image → preprocessing → LLM → validation → result
3. ✅ File type routing correctly sends images to LLM, XMLs to parser
4. ✅ Fallback chain tested at HTTP level: primary fail → secondary/tertiary succeed
5. ✅ All-providers-fail scenario returns proper error
6. ✅ RabbitMQ flow tested (with testcontainer or high-level mock): message in → result out
7. ✅ FastAPI endpoint integration tests cover all extraction scenarios
8. ✅ Internal API key authentication verified
9. ✅ Golden test data set: 4+ fixtures with expected outputs, all comparisons pass
10. ✅ XML path produces high confidence (95-100) without LLM call
11. ✅ Performance baseline metrics documented
12. ✅ Total Python integration test count ≥ 40
13. ✅ All integration tests pass
14. ✅ CI/CD runs integration tests
15. ✅ Result file created at docs/FURKAN/step_results/faz_36b_result.md

---

## IMPORTANT NOTES

1. **Mock HTTP, Not Functions**: The key difference from Phase 35-B is mocking level. Use `respx` to intercept HTTP requests to `https://generativelanguage.googleapis.com/...` (Gemini), `https://api.openai.com/...` (GPT), `https://api.anthropic.com/...` (Claude). This tests the full client code including serialization, header setting, and error handling.

2. **Real Images Are Small**: Test images should be small (< 100KB) for fast tests. Create synthetic invoice images with Pillow if needed — they don't need to look realistic, just test the pipeline.

3. **RabbitMQ Testcontainers Are Optional**: If setting up RabbitMQ testcontainers proves too complex (especially in CI), an acceptable fallback is to mock pika at a higher level. Document the decision. The important thing is testing the consumer logic, not the RabbitMQ protocol.

4. **Async Test Caution**: FastAPI and its services may use async. Ensure all integration tests properly handle async: use `@pytest.mark.asyncio`, `async with httpx.AsyncClient(app=app)`, etc.

5. **Golden Tests Catch Regressions**: The expected output JSON files are a safety net. If someone changes the extraction logic and accidentally breaks the output format, golden tests will catch it.

6. **Coordinate with Ömer**: Both 36-A and 36-B run in parallel. The RabbitMQ message format must match what Spring Boot expects. If integration testing reveals message format issues, coordinate the fix.

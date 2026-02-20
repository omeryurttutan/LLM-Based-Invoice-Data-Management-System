# PHASE 15: GEMINI 3 FLASH LLM INTEGRATION (PRIMARY ENGINE)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 - LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-14 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend (layout, auth pages, invoice CRUD UI)
- ✅ Phase 13: Python FastAPI service setup with health endpoints, structured logging, error handling, CORS, Docker configuration, LLM API key env variables, Spring Boot ↔ FastAPI connectivity
- ✅ Phase 14: Image preprocessing pipeline (Pillow + PyMuPDF) - format detection, PDF conversion, orientation fix, deskew, contrast/brightness/sharpness enhancement, size optimization, base64 encoding

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Integrate Gemini 3 Flash as the primary LLM engine for invoice data extraction. This phase builds the core capability of sending preprocessed invoice images to Gemini's multimodal API and receiving structured JSON data containing all invoice fields. The system must handle Turkish invoice formats, parse LLM responses reliably, and include error tolerance mechanisms (timeout, retry, rate limit handling).

**This is the heart of the entire extraction system** — everything before this was preparation, and everything after builds on top of it.

---

## WHY GEMINI 3 FLASH AS PRIMARY

- **Cost-Effective**: Lowest per-token pricing among multimodal LLMs
- **Fast**: Optimized for speed (Flash variant)
- **Multimodal Native**: Accepts images directly alongside text prompts
- **JSON Mode**: Supports structured output generation
- **Turkish Support**: Good performance with Turkish language content

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add the Gemini integration module to the existing extraction-service. Place new files within the established project structure:

```
extraction-service/app/
├── services/
│   ├── preprocessing/          # ← Already exists from Phase 14
│   │   └── ...
│   ├── llm/
│   │   ├── __init__.py
│   │   ├── base_provider.py        # Abstract base class for all LLM providers
│   │   ├── gemini_provider.py      # Gemini 3 Flash implementation
│   │   ├── prompt_manager.py       # Prompt template management
│   │   └── response_parser.py      # Parse and normalize LLM JSON responses
│   └── extraction/
│       ├── __init__.py
│       └── extraction_service.py   # Orchestrates preprocessing → LLM → parsing
├── models/
│   ├── preprocessing.py            # ← Already exists from Phase 14
│   ├── extraction.py               # Extraction request/response DTOs
│   └── invoice_data.py             # Structured invoice data model
├── config/
│   └── llm_config.py               # LLM provider configurations
└── ...
```

### 2. Abstract LLM Provider Interface

Design the base provider class using the Strategy Pattern. This is critical because Phase 16 will add GPT-5.2 and Claude Haiku 4.5 as fallback providers using the same interface.

**base_provider.py must define:**

- An abstract method for sending an image + prompt and receiving raw text response
- An abstract method for getting the provider name (returns a string like "GEMINI", "GPT", "CLAUDE")
- An abstract method for checking if the provider is available/healthy
- Common configuration: timeout, max_retries, retry_delay
- Common error types that all providers should handle

**Provider Name Constants:**
- "GEMINI" — maps to `llm_provider` column in invoices table
- "GPT" — for Phase 16
- "CLAUDE" — for Phase 16

This abstraction is important: Phase 16 will add two more providers that implement this same interface.

### 3. Gemini 3 Flash API Client

Implement the Gemini provider using Google's `google-generativeai` Python SDK.

**API Configuration:**
- Model: `gemini-2.0-flash` (or latest available flash model — check Google AI Studio for the current model name at implementation time)
- API Key: Read from environment variable `GEMINI_API_KEY` (already configured in Phase 13)
- Timeout: 30 seconds per request
- Max retries: 2 (with exponential backoff)
- Generation config: temperature=0.1 (low for accuracy), max output tokens=4096

**Multimodal Request Flow:**
1. Accept a base64-encoded image (output from Phase 14 preprocessing)
2. Construct a multimodal content array: [image_part, text_prompt_part]
3. Send to Gemini API with generation config
4. Receive text response
5. Return raw response text for parsing

**Safety Settings:**
- Set all safety categories to BLOCK_NONE or minimum blocking (invoice images are business documents, not harmful content)

**Error Handling — the provider must catch and handle:**
- API timeout (30 seconds) → raise a custom `LLMTimeoutError`
- HTTP 429 (rate limit) → raise a custom `LLMRateLimitError`
- HTTP 5xx (server error) → raise a custom `LLMServerError`
- Network/connection errors → raise a custom `LLMConnectionError`
- Invalid API key → raise a custom `LLMAuthenticationError`
- Malformed response (not valid text) → raise a custom `LLMResponseError`
- All custom errors should inherit from a base `LLMError` class

**Retry Logic:**
- On timeout or 5xx: retry up to 2 times with exponential backoff (1s, then 2s)
- On 429 rate limit: wait for the duration specified in Retry-After header, or 5 seconds default
- On auth error: do NOT retry (fail immediately)
- On network error: retry once after 2 seconds

### 4. Invoice Extraction Prompt Design

This is one of the most critical parts of the entire project. The prompt quality directly determines extraction accuracy.

**Prompt Structure — the prompt must include:**

**a) Role Definition:**
- Tell the LLM it is an expert Turkish invoice data extraction system
- It must analyze the invoice image and extract ALL relevant fields

**b) Expected JSON Output Schema:**
Define the exact JSON structure the LLM should return. The schema must match the database columns from Phase 3:

```
{
  "invoice_number": "string — fatura numarası",
  "invoice_date": "string — format: YYYY-MM-DD",
  "due_date": "string or null — format: YYYY-MM-DD — vade tarihi",
  "supplier_name": "string — tedarikçi/satıcı firma adı",
  "supplier_tax_number": "string — VKN (10 hane) veya TCKN (11 hane)",
  "supplier_address": "string or null — tedarikçi adresi",
  "buyer_name": "string or null — alıcı firma adı",
  "buyer_tax_number": "string or null — alıcı VKN/TCKN",
  "items": [
    {
      "description": "string — ürün/hizmet açıklaması",
      "quantity": "number — miktar",
      "unit": "string or null — birim (adet, kg, lt, vb.)",
      "unit_price": "number — birim fiyat (KDV hariç)",
      "tax_rate": "number — KDV oranı (%, örn: 20)",
      "tax_amount": "number — KDV tutarı",
      "line_total": "number — satır toplamı (KDV dahil)"
    }
  ],
  "subtotal": "number — ara toplam (KDV hariç)",
  "tax_amount": "number — toplam KDV tutarı",
  "total_amount": "number — genel toplam (KDV dahil)",
  "currency": "string — para birimi: TRY, USD, EUR, GBP",
  "notes": "string or null — varsa fatura üzerindeki notlar"
}
```

**c) Turkish Invoice-Specific Instructions:**
- Common Turkish invoice field labels to look for: "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Toplam", "KDV", "Ara Toplam", "Genel Toplam", "Vergi Dairesi", "VKN", "TCKN"
- Turkish number format: comma as decimal separator (1.234,56), the LLM should convert to standard decimal format (1234.56) in the JSON
- Turkish date formats: DD.MM.YYYY, DD/MM/YYYY — always convert to YYYY-MM-DD in the JSON
- Currency detection: if not explicitly stated, default to "TRY"
- Tax rates commonly used in Turkey: 1%, 10%, 20%
- "KDV Dahil" means tax-inclusive, "KDV Hariç" means tax-exclusive

**d) Output Rules:**
- Return ONLY valid JSON, no markdown code blocks, no explanation text
- If a field is not visible on the invoice, set it to null (not empty string)
- All monetary amounts must be numbers (not strings), using dot as decimal separator
- If multiple tax rates exist, calculate totals accordingly
- If items cannot be individually identified, create a single item with the totals

**e) Prompt Versioning:**
- Store the prompt as a versioned template (e.g., v1, v2, v3)
- Include a prompt version identifier in the extraction result metadata
- This enables prompt A/B testing and iteration in the future

### 5. Response Parser

The response parser handles the raw LLM text output and converts it to a structured Python data model.

**Parsing Steps:**
1. Strip any markdown code fences if present (```json ... ```)
2. Strip any leading/trailing whitespace or newlines
3. Attempt JSON parsing
4. If JSON parsing fails, attempt to find JSON within the response text (regex for { ... })
5. If still fails, raise `LLMResponseError` with the raw response for debugging

**Data Normalization after parsing:**
- Dates: Validate format, attempt to parse DD.MM.YYYY or DD/MM/YYYY and convert to YYYY-MM-DD
- Numbers: Ensure all monetary values are float/decimal, handle Turkish comma format if LLM didn't convert
- Strings: Strip extra whitespace, normalize Turkish characters
- Null handling: Convert empty strings to None
- Currency: Uppercase, default to "TRY" if missing or unrecognized

**The parser should return a structured Pydantic model** (InvoiceData) that mirrors the JSON schema above. This model will be used by later phases (Phase 17 for validation, Phase 19 for queue messaging).

### 6. Extraction Service (Orchestrator)

Create an extraction service that ties preprocessing and LLM extraction together:

**Flow:**
1. Receive an image file (bytes or file path)
2. Call the preprocessing pipeline (Phase 14) to get a base64-encoded optimized image
3. Load the extraction prompt from prompt_manager
4. Call the Gemini provider with the image + prompt
5. Parse the response using response_parser
6. Return an ExtractionResult containing:
   - The parsed InvoiceData
   - The provider used ("GEMINI")
   - Processing duration in milliseconds
   - The prompt version used
   - Raw LLM response (for debugging/audit)

### 7. FastAPI Endpoint

Add a new extraction endpoint to the existing FastAPI app:

**POST /extract**
- Accepts: multipart/form-data with an image file
- Processing: preprocess → LLM extract → parse → return
- Response: JSON with extracted invoice data, provider info, processing time, confidence metadata

**POST /extract/base64**
- Accepts: JSON body with base64-encoded image and MIME type
- Same processing flow
- Useful for when Spring Boot sends pre-encoded images

**GET /extract/prompt-info**
- Returns the current prompt version and template info
- Useful for debugging and monitoring

**Error Responses:**
- 400: Invalid file format or corrupted image
- 408: LLM timeout after all retries
- 429: Rate limit exceeded on all providers
- 500: Unexpected processing error
- 503: LLM service unavailable

### 8. Configuration Management

All LLM-related configurations should be manageable via environment variables:

- `GEMINI_API_KEY`: API key (already in .env from Phase 13)
- `GEMINI_MODEL`: Model name (default: gemini-2.0-flash)
- `GEMINI_TIMEOUT`: Request timeout in seconds (default: 30)
- `GEMINI_MAX_RETRIES`: Max retry attempts (default: 2)
- `GEMINI_TEMPERATURE`: Generation temperature (default: 0.1)
- `GEMINI_MAX_OUTPUT_TOKENS`: Max response tokens (default: 4096)
- `LLM_DEFAULT_PROVIDER`: Default provider to use (default: GEMINI)

### 9. Logging Requirements

Log these events with structured logging (already set up in Phase 13):

**INFO level:**
- Extraction request received (file type, file size)
- Preprocessing completed (duration)
- LLM request sent (provider, model, prompt version)
- LLM response received (provider, duration, response size)
- Extraction completed successfully (provider, total duration)

**WARNING level:**
- LLM retry triggered (reason, attempt number)
- Response required cleanup (markdown stripping, JSON repair)
- Turkish number/date format conversion applied

**ERROR level:**
- LLM request failed after all retries
- Response parsing failed (include raw response snippet)
- Invalid JSON from LLM
- API authentication failed

**DEBUG level:**
- Full prompt sent to LLM
- Full raw response from LLM
- Parsed invoice data fields
- Preprocessing pipeline details

### 10. Turkish Character Handling

Ensure the entire pipeline correctly handles Turkish-specific characters:
- Letters: Ç, ç, Ğ, ğ, I, ı, İ, i, Ö, ö, Ş, ş, Ü, ü
- The prompt must explicitly mention Turkish character preservation
- Response parser must validate that Turkish characters survived the round-trip
- All string fields must use UTF-8 encoding

### 11. Dependencies

Add these Python packages to the extraction-service requirements:

- `google-generativeai` — Google's official Gemini Python SDK
- `pydantic` — Data validation and modeling (likely already present from Phase 13)
- `tenacity` — Retry logic with exponential backoff (optional, can use custom retry)

Update the Dockerfile and requirements.txt accordingly.

---

## TESTING REQUIREMENTS

### Test Categories

**1. Unit Tests for Gemini Provider:**
- Test successful API call mock (mock the Gemini SDK response)
- Test timeout handling (mock a slow response)
- Test rate limit handling (mock a 429 response)
- Test server error handling (mock a 5xx response)
- Test authentication error handling (mock invalid API key)
- Test retry logic (verify retries happen with correct delays)

**2. Unit Tests for Prompt Manager:**
- Test prompt template loading
- Test prompt version tracking
- Test prompt contains all required field instructions

**3. Unit Tests for Response Parser:**
- Test valid JSON parsing
- Test JSON with markdown code fences
- Test JSON embedded in explanation text
- Test Turkish number format conversion (1.234,56 → 1234.56)
- Test Turkish date format conversion (15.01.2026 → 2026-01-15)
- Test null/empty field handling
- Test currency normalization
- Test malformed JSON error handling
- Test items array parsing with multiple line items

**4. Integration Test for Extraction Flow:**
- Test full flow: image file → preprocess → extract → parsed result
- Use a mock LLM response (do not call real API in automated tests)
- Verify ExtractionResult contains all expected fields
- Verify processing duration is measured
- Verify provider name is "GEMINI"

**5. Manual Testing with Real API (not automated):**
- Test with at least 5 different real Turkish invoice images
- Record accuracy for each field
- Document which fields have low accuracy (for prompt iteration)
- This manual testing is for prompt refinement, not CI/CD

### Test File Structure:
```
extraction-service/tests/
├── unit/
│   ├── test_gemini_provider.py
│   ├── test_prompt_manager.py
│   └── test_response_parser.py
├── integration/
│   └── test_extraction_flow.py
└── fixtures/
    ├── mock_gemini_responses/
    │   ├── valid_response.json
    │   ├── response_with_markdown.txt
    │   ├── response_with_turkish_format.json
    │   ├── partial_response.json
    │   └── malformed_response.txt
    └── images/               # ← Already exists from Phase 14
```

---

## DATABASE CONSIDERATIONS

### Check: Does the existing schema support this phase?

Review the invoices table from Phase 3. The following columns are already present and will be used:
- `source_type` — will be set to 'LLM' for extracted invoices
- `llm_provider` — will be set to 'GEMINI'
- `confidence_score` — will be populated in Phase 17 (not this phase)
- `processing_duration_ms` — will store the extraction duration
- `original_file_path`, `original_file_name`, `original_file_size`, `original_file_type` — file metadata

**If any new columns or tables are needed** (unlikely for this phase, but check), create a Flyway migration file:
- Naming convention: `V{next_number}__phase_15_{description}.sql`
- Place in: `backend/src/main/resources/db/migration/`

For this phase, the existing schema should be sufficient. The extraction service returns data that the Spring Boot backend will persist. No direct DB access from Python service in this phase.

---

## VERIFICATION CHECKLIST

After completing this phase, verify ALL items:

### Provider Setup
- [ ] google-generativeai package installed and importable
- [ ] GEMINI_API_KEY loaded from environment
- [ ] Abstract base provider class created with all methods
- [ ] Gemini provider implements all abstract methods
- [ ] Provider returns correct name "GEMINI"

### Prompt Design
- [ ] Prompt includes role definition
- [ ] Prompt includes complete JSON schema
- [ ] Prompt includes Turkish-specific instructions (number format, date format, field labels)
- [ ] Prompt instructs JSON-only output (no markdown)
- [ ] Prompt handles null fields correctly
- [ ] Prompt is versioned (v1 at minimum)
- [ ] Prompt mentions all database-matching fields

### API Communication
- [ ] Multimodal request works (image + text)
- [ ] Base64 image is correctly sent to Gemini
- [ ] Response is received as text
- [ ] Timeout set to 30 seconds
- [ ] Retry logic works (2 retries with backoff)
- [ ] Rate limit (429) handled correctly
- [ ] Server error (5xx) handled correctly
- [ ] Auth error fails immediately (no retry)

### Response Parsing
- [ ] Valid JSON parsed correctly
- [ ] Markdown fences stripped
- [ ] Turkish number format converted
- [ ] Turkish date format converted
- [ ] Empty strings converted to null
- [ ] Currency normalized to uppercase
- [ ] Pydantic InvoiceData model validated
- [ ] Items array parsed with all fields
- [ ] Malformed JSON raises proper error

### Extraction Service
- [ ] Full pipeline works: file → preprocess → LLM → parse → result
- [ ] ExtractionResult includes provider name
- [ ] ExtractionResult includes processing duration
- [ ] ExtractionResult includes prompt version
- [ ] Raw response stored for debugging

### API Endpoints
- [ ] POST /extract accepts file upload and returns extracted data
- [ ] POST /extract/base64 accepts base64 input and returns extracted data
- [ ] GET /extract/prompt-info returns prompt metadata
- [ ] Error responses use correct HTTP status codes
- [ ] CORS allows requests from frontend (port 3001)

### Error Handling
- [ ] LLMTimeoutError raised on timeout
- [ ] LLMRateLimitError raised on 429
- [ ] LLMServerError raised on 5xx
- [ ] LLMConnectionError raised on network errors
- [ ] LLMAuthenticationError raised on invalid key
- [ ] LLMResponseError raised on unparseable response
- [ ] All errors have descriptive messages

### Turkish Support
- [ ] Ç, ğ, ı, İ, ö, ş, ü characters preserved in extraction
- [ ] Turkish date formats correctly converted
- [ ] Turkish number formats correctly converted
- [ ] Common Turkish invoice labels recognized in prompt

### Tests
- [ ] All unit tests pass
- [ ] Integration test passes (with mocked LLM)
- [ ] Mock response fixtures are realistic

### Configuration
- [ ] All config values have sensible defaults
- [ ] Config values overridable via environment variables
- [ ] Dockerfile updated with new dependencies
- [ ] requirements.txt updated

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_15_result.md`

Include the following sections:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date and actual time vs estimated (3-4 days)

### 2. Completed Tasks
Checklist of all tasks with status

### 3. Files Created/Modified
List all new and modified files with full paths

### 4. Prompt Design
- Include the final prompt template (v1)
- Explain the prompt structure and reasoning
- Note any prompt iterations done

### 5. Response Parsing Samples
Show at least 3 examples:
- A clean, well-structured LLM response → parsed result
- A response with Turkish format issues → normalized result  
- A problematic response → how it was handled

### 6. Test Results
- Unit test output summary
- Integration test output summary
- Number of tests passed/failed

### 7. Manual API Test Results (if performed)
| Invoice Image | Fields Extracted | Fields Correct | Fields Wrong | Accuracy % |
|---------------|-----------------|----------------|--------------|------------|
| sample_1.jpg  | 12              | 11             | 1            | 91.7%      |
| ...           | ...             | ...            | ...          | ...        |

### 8. Error Handling Verification
| Error Scenario | Expected Behavior | Actual Behavior | Status |
|---------------|-------------------|-----------------|--------|
| 30s timeout   | LLMTimeoutError   | ...             | ✅/❌  |
| 429 rate limit| LLMRateLimitError | ...             | ✅/❌  |
| ...           | ...               | ...             | ...    |

### 9. Database Changes
- List any new migration files created
- Or confirm "No database changes needed for this phase"

### 10. Issues Encountered
Problems and their solutions

### 11. Performance Metrics
- Average extraction time per image
- Average LLM response time
- Token usage per request (if available)

### 12. Next Steps
- What Phase 16 (Fallback Chain) needs from this phase
- Any prompt improvements identified for future iterations
- Any concerns or risks identified

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 13**: FastAPI service structure, configuration, error handling, Docker, CORS, API key env vars
- **Phase 14**: Image preprocessing pipeline (base64-encoded images ready for LLM)

### Required By
- **Phase 16**: LLM Fallback Chain — will use the same base_provider interface and prompt_manager to add GPT-5.2 and Claude Haiku 4.5
- **Phase 17**: Response Validation & Confidence Score — will consume InvoiceData model for deeper validation
- **Phase 19-A**: RabbitMQ Consumer — will use extraction_service for async processing
- **Phase 20**: File Upload Backend — will trigger extraction via HTTP

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Gemini 3 Flash API is callable from the Python service with an image + prompt
2. ✅ The extraction prompt produces structured JSON output for Turkish invoices
3. ✅ Response parser converts LLM output to a validated Pydantic model
4. ✅ Turkish number formats (comma decimal) are correctly normalized
5. ✅ Turkish date formats (DD.MM.YYYY) are correctly converted to YYYY-MM-DD
6. ✅ Turkish characters (Ç, ğ, ı, İ, ö, ş, ü) are preserved in extracted text
7. ✅ Error handling covers timeout, rate limit, server error, auth error, parse error
8. ✅ Retry logic works with exponential backoff
9. ✅ Abstract base provider interface is ready for Phase 16 fallback providers
10. ✅ POST /extract endpoint works end-to-end
11. ✅ All automated tests pass
12. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **Focus on Gemini Only**: This phase implements ONLY the Gemini provider. GPT and Claude are Phase 16. However, the abstract interface must be designed to accommodate them.

2. **Prompt is Iterative**: The first prompt version (v1) will likely not be perfect. Document its accuracy, and plan for refinement. The prompt_manager is designed to support versioning for this reason.

3. **No Confidence Score Yet**: Phase 17 handles confidence scoring. This phase only extracts data and returns it. The confidence_score field in the database will remain null until Phase 17.

4. **No Queue Integration Yet**: Phase 19 adds RabbitMQ async processing. This phase provides synchronous HTTP endpoints only.

5. **Mock Tests for CI/CD**: Never call the real Gemini API in automated tests. Use mocked responses. Real API testing is manual only.

6. **Check Model Availability**: Gemini model names change over time. At implementation time, verify the current model name in Google AI Studio. The prompt references `gemini-2.0-flash` but the actual available model might differ.

7. **Token Cost Awareness**: Log token usage if the SDK provides it. This data will be useful for cost monitoring features later.

8. **Spring Boot Does NOT Call Gemini Directly**: The Spring Boot backend sends the file to the Python service, which handles all LLM communication. Never put LLM API calls in the Java codebase.

9. **Base64 Size Limit**: Gemini has a per-request payload limit. The preprocessing pipeline (Phase 14) should have already optimized the image size. Verify that processed images stay within Gemini's limits.

10. **The Extraction Prompt Language**: The prompt itself should be in English (for better LLM performance), but it must instruct the LLM to recognize and preserve Turkish text from the invoice image.

---

**Phase 15 Completion Target**: A fully functional Gemini 3 Flash integration that accepts preprocessed invoice images, sends them with a well-crafted extraction prompt, and returns structured, normalized invoice data as a Pydantic model — ready for fallback chain extension in Phase 16 and validation in Phase 17.

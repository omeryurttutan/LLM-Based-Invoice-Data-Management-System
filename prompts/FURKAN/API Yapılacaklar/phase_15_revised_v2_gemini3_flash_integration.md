# PHASE 15 (REVISED V2): GEMINI 3 FLASH LLM INTEGRATION — SINGLE-STAGE MULTIMODAL EXTRACTION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using multimodal LLM-based analysis, eliminating manual data entry for accounting offices.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082 (host) / 8080 (container internal)
  - **Python Microservice (FastAPI)**: Port 8001 (host) / 8000 (container internal) - Extraction orchestrator
  - **Next.js Frontend**: Port 3001 (host) / 3000 (container internal)
  - **PostgreSQL**: Port 5436 (host) / 5432 (container internal)
  - **Redis**: Port 6380 (host) / 6379 (container internal)
  - **RabbitMQ**: Port 5673 (host) / 5672 (container internal), Management: 15673 (host) / 15672 (container internal)

### LLM Strategy (Revised)
The system uses a three-tier multimodal fallback chain for invoice data extraction:
1. **Primary**: Gemini 3 Flash — Google's latest multimodal model, best accuracy
2. **Fallback 1**: Gemini 2.5 Flash — Proven stable model, cheapest option ($0.10/1M input)
3. **Fallback 2**: GPT-5 nano — OpenAI's cost-efficient multimodal model ($0.05/1M input), strong structured output

All three models accept images directly (multimodal) — no separate OCR service needed.

### Current State (Phases 0-14 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend (layout, auth pages, invoice CRUD UI)
- ✅ Phase 13: Python FastAPI service setup — health endpoints, structured logging, error handling, CORS, Docker, API key env variables, Spring Boot ↔ FastAPI connectivity
- ✅ Phase 14: Image preprocessing pipeline — Pillow + PyMuPDF, format detection, PDF conversion, orientation fix, deskew, enhancement, size optimization, base64 encoding

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Integrate Gemini 3 Flash as the **primary multimodal LLM** for extracting structured invoice data directly from preprocessed images. Gemini receives the invoice image alongside a text prompt and returns structured JSON — performing both visual analysis and data extraction in a single API call.

**Single-Stage Pipeline:**
```
Invoice Image → Preprocessing (Phase 14) → Gemini 3 Flash API (image + prompt) → Structured JSON
```

There is no separate OCR service. Gemini 3 Flash's multimodal vision capability handles text recognition and data structuring in one step. This simplifies the pipeline and eliminates GPU infrastructure requirements on the server.

**This is the heart of the entire extraction system** — everything before this was preparation, and everything after builds on top of it.

---

## WHY GEMINI 3 FLASH AS PRIMARY

- **Latest Multimodal Model**: Google's newest Flash model with improved vision and reasoning
- **Native Image + PDF Support**: Accepts images and PDFs directly alongside text prompts
- **JSON Mode**: Supports structured output generation with JSON schema compliance
- **Turkish Support**: Good performance with Turkish language content
- **Fast**: Flash variants are optimized for speed — critical for real-time invoice processing
- **Generous Free Tier**: Free tier allows sufficient requests for development and testing
- **No GPU Required**: Cloud API — the server needs only CPU and network access

---

## SINGLE-STAGE PIPELINE ARCHITECTURE

```
Invoice Image (JPG/PNG/PDF)
       │
       ▼
┌──────────────────────────────┐
│  Phase 14: Preprocessing     │
│  (Pillow + PyMuPDF)          │
│  - Format detection          │
│  - Orientation fix           │
│  - Enhancement               │
│  - Base64 encoding           │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│  Gemini 3 Flash (API)        │  ← THIS PHASE
│  (Google AI Studio)          │
│  - Input: Image + Prompt     │
│  - Output: Structured JSON   │
│  - Multimodal (vision)       │
│  - Single API call           │
└──────────────┬───────────────┘
               │
               ▼
       Structured Invoice JSON
       (InvoiceData Pydantic model)
```

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add and modify files within the existing extraction-service structure:

```
extraction-service/app/
├── services/
│   ├── preprocessing/          ← Already exists from Phase 14
│   │   └── ...
│   ├── llm/
│   │   ├── __init__.py
│   │   ├── base_provider.py        ← NEW: Abstract base class for LLM providers
│   │   ├── gemini_provider.py      ← NEW: Gemini 3 Flash implementation
│   │   ├── prompt_manager.py       ← NEW: Prompt template management
│   │   └── response_parser.py      ← NEW: Parse and normalize LLM JSON responses
│   └── extraction/
│       ├── __init__.py
│       └── extraction_service.py   ← NEW: Orchestrates preprocessing → LLM → parsing
├── models/
│   ├── preprocessing.py            ← Already exists from Phase 14
│   ├── extraction.py               ← NEW: Extraction request/response DTOs
│   └── invoice_data.py             ← NEW: Structured invoice data model
├── config/
│   └── llm_config.py               ← NEW: LLM provider configurations
└── ...
```

### 2. Abstract LLM Provider Interface

Design the base provider class using the Strategy Pattern. This is critical because Phase 16 will add Gemini 2.5 Flash and GPT-5 nano as fallback providers using the same interface.

**base_provider.py must define:**
- An abstract method for sending an image (base64 + MIME type) + prompt and receiving raw text response — the LLM receives the invoice image directly for multimodal analysis
- An abstract method for getting the provider name (returns a string like "GEMINI_3_FLASH", "GEMINI_2_5_FLASH", "GPT5_NANO")
- An abstract method for checking if the provider is available/healthy
- Common configuration: timeout, max_retries, retry_delay
- Common error types that all providers should handle

**Provider Name Constants:**
- "GEMINI_3_FLASH" — maps to `llm_provider` column in invoices table (primary)
- "GEMINI_2_5_FLASH" — for Phase 16 first fallback
- "GPT5_NANO" — for Phase 16 second fallback

This abstraction is important: Phase 16 will add two more providers that implement this same interface.

**Key Design Point**: The base provider interface accepts **image input** (base64-encoded image + MIME type) alongside the text prompt. All three providers (Gemini 3 Flash, Gemini 2.5 Flash, GPT-5 nano) are multimodal and can process images directly.

### 3. Gemini 3 Flash Provider

Implement the Gemini 3 Flash provider using the official `google-generativeai` Python SDK.

**SDK**: Use the official `google-generativeai` Python package.

**API Configuration:**
- Model: `gemini-3-flash` (verify the exact model name at implementation time — it may be `gemini-3.0-flash` or `gemini-3.0-flash-preview`)
- API Key: Read from environment variable `GEMINI_API_KEY`
- Timeout: 30 seconds per request
- Max retries: 2 (with exponential backoff)
- Generation config: temperature=0.1 (low for accuracy), max_output_tokens=4096

**Multimodal Request Flow:**
1. Accept base64-encoded image and MIME type from preprocessing
2. Set the system instruction via `system_instruction` parameter in the model config
3. Construct a content array containing both the image (as inline data) and the text prompt
4. Call `generate_content` method
5. Receive text response (should be JSON)
6. Return raw response text for parsing

**Safety Settings:**
- Set all safety categories to BLOCK_NONE or minimum blocking (invoice text is business content, not harmful)

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

**a) System Message (Role Definition):**
- Tell the LLM it is an expert Turkish invoice data extraction system
- It receives invoice images and must visually analyze the document
- It must extract ALL relevant fields into a specific JSON format
- Emphasize that the image may be a scan, photograph, or digital document with varying quality

**b) User Message Content:**
- The invoice image (as inline data in the content array)
- The exact JSON output schema the LLM must follow
- Specific extraction instructions

**c) Expected JSON Output Schema:**
Define the exact JSON structure. The schema must match the database columns from Phase 3:

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

**d) Turkish Invoice-Specific Instructions:**
- Common Turkish invoice field labels to look for: "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Toplam", "KDV", "Ara Toplam", "Genel Toplam", "Vergi Dairesi", "VKN", "TCKN"
- Turkish number format: comma as decimal separator (1.234,56) — the LLM should convert to standard decimal format (1234.56) in the JSON
- Turkish date formats: DD.MM.YYYY, DD/MM/YYYY — always convert to YYYY-MM-DD in the JSON
- Currency detection: if not explicitly stated, default to "TRY"
- Tax rates commonly used in Turkey: 1%, 10%, 20%
- "KDV Dahil" means tax-inclusive, "KDV Hariç" means tax-exclusive

**e) Output Rules:**
- Return ONLY valid JSON, no markdown code blocks, no explanation text
- If a field is not found in the image, set it to null (not empty string)
- All monetary amounts must be numbers (not strings), using dot as decimal separator
- If multiple tax rates exist, calculate totals accordingly
- If items cannot be individually identified, create a single item with the totals

**f) Prompt Versioning:**
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
- Numbers: Ensure all monetary values are float/decimal, handle Turkish comma format if LLM did not convert
- Strings: Strip extra whitespace, normalize Turkish characters
- Null handling: Convert empty strings to None
- Currency: Uppercase, default to "TRY" if missing or unrecognized

**The parser should return a structured Pydantic model** (InvoiceData) that mirrors the JSON schema above. This model will be used by later phases (Phase 17 for validation, Phase 19 for queue messaging).

### 6. Extraction Service (Orchestrator)

Create an extraction service that orchestrates the full pipeline:

**Flow:**
1. Receive an image file (bytes or file path)
2. Call the preprocessing pipeline (Phase 14) to get a base64-encoded optimized image
3. Load the extraction prompt from prompt_manager
4. Call the Gemini 3 Flash provider with the image + prompt
5. Parse the response using response_parser
6. Return an ExtractionResult containing:
   - The parsed InvoiceData
   - The LLM provider used ("GEMINI_3_FLASH")
   - Total processing duration in milliseconds
   - LLM processing duration in milliseconds
   - The prompt version used
   - Raw LLM response (for debugging/audit)

### 7. FastAPI Endpoints

Add extraction endpoints to the existing FastAPI app (running on port 8001 host / 8000 container internal):

**POST /extract**
- Accepts: multipart/form-data with an image file
- Processing: preprocess → Gemini 3 Flash (image + prompt) → parse → return
- Response: JSON with extracted invoice data, provider info, processing time

**POST /extract/base64**
- Accepts: JSON body with base64-encoded image and MIME type
- Same processing flow
- Useful for when Spring Boot (port 8082) sends pre-encoded images

**GET /extract/prompt-info**
- Returns the current prompt version and template info
- Useful for debugging and monitoring

**GET /extract/pipeline-status**
- Returns health status of the LLM service
- Useful for monitoring

**Error Responses:**
- 400: Invalid file format or corrupted image
- 408: LLM timeout after all retries
- 429: Rate limit exceeded on LLM provider
- 500: Unexpected processing error
- 503: LLM service unavailable

### 8. Configuration Management

All configurations should be manageable via environment variables:

**Gemini Provider:**
- `GEMINI_API_KEY`: API key from Google AI Studio
- `GEMINI_MODEL`: Model name (default: `gemini-3-flash`)
- `GEMINI_TIMEOUT`: Request timeout in seconds (default: `30`)
- `GEMINI_MAX_RETRIES`: Max retry attempts (default: `2`)
- `GEMINI_TEMPERATURE`: Generation temperature (default: `0.1`)
- `GEMINI_MAX_OUTPUT_TOKENS`: Max response tokens (default: `4096`)

**General:**
- `LLM_DEFAULT_PROVIDER`: Default LLM provider (default: `GEMINI_3_FLASH`)

### 9. Logging Requirements

Log these events with structured logging (already set up in Phase 13):

**INFO level:**
- Extraction request received (file type, file size)
- Preprocessing completed (duration)
- LLM request sent to Gemini (model, prompt version)
- LLM response received (duration, response size)
- Extraction completed successfully (total duration, LLM duration)

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
- Full prompt sent to Gemini
- Full raw response from Gemini
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

**Note:** Do NOT install `openai` SDK in this phase. GPT-5 nano is added in Phase 16 as a fallback.

---

## TESTING REQUIREMENTS

### Test Categories

**1. Unit Tests for Gemini Provider:**
- Test successful API call (mock Gemini SDK response)
- Test timeout handling
- Test rate limit handling (mock a 429 response)
- Test server error handling (mock a 5xx response)
- Test authentication error handling (mock invalid API key)
- Test retry logic (verify retries happen with correct delays)
- Test that image is correctly sent as inline data in the request

**2. Unit Tests for Prompt Manager:**
- Test prompt template loading
- Test prompt version tracking
- Test prompt contains all required field instructions
- Test that the prompt includes the correct JSON schema

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

**4. Integration Test for Full Pipeline:**
- Test full flow: image → preprocess → LLM → parsed result
- Mock Gemini API response
- Verify ExtractionResult contains all expected fields including LLM duration
- Verify provider name is "GEMINI_3_FLASH"

**5. Manual Testing with Real Services (not automated):**
- Test with at least 5 different real Turkish invoice images
- Record accuracy for each field
- Document which fields have low accuracy (for prompt iteration)

**Note:** For CI/CD, mock ALL external calls (Gemini API). Real testing is manual only.

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at the following path:

```
docs/FURKAN/step_results/faz_15_1_result.md
```

Include the following sections:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date and actual time vs estimated (3-4 days)

### 2. Completed Tasks
Checklist of all tasks with status

### 3. Files Created/Modified
List all new and modified files with full paths

### 4. Pipeline Performance
| Metric | Value |
|--------|-------|
| Average LLM time (Gemini 3 Flash) | ... ms |
| Average total extraction time | ... ms |
| Gemini token usage per invoice | ... tokens |

### 5. Prompt Design
- Include the final prompt template (v1)
- Explain the prompt structure and reasoning
- Note any prompt iterations done

### 6. Response Parsing Samples
Show at least 3 examples:
- Gemini JSON → parsed InvoiceData
- A response with Turkish format issues → normalized result
- A problematic response → how it was handled

### 7. Test Results
- Unit test output summary (per test file)
- Integration test output summary
- Total tests passed/failed

### 8. Manual API Test Results (if performed)
| Invoice Image | Fields Extracted | Fields Correct | Fields Wrong | Accuracy % |
|---------------|-----------------|----------------|--------------|------------|
| sample_1.jpg  | 12              | 11             | 1            | 91.7%      |
| ...           | ...             | ...            | ...          | ...        |

### 9. Error Handling Verification
| Error Scenario | Expected Behavior | Actual Behavior | Status |
|---------------|-------------------|-----------------|--------|
| LLM timeout   | LLMTimeoutError   | ...             | ✅/❌  |
| 429 rate limit| LLMRateLimitError | ...             | ✅/❌  |

### 10. Database Changes
- Confirm "No database changes needed for this phase"

### 11. Issues Encountered
Problems and their solutions

### 12. Next Steps
- What Phase 16 (Fallback Chain) needs from this phase
- Any prompt improvements identified for future iterations

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 13**: FastAPI service structure, configuration, error handling, Docker, CORS, API key env vars
- **Phase 14**: Image preprocessing pipeline (base64-encoded images ready for LLM)

### Required By
- **Phase 16 (Revised V2)**: Fallback Chain — will add Gemini 2.5 Flash and GPT-5 nano as fallback providers using the same base_provider interface
- **Phase 17**: Response Validation & Confidence Score — validates InvoiceData from any provider
- **Phase 19-A**: RabbitMQ Consumer — uses the extraction_service for async processing
- **Phase 20**: File Upload Backend — triggers extraction via HTTP

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Gemini 3 Flash API is callable from the Python service with an image + prompt
2. ✅ The extraction prompt produces structured JSON output for Turkish invoices
3. ✅ Response parser converts LLM output to a validated Pydantic model
4. ✅ Turkish number formats (comma decimal) are correctly normalized
5. ✅ Turkish date formats (DD.MM.YYYY) are correctly converted to YYYY-MM-DD
6. ✅ Turkish characters (Ç, ğ, ı, İ, ö, ş, ü) are preserved in extracted text
7. ✅ Error handling covers LLM timeout, rate limit, server error, auth error, parse error
8. ✅ Retry logic works with exponential backoff
9. ✅ Abstract base provider interface is ready for Phase 16 fallback providers (Gemini 2.5 Flash, GPT-5 nano)
10. ✅ POST /extract endpoint works end-to-end
11. ✅ All automated tests pass (with mocked Gemini)
12. ✅ Result file is created at `docs/FURKAN/step_results/faz_15_1_result.md`

---

## IMPORTANT NOTES

1. **Single-Stage Pipeline**: This phase uses a single-stage pipeline where Gemini 3 Flash receives the invoice image directly and returns structured JSON. There is no separate OCR service — Gemini handles both visual understanding and data extraction in one call.

2. **No Separate OCR Service**: The earlier design included a separate PaddleOCR-VL-1.5 microservice (Phase 14-B). This has been removed from the architecture. Gemini handles everything. Phase 14-B does NOT exist.

3. **Focus on Gemini 3 Flash Only**: This phase implements ONLY the Gemini 3 Flash provider. Gemini 2.5 Flash and GPT-5 nano as fallbacks are Phase 16. However, the abstract interface must be designed to accommodate them.

4. **Prompt is Iterative**: The first prompt version (v1) will likely not be perfect. Document its accuracy, and plan for refinement. The prompt_manager supports versioning for this reason.

5. **No Confidence Score Yet**: Phase 17 handles confidence scoring. This phase only extracts data and returns it.

6. **No Queue Integration Yet**: Phase 19 adds RabbitMQ async processing. This phase provides synchronous HTTP endpoints only.

7. **Mock Everything in Tests**: Never call the real Gemini API in automated tests. Use mocked responses.

8. **Check Model Names**: Gemini model names change frequently. Verify the current available model name at implementation time. The prompt references `gemini-3-flash` but the actual model string might differ (e.g., `gemini-3.0-flash`, `gemini-3.0-flash-preview`).

9. **Spring Boot Does NOT Call Gemini Directly**: The Spring Boot backend (port 8082) sends the file to the Python FastAPI service (port 8001), which orchestrates preprocessing and LLM extraction. Never put LLM API calls in the Java codebase.

10. **The Extraction Prompt Language**: The prompt itself should be in English (for better LLM performance), but it must instruct the LLM to recognize and preserve Turkish text from the invoice image.

11. **No GPU Required on Server**: Since Gemini is a cloud API, the extraction service runs purely on CPU. No NVIDIA GPU, no CUDA, no model downloads. This simplifies deployment significantly.

12. **Port Configuration**: The FastAPI extraction service listens on port 8000 inside the Docker container and is mapped to port 8001 on the host. Spring Boot connects to FastAPI via Docker internal networking at port 8000. From the host machine (for development/testing), use port 8001.

---

**Phase 15 Completion Target**: A fully functional single-stage extraction pipeline using Gemini 3 Flash's multimodal API — that accepts invoice images, sends them directly to Gemini for visual analysis and JSON structuring, and returns structured, normalized invoice JSON data via a Pydantic model — ready for fallback chain extension in Phase 16 and validation in Phase 17.

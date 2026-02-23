# PHASE 16 (REVISED V2): LLM FALLBACK CHAIN — GEMINI 2.5 FLASH + GPT-5 NANO AS FALLBACKS

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using multimodal LLM-based analysis.

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

### Current State (Phases 0-15 Revised V2 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend
- ✅ Phase 13: Python FastAPI service setup
- ✅ Phase 14: Image preprocessing pipeline
- ✅ Phase 15 (Revised V2): Gemini 3 Flash multimodal integration — single-stage pipeline (image + prompt → JSON), abstract base provider interface (Strategy Pattern), Gemini 3 Flash provider using google-generativeai SDK with vision input, invoice extraction prompt for image-to-JSON, response parser with Turkish number/date normalization, extraction orchestrator service, POST /extract endpoints, custom error hierarchy (LLMError subtypes), retry logic, InvoiceData Pydantic model, ExtractionResult model

### What Phase 15 (Revised V2) Delivered (Available for This Phase)
- **base_provider.py**: Abstract base class — extract method (image base64 + MIME type + prompt → raw text), get_provider_name, is_available, common config
- **gemini_provider.py**: Gemini 3 Flash implementation using google-generativeai SDK with multimodal vision input
- **prompt_manager.py**: Prompt template management with versioning
- **response_parser.py**: Parses raw LLM JSON text into InvoiceData Pydantic model
- **extraction_service.py**: Orchestrator (preprocessing → LLM → parsing)
- **Custom error classes**: LLMError hierarchy (LLMTimeoutError, LLMRateLimitError, LLMServerError, LLMConnectionError, LLMAuthenticationError, LLMResponseError)
- **models/invoice_data.py**: InvoiceData Pydantic model
- **models/extraction.py**: ExtractionResult model

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3 days

---

## OBJECTIVE

Implement **Gemini 2.5 Flash** and **GPT-5 nano** as multimodal fallback LLM providers, then build a Fallback Chain Manager that automatically cascades through providers when one fails. The chain order is: **Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano**.

All three providers receive the same preprocessed image directly — no separate OCR stage. Each provider performs visual analysis and JSON extraction independently.

---

## WHY THIS SPECIFIC FALLBACK ORDER

### 1. Gemini 3 Flash (Primary)
- Google's latest and most capable Flash model
- Best accuracy on multimodal tasks

### 2. Gemini 2.5 Flash (Fallback 1)
- Proven stable model, cheapest option: $0.10/1M input, $0.40/1M output
- Structured Outputs (JSON Schema) support
- Same SDK and API key as primary — minimal additional code
- Different model version may still work when 3 Flash has issues

### 3. GPT-5 nano (Fallback 2)
- OpenAI's cost-efficient multimodal model: $0.05/1M input, $0.40/1M output
- Strong structured output guarantees (JSON Schema enforcement)
- Completely different infrastructure from Google — true redundancy
- If all Google services are down, OpenAI likely still works

---

## FALLBACK CHAIN ARCHITECTURE

```
Invoice Image (preprocessed, base64)
       │
       ▼
┌──────────────────────────┐
│  Gemini 3 Flash          │ ← Priority 1 (primary, latest model)
│  (GEMINI_3_FLASH)        │
│  Image + Prompt → JSON   │
└──────────┬───────────────┘
           │
    Success? ──Yes──→ Return Result (llm_provider="GEMINI_3_FLASH")
           │
          No (timeout / 5xx / network error / parse error)
           │
    Wait 2 seconds
           │
           ▼
┌──────────────────────────┐
│  Gemini 2.5 Flash        │ ← Priority 2 (stable, cheapest)
│  (GEMINI_2_5_FLASH)      │
│  Image + Prompt → JSON   │
└──────────┬───────────────┘
           │
    Success? ──Yes──→ Return Result (llm_provider="GEMINI_2_5_FLASH")
           │
          No
           │
    Wait 2 seconds
           │
           ▼
┌──────────────────────────┐
│  GPT-5 nano              │ ← Priority 3 (different infra, redundancy)
│  (GPT5_NANO)             │
│  Image + Prompt → JSON   │
└──────────┬───────────────┘
           │
    Success? ──Yes──→ Return Result (llm_provider="GPT5_NANO")
           │
          No
           │
           ▼
   AllProvidersFailedError
```

**Critical Rules:**
- Strictly sequential — NOT parallel.
- Each provider gets internal retry attempts (default 2) BEFORE falling to next.
- 2-second delay between providers (configurable).
- Unparseable JSON response counts as failure → triggers fallback.
- Authentication errors skip provider immediately without retries.
- Same preprocessed base64 image passed to all providers.

---

## DETAILED REQUIREMENTS

### 1. New Files to Create

```
extraction-service/app/
├── services/
│   ├── llm/
│   │   ├── base_provider.py              ← Already exists (Phase 15)
│   │   ├── gemini_provider.py            ← Already exists (Phase 15) — Gemini 3 Flash
│   │   ├── gemini_2_5_flash_provider.py  ← NEW: Gemini 2.5 Flash
│   │   ├── openai_provider.py            ← NEW: GPT-5 nano
│   │   ├── fallback_chain.py             ← NEW: Fallback chain manager
│   │   ├── provider_health.py            ← NEW: Provider health tracking
│   │   ├── prompt_manager.py             ← MODIFY: Add provider-specific prompt adaptations
│   │   └── response_parser.py            ← Already exists (Phase 15)
│   └── extraction/
│       └── extraction_service.py         ← MODIFY: Use fallback chain
├── models/
│   ├── extraction.py                     ← MODIFY: Add fallback attempt details
│   └── provider_status.py               ← NEW: Provider health/status models
└── config/
    └── llm_config.py                     ← MODIFY: Add new provider configs
```

### 2. Gemini 2.5 Flash Provider

**SDK**: Same `google-generativeai` package (already installed).

**API Configuration:**
- Model: `gemini-2.5-flash` (verify exact name at implementation time)
- API Key: Same `GEMINI_API_KEY` (shared with Gemini 3 Flash)
- Timeout: 30s, Max retries: 2, Temperature: 0.1, Max output tokens: 4096

**Multimodal Request** — same approach as Gemini 3 Flash: base64 image sent as inline data alongside text prompt via `generate_content`.

**Key Note**: Both Gemini providers share `GEMINI_API_KEY`. If Google's API is completely down, BOTH fail — that's why GPT-5 nano exists on different infrastructure.

**Provider Name:** "GEMINI_2_5_FLASH"

### 3. GPT-5 nano Provider (OpenAI API)

**SDK**: Official `openai` Python package.

**API Configuration:**
- Model: `gpt-5-nano` (verify exact name at implementation time)
- API Key: `OPENAI_API_KEY`
- Timeout: 30s, Max retries: 2, Temperature: 0.1, Max tokens: 4096

**Multimodal Request:**
- Image sent as base64 data URL in user message content array (type "image_url")
- Text prompt as content block with type "text"
- Format: `data:{mime_type};base64,{base64_data}`
- System message contains extraction instructions

**Provider Name:** "GPT5_NANO"

**Prompt Adaptation:**
- Core prompt content identical across all providers
- Only API-specific message structure changes (Gemini format vs OpenAI format)
- prompt_manager returns provider-adapted version

### 4. Fallback Chain Manager

**Chain Manager Responsibilities:**
- Accept providers in priority order (default: [Gemini 3 Flash, Gemini 2.5 Flash, GPT-5 nano])
- Accept preprocessed base64 image (same for all providers)
- Try each provider sequentially until success
- Track attempt details, enforce 2-second delay between attempts
- Return successful result with metadata about all attempts

**Attempt Tracking per provider:** provider name, success/fail, error type, error message, duration, retry count

**AllProvidersFailedError:** List of all attempts, total time, user suggestion

**Skip Logic:** Auth error → skip immediately. UNHEALTHY status → skip (but try if all healthy fail).

### 5. Provider Health Tracking

**Health States:** HEALTHY / DEGRADED / UNHEALTHY

**Sliding window of last 10 requests:**
- HEALTHY: 8+ successes / 10
- DEGRADED: 4-7 successes / 10
- UNHEALTHY: 0-3 successes / 10

**Recovery:** UNHEALTHY retried every 5 min. Success → DEGRADED. 3 consecutive successes → HEALTHY.

### 6. Update Extraction Service

**Updated Flow:**
1. Receive image → Preprocess → base64
2. Pass to Fallback Chain Manager
3. Chain tries providers in order
4. Parse successful response
5. Return ExtractionResult with attempt details

**ExtractionResult additions:** `fallback_used` (bool), `attempts` (list), `total_providers_tried` (int)

### 7. New API Endpoints

**GET /providers** — list configured providers + status + chain order

**GET /providers/health** — detailed health metrics per provider

### 8. Configuration

**Gemini 2.5 Flash:**
- `GEMINI_2_5_FLASH_MODEL`: default `gemini-2.5-flash`
- `GEMINI_2_5_FLASH_TIMEOUT`: default `30`
- `GEMINI_2_5_FLASH_MAX_RETRIES`: default `2`
- `GEMINI_2_5_FLASH_TEMPERATURE`: default `0.1`
- `GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS`: default `4096`

**GPT-5 nano:**
- `OPENAI_API_KEY`: OpenAI API key
- `OPENAI_MODEL`: default `gpt-5-nano`
- `OPENAI_TIMEOUT`: default `30`
- `OPENAI_MAX_RETRIES`: default `2`
- `OPENAI_TEMPERATURE`: default `0.1`
- `OPENAI_MAX_OUTPUT_TOKENS`: default `4096`

**Fallback Chain:**
- `FALLBACK_DELAY_SECONDS`: default `2`
- `FALLBACK_CHAIN_ORDER`: default `GEMINI_3_FLASH,GEMINI_2_5_FLASH,GPT5_NANO`

### 9. Dependencies

Add: `openai` — OpenAI's official Python SDK (for GPT-5 nano)

---

## TESTING REQUIREMENTS

**1. Unit Tests for Gemini 2.5 Flash Provider:**
- Successful API call, timeout, rate limit, correct model name, shared API key

**2. Unit Tests for GPT-5 nano Provider:**
- Successful API call, timeout, rate limit, server error, auth error, image format

**3. Unit Tests for Fallback Chain:**
- Happy path (primary succeeds), single fallback, double fallback, all fail
- 2-second delay, auth error skip, parse error fallback, attempt tracking
- Same base64 image passed to all providers

**4. Unit Tests for Provider Health:**
- State transitions, sliding window, recovery, health probes

**5. Integration Tests:**
- Full pipeline with mocked LLMs, fallback scenarios

**Note:** Mock everything. Never call real APIs.

---

## RESULT FILE REQUIREMENTS

Create result file at:

```
docs/FURKAN/step_results/faz_16_1_result.md
```

Include: Execution Status, Completed Tasks, Files Created/Modified, Provider Implementation Summary, Fallback Chain Test Results, Test Results, Issues Encountered, Next Steps.

---

## DEPENDENCIES

### Requires
- **Phase 13**: FastAPI service, Docker, env variables
- **Phase 14**: Image preprocessing pipeline
- **Phase 15 (Revised V2)**: Base provider interface, Gemini 3 Flash, prompt manager, response parser, extraction service, error hierarchy, models

### Required By
- **Phase 17**: Response Validation & Confidence Score
- **Phase 19-A**: RabbitMQ Consumer
- **Phase 20**: File Upload Backend
- **Phase 30-A**: Template Learning

---

## SUCCESS CRITERIA

1. ✅ Gemini 2.5 Flash provider implemented, extracts invoice JSON from images
2. ✅ GPT-5 nano provider implemented, extracts invoice JSON from images
3. ✅ Same extraction prompt works across all three providers
4. ✅ Fallback chain cascades: Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano
5. ✅ 2-second delay between attempts enforced
6. ✅ Auth errors skip provider immediately
7. ✅ Parse errors trigger fallback
8. ✅ AllProvidersFailedError with details when all fail
9. ✅ ExtractionResult includes provider name and attempt details
10. ✅ Provider health tracking works
11. ✅ GET /providers and GET /providers/health endpoints work
12. ✅ All providers receive same preprocessed image
13. ✅ All automated tests pass
14. ✅ Result file created at `docs/FURKAN/step_results/faz_16_1_result.md`

---

## IMPORTANT NOTES

1. **Reuse Phase 15 Components**: Extend, don't rewrite. base_provider, response_parser, prompt_manager were designed for extension.

2. **Same Prompt, Different Packaging**: Core prompt identical. Only API message structure changes.

3. **All Three Are Multimodal**: All receive invoice image directly. Best quality from each.

4. **Gemini Providers Share API Key**: Same `GEMINI_API_KEY`. If Google down, both fail → GPT-5 nano (OpenAI) provides true redundancy.

5. **Mock Everything in Tests**: Never call real APIs.

6. **Check Model Names**: Verify at implementation time. Names change frequently.

7. **Cost Awareness**: Primary = best quality. Fallbacks = cheapest. Never call fallbacks "just in case."

8. **No GPU Required**: All cloud APIs. CPU only on server.

9. **Port Configuration**: FastAPI at 8000 (container) / 8001 (host). Spring Boot at 8080 (container) / 8082 (host). Inter-service communication via Docker network uses internal ports.

---

**Phase 16 Completion Target**: A resilient, three-tier LLM fallback chain (Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano) where all providers directly analyze invoice images via their multimodal APIs — guaranteeing extraction as long as at least one provider is available.

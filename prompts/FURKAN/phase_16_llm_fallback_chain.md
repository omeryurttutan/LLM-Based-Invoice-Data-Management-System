# PHASE 16: LLM FALLBACK CHAIN (GPT-5.2 + CLAUDE HAIKU 4.5)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 - LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-15 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, RBAC, CRUD, frontend (layout, auth pages, invoice CRUD UI)
- ✅ Phase 13: Python FastAPI service setup — health endpoints, structured logging, error handling, CORS, Docker, API key env variables, Spring Boot connectivity
- ✅ Phase 14: Image preprocessing pipeline — Pillow + PyMuPDF, format detection, PDF conversion, orientation fix, deskew, enhancement, size optimization, base64 encoding
- ✅ Phase 15: Gemini 3 Flash LLM integration — abstract base provider interface (Strategy Pattern), Gemini multimodal API client, invoice extraction prompt (versioned, Turkish-specific), response parser with Turkish number/date normalization, extraction orchestrator service, POST /extract and POST /extract/base64 endpoints, custom error hierarchy (LLMTimeoutError, LLMRateLimitError, LLMServerError, LLMConnectionError, LLMAuthenticationError, LLMResponseError), retry logic with exponential backoff, InvoiceData Pydantic model, ExtractionResult model

### What Phase 15 Delivered (Available for This Phase)
The following components from Phase 15 are ready and must be reused:
- **base_provider.py**: Abstract base class defining the LLM provider interface — extract method (image + prompt → raw text), get_provider_name method, is_available method, common config (timeout, retries, delay)
- **gemini_provider.py**: Concrete Gemini implementation of the base provider
- **prompt_manager.py**: Prompt template management with versioning — the same prompt will be adapted for GPT and Claude
- **response_parser.py**: Parses raw LLM JSON text into InvoiceData Pydantic model — handles markdown stripping, Turkish format normalization, null handling
- **extraction_service.py**: Orchestrator that ties preprocessing → LLM → parsing together
- **Custom error classes**: LLMError base, LLMTimeoutError, LLMRateLimitError, LLMServerError, LLMConnectionError, LLMAuthenticationError, LLMResponseError
- **models/invoice_data.py**: InvoiceData Pydantic model matching the database invoice fields
- **models/extraction.py**: ExtractionResult model (invoice_data, provider, duration, prompt_version, raw_response)

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 3 days

---

## OBJECTIVE

Implement GPT-5.2 and Claude Haiku 4.5 as fallback LLM providers, then build a Fallback Chain Manager that automatically cascades through providers when one fails. The chain order is: Gemini 3 Flash → GPT-5.2 → Claude Haiku 4.5. The system must guarantee that if any single provider is available, the user gets a result.

This phase transforms the single-provider system from Phase 15 into a resilient, multi-provider extraction engine.

---

## FALLBACK CHAIN ARCHITECTURE

The fallback chain follows a strict sequential cascade model:

```
Extraction Request
       │
       ▼
┌──────────────────┐
│  Gemini 3 Flash  │ ← Priority 1 (primary, cheapest)
│  (GEMINI)        │
└────────┬─────────┘
         │
    Success? ──Yes──→ Return Result (provider="GEMINI")
         │
        No (timeout / 5xx / 429 / network error)
         │
    Wait 2 seconds
         │
         ▼
┌──────────────────┐
│    GPT-5.2       │ ← Priority 2 (first fallback)
│    (GPT)         │
└────────┬─────────┘
         │
    Success? ──Yes──→ Return Result (provider="GPT")
         │
        No
         │
    Wait 2 seconds
         │
         ▼
┌──────────────────┐
│ Claude Haiku 4.5 │ ← Priority 3 (last resort)
│   (CLAUDE)       │
└────────┬─────────┘
         │
    Success? ──Yes──→ Return Result (provider="CLAUDE")
         │
        No
         │
         ▼
   AllProvidersFailedError
   (return error to user with details of each failure)
```

**Critical rules:**
- The chain does NOT call all providers in parallel. It is strictly sequential.
- Each provider gets its own internal retry attempts (from Phase 15 base config — default 2 retries) BEFORE falling to the next provider.
- Between providers, wait exactly 2 seconds (configurable).
- If a provider returns a successful response but the response cannot be parsed into valid JSON, that counts as a provider failure and triggers fallback.
- Authentication errors (invalid API key) on a provider should skip that provider immediately without retries.

---

## DETAILED REQUIREMENTS

### 1. New Files to Create

Add to the existing extraction-service structure:

```
extraction-service/app/
├── services/
│   ├── llm/
│   │   ├── base_provider.py          ← Already exists (Phase 15)
│   │   ├── gemini_provider.py        ← Already exists (Phase 15)
│   │   ├── openai_provider.py        ← NEW: GPT-5.2 implementation
│   │   ├── anthropic_provider.py     ← NEW: Claude Haiku 4.5 implementation
│   │   ├── fallback_chain.py         ← NEW: Fallback chain manager
│   │   ├── provider_health.py        ← NEW: Provider health tracking
│   │   ├── prompt_manager.py         ← MODIFY: Add provider-specific prompt adaptations
│   │   └── response_parser.py        ← Already exists (Phase 15)
│   └── extraction/
│       └── extraction_service.py     ← MODIFY: Use fallback chain instead of single provider
├── models/
│   ├── invoice_data.py               ← Already exists (Phase 15)
│   ├── extraction.py                 ← MODIFY: Add fallback attempt details
│   └── provider_status.py            ← NEW: Provider health/status models
└── config/
    └── llm_config.py                 ← MODIFY: Add GPT and Claude configs
```

### 2. GPT-5.2 Provider (OpenAI API)

Implement the GPT provider by extending the abstract base_provider from Phase 15.

**SDK**: Use the official `openai` Python package.

**API Configuration:**
- Model: `gpt-5.2` (or latest available — verify at implementation time)
- API Key: Read from environment variable `OPENAI_API_KEY` (already configured in Phase 13)
- Timeout: 30 seconds
- Max retries: 2 (with exponential backoff, same as Gemini)
- Temperature: 0.1 (same as Gemini for consistency)
- Max tokens: 4096

**Multimodal Request:**
- OpenAI's vision API accepts images as base64 data URLs within the messages array
- The image must be sent as a "user" message content block with type "image_url"
- The text prompt is sent as another content block with type "text" in the same message
- The base64 image from preprocessing needs to be wrapped in the format: "data:{mime_type};base64,{base64_data}"

**Provider Name:** Must return "GPT" — this value will be stored in the invoices.llm_provider column.

**Error Handling:** Must raise the same custom error types from Phase 15 (LLMTimeoutError, LLMRateLimitError, etc.) so the fallback chain can handle all providers uniformly.

**Prompt Adaptation:**
- OpenAI may handle the same prompt differently than Gemini
- The prompt_manager should have a method that returns a provider-adapted version of the extraction prompt
- For GPT, the core prompt content stays the same but the message structure follows OpenAI's chat format (system message + user message with image)
- The system message should contain the role definition and JSON schema
- The user message should contain the image and the extraction instruction

### 3. Claude Haiku 4.5 Provider (Anthropic API)

Implement the Claude provider by extending the abstract base_provider from Phase 15.

**SDK**: Use the official `anthropic` Python package.

**API Configuration:**
- Model: `claude-haiku-4-5-20251001` (or latest available — verify at implementation time)
- API Key: Read from environment variable `ANTHROPIC_API_KEY` (already configured in Phase 13)
- Timeout: 30 seconds
- Max retries: 2
- Temperature: 0.1
- Max tokens: 4096

**Multimodal Request:**
- Anthropic's API accepts images as base64 content blocks within the messages array
- The image must be sent as a content block with type "image" and source type "base64"
- The media_type must match the actual image format (e.g., "image/jpeg", "image/png")
- The text prompt is sent as another content block with type "text" in the same user message

**Provider Name:** Must return "CLAUDE"

**Error Handling:** Same custom error types as the other providers.

**Prompt Adaptation:**
- Claude uses a different message structure than OpenAI and Gemini
- Anthropic API uses a "system" parameter (separate from messages) for the system prompt
- User message contains both the image block and the text instruction block
- The system parameter should contain the role definition and JSON schema
- The user message should contain the image and extraction instruction

### 4. Prompt Adaptation Strategy

The extraction prompt designed in Phase 15 is the "master prompt." Each provider may need slight structural adaptations due to different API formats, but the **core content must remain identical** across all providers.

**What stays the same across all providers:**
- The JSON schema definition (exact same fields)
- Turkish invoice-specific instructions
- Number/date format conversion rules
- Null handling rules
- The requirement to output only valid JSON

**What changes per provider:**
- How the system role is communicated (generation config vs system message vs system parameter)
- How the image is packaged in the request (inline part vs image_url vs image content block)
- Minor wording adjustments if a provider consistently misunderstands certain instructions

**prompt_manager.py should be updated to have:**
- A method to get the base/master prompt text
- A method to get a provider-specific prompt package (returns the appropriately structured prompt for each provider's API format)
- The ability to version prompts per-provider if needed in the future (v1 for all initially)

### 5. Fallback Chain Manager

This is the core new component of Phase 16. Create fallback_chain.py.

**Responsibilities:**
- Maintain an ordered list of providers: [Gemini, GPT, Claude]
- Accept an extraction request (preprocessed image + metadata)
- Try each provider in order until one succeeds
- Wait the configured delay (default 2 seconds) between provider attempts
- Track each attempt's result (success/failure, error type, duration)
- Return the final ExtractionResult with the successful provider's data
- If all providers fail, raise AllProvidersFailedError with a summary of all failures

**Fallback Trigger Conditions — cascade to next provider when:**
- LLMTimeoutError: Provider did not respond within 30 seconds (after its own retries)
- LLMRateLimitError: Provider returned 429 (after waiting per Retry-After)
- LLMServerError: Provider returned 5xx (after its own retries)
- LLMConnectionError: Network/connection failure (after its own retries)
- LLMResponseError: Provider returned a response but it could not be parsed as valid JSON (after its own retries)

**Do NOT trigger fallback for:**
- LLMAuthenticationError: Skip this provider immediately (no retries), move to next. This means the API key is wrong — retrying won't help.
- Successful extraction: Obviously, stop the chain.

**Inter-Provider Delay:**
- Default: 2 seconds between provider attempts
- Configurable via environment variable `LLM_FALLBACK_DELAY_SECONDS`
- Purpose: Avoid cascading rate limits and give transient issues time to resolve

**Chain Configuration:**
- `LLM_CHAIN_ORDER`: Comma-separated provider names (default: "GEMINI,GPT,CLAUDE")
- `LLM_FALLBACK_DELAY_SECONDS`: Delay between providers (default: 2)
- `LLM_CHAIN_ENABLED`: Boolean to enable/disable fallback (default: true). When disabled, only the first provider is used.

### 6. Provider Health Tracking

Create provider_health.py to track provider availability.

**Purpose:** If a provider has been failing consistently, the chain can optionally skip it to save time. This is a "soft" optimization, not a hard block.

**Tracking Metrics per Provider:**
- Last success timestamp
- Last failure timestamp
- Failure count in the last N minutes (configurable, default 10 minutes)
- Current status: HEALTHY, DEGRADED, UNHEALTHY

**Status Determination:**
- HEALTHY: Less than 2 failures in the last 10 minutes
- DEGRADED: 2-4 failures in the last 10 minutes (still try, but log a warning)
- UNHEALTHY: 5+ failures in the last 10 minutes (optionally skip, but still try if all others fail)

**Important:** Even if a provider is marked UNHEALTHY, the chain should still attempt it as a last resort if all other providers also fail. Never completely disable a provider based on health alone.

**Health Endpoint:**
- GET /providers/health — returns the health status of all configured providers
- Useful for monitoring and debugging

### 7. Response Normalization

Each LLM provider may return slightly different JSON structures even with the same prompt. The response_parser from Phase 15 already handles basic parsing, but this phase must ensure normalization works across all three providers.

**Common Provider Differences to Handle:**
- Some providers wrap JSON in markdown code blocks (```json ... ```), others don't
- Number precision may vary (GPT might return 1234.50, Claude might return 1234.5)
- Null representation may differ (null vs "null" vs "" vs missing field)
- Array formatting may differ for items (empty array [] vs null vs missing)
- Some providers may add explanatory text before/after the JSON

**The response_parser should be robust enough to:**
- Strip any non-JSON content around the actual JSON object
- Handle all null/empty variations
- Normalize number precision to 2 decimal places for monetary values
- Ensure the items array is always present (empty array if no items found)
- Log a warning when normalization fixes are applied (helps track which providers need prompt adjustments)

### 8. Updated Extraction Service

Modify extraction_service.py to use the fallback chain instead of directly calling the Gemini provider.

**Updated Flow:**
1. Receive an image file (bytes or file path)
2. Call the preprocessing pipeline (Phase 14) — unchanged
3. Pass the preprocessed image to the fallback chain manager
4. The fallback chain tries providers in order and returns the result
5. Return an enhanced ExtractionResult containing:
   - The parsed InvoiceData (same as before)
   - The provider that succeeded (could be "GEMINI", "GPT", or "CLAUDE")
   - Total processing duration (including preprocessing + all LLM attempts)
   - The prompt version used
   - Raw LLM response from the successful provider
   - Fallback attempt details: list of each provider tried, whether it succeeded/failed, error type if failed, duration of each attempt

### 9. Updated/New API Endpoints

**Modify existing endpoints:**
- POST /extract — now uses the fallback chain. Response should include which provider was used and fallback attempt details.
- POST /extract/base64 — same update.

**Add new endpoints:**
- GET /providers — list all configured providers with their priority order and current health status
- GET /providers/health — detailed health check for each provider (last success, failure count, status)
- POST /providers/{provider_name}/test — send a simple test request to a specific provider to check if it's working (not a full extraction, just a quick health ping)

### 10. Configuration — New Environment Variables

Add these to .env and document in .env.example:

- `OPENAI_API_KEY`: OpenAI API key (already in .env from Phase 13)
- `OPENAI_MODEL`: Model name (default: gpt-5.2)
- `OPENAI_TIMEOUT`: Request timeout in seconds (default: 30)
- `OPENAI_MAX_RETRIES`: Max retry attempts (default: 2)
- `OPENAI_TEMPERATURE`: Generation temperature (default: 0.1)
- `OPENAI_MAX_TOKENS`: Max response tokens (default: 4096)

- `ANTHROPIC_API_KEY`: Anthropic API key (already in .env from Phase 13)
- `ANTHROPIC_MODEL`: Model name (default: claude-haiku-4-5-20251001)
- `ANTHROPIC_TIMEOUT`: Request timeout in seconds (default: 30)
- `ANTHROPIC_MAX_RETRIES`: Max retry attempts (default: 2)
- `ANTHROPIC_TEMPERATURE`: Generation temperature (default: 0.1)
- `ANTHROPIC_MAX_TOKENS`: Max response tokens (default: 4096)

- `LLM_CHAIN_ORDER`: Provider order (default: GEMINI,GPT,CLAUDE)
- `LLM_FALLBACK_DELAY_SECONDS`: Delay between providers (default: 2)
- `LLM_CHAIN_ENABLED`: Enable/disable fallback (default: true)
- `LLM_HEALTH_WINDOW_MINUTES`: Health tracking window (default: 10)
- `LLM_HEALTH_UNHEALTHY_THRESHOLD`: Failures to mark unhealthy (default: 5)

### 11. Logging Requirements

**INFO level:**
- Fallback chain started (image info, chain order)
- Provider attempt started (provider name, attempt number in chain)
- Provider attempt succeeded (provider name, duration)
- Fallback chain completed (final provider, total duration, number of providers tried)

**WARNING level:**
- Provider failed, falling back to next (provider name, error type, error message)
- Provider health status changed (provider name, old status → new status)
- Provider marked as DEGRADED or UNHEALTHY
- All providers exhausted — AllProvidersFailedError

**ERROR level:**
- Individual provider failure details (provider, error class, message, duration)
- AllProvidersFailedError with complete chain attempt summary
- Configuration errors (missing API key, invalid chain order)

**DEBUG level:**
- Inter-provider delay starting (duration)
- Provider health metrics update
- Full error details for each failed attempt

### 12. Dependencies — New Python Packages

Add to requirements.txt:
- `openai` — OpenAI official Python SDK
- `anthropic` — Anthropic official Python SDK

Update the Dockerfile to install these new packages.

---

## TESTING REQUIREMENTS

### 1. Unit Tests for OpenAI Provider
- Test successful API call (mock OpenAI SDK response)
- Test image packaging in OpenAI's expected format (data URL with base64)
- Test timeout handling → raises LLMTimeoutError
- Test rate limit handling → raises LLMRateLimitError
- Test server error handling → raises LLMServerError
- Test auth error handling → raises LLMAuthenticationError immediately (no retry)
- Test retry logic (verify correct number of retries and backoff timing)
- Test provider name returns "GPT"

### 2. Unit Tests for Anthropic Provider
- Test successful API call (mock Anthropic SDK response)
- Test image packaging in Anthropic's expected format (base64 content block)
- Test timeout handling → raises LLMTimeoutError
- Test rate limit handling → raises LLMRateLimitError
- Test server error handling → raises LLMServerError
- Test auth error handling → raises LLMAuthenticationError immediately
- Test retry logic
- Test provider name returns "CLAUDE"

### 3. Unit Tests for Fallback Chain
- Test happy path: first provider (Gemini) succeeds → no fallback triggered
- Test first fallback: Gemini fails with timeout → GPT succeeds → result has provider="GPT"
- Test second fallback: Gemini + GPT both fail → Claude succeeds → result has provider="CLAUDE"
- Test all fail: All three providers fail → AllProvidersFailedError raised with details of all three failures
- Test auth error skip: Gemini has auth error → immediately skips to GPT (no retries on Gemini)
- Test inter-provider delay: Verify 2 second delay between provider attempts (mock time/sleep)
- Test parse error fallback: Gemini returns unparseable response → falls back to GPT
- Test chain order configuration: Custom chain order "GPT,CLAUDE,GEMINI" → GPT is tried first
- Test chain disabled: When LLM_CHAIN_ENABLED=false → only first provider is tried
- Test fallback attempt details: Verify the ExtractionResult contains the correct attempt log for each provider

### 4. Unit Tests for Provider Health
- Test initial state is HEALTHY for all providers
- Test failure recording increments failure count
- Test success recording resets tracking
- Test DEGRADED threshold (2-4 failures in window)
- Test UNHEALTHY threshold (5+ failures in window)
- Test health window expiry (old failures drop off after window period)
- Test health endpoint returns correct status for each provider

### 5. Unit Tests for Prompt Adaptation
- Test that base prompt content is identical across all provider adaptations
- Test that Gemini prompt uses the Gemini SDK format
- Test that OpenAI prompt uses the chat completion format (system + user messages)
- Test that Anthropic prompt uses the Anthropic format (system param + user message)
- Test that all prompt versions contain the required JSON schema
- Test that all prompt versions contain Turkish-specific instructions

### 6. Integration Test for Full Fallback Flow
- Mock all three providers
- Test end-to-end: image → preprocess → fallback chain (first provider fails, second succeeds) → parsed result
- Verify ExtractionResult has correct provider, duration, and attempt details
- Verify response normalization works regardless of which provider succeeded

### Test File Structure:
```
extraction-service/tests/
├── unit/
│   ├── test_gemini_provider.py        ← Already exists (Phase 15)
│   ├── test_openai_provider.py        ← NEW
│   ├── test_anthropic_provider.py     ← NEW
│   ├── test_fallback_chain.py         ← NEW
│   ├── test_provider_health.py        ← NEW
│   ├── test_prompt_adaptation.py      ← NEW
│   ├── test_prompt_manager.py         ← Already exists (Phase 15)
│   └── test_response_parser.py        ← Already exists (Phase 15)
├── integration/
│   ├── test_extraction_flow.py        ← Already exists (Phase 15)
│   └── test_fallback_flow.py          ← NEW
└── fixtures/
    ├── mock_gemini_responses/         ← Already exists (Phase 15)
    ├── mock_openai_responses/         ← NEW
    │   ├── valid_response.json
    │   ├── response_with_markdown.txt
    │   └── malformed_response.txt
    └── mock_anthropic_responses/      ← NEW
        ├── valid_response.json
        ├── response_with_markdown.txt
        └── malformed_response.txt
```

---

## DATABASE CONSIDERATIONS

### Check: Does the existing schema support this phase?

The invoices table from Phase 3 already has:
- `llm_provider VARCHAR(20)` with CHECK constraint: `IN ('GEMINI', 'GPT', 'CLAUDE')`
- This is sufficient for storing which provider was used.

**Potential new requirement:** Consider whether a separate table or log for tracking fallback attempts per extraction would be useful. This could help with analytics (e.g., "how often does Gemini fail and we fall back to GPT?").

**Option A (Recommended for now):** No new migration. Store fallback attempt details in the application logs only. The invoices.llm_provider column records which provider ultimately succeeded.

**Option B (Optional, for future analytics):** Create a new table `llm_extraction_logs` to record each provider attempt. This would need a Flyway migration. Consider whether this adds enough value for the current project scope.

If Option B is chosen, the migration file should be:
- `V{next_number}__phase_16_llm_extraction_logs.sql`
- Columns: id, invoice_id (FK), provider_name, status (SUCCESS/FAILED), error_type, error_message, duration_ms, attempt_order, created_at

**Decision:** Discuss with Ömer whether to add this table. If yes, create the migration. If no, skip it and rely on structured logs.

---

## VERIFICATION CHECKLIST

### OpenAI Provider
- [ ] openai package installed and importable
- [ ] OPENAI_API_KEY loaded from environment
- [ ] GPT provider extends base_provider correctly
- [ ] Multimodal request sends image as data URL in chat completion format
- [ ] Provider returns name "GPT"
- [ ] All error types handled (timeout, rate limit, 5xx, auth, connection, parse)
- [ ] Retry logic works with exponential backoff
- [ ] Auth errors skip retries immediately

### Anthropic Provider
- [ ] anthropic package installed and importable
- [ ] ANTHROPIC_API_KEY loaded from environment
- [ ] Claude provider extends base_provider correctly
- [ ] Multimodal request sends image as base64 content block
- [ ] System prompt uses Anthropic's system parameter (not in messages)
- [ ] Provider returns name "CLAUDE"
- [ ] All error types handled correctly
- [ ] Retry logic works
- [ ] Auth errors skip retries immediately

### Fallback Chain
- [ ] Chain order is configurable (default: GEMINI, GPT, CLAUDE)
- [ ] Chain tries providers sequentially (not in parallel)
- [ ] 2-second delay between providers
- [ ] Each provider gets its own retry attempts before falling to next
- [ ] Auth errors skip provider immediately
- [ ] Parse errors trigger fallback
- [ ] AllProvidersFailedError raised when all fail
- [ ] AllProvidersFailedError includes details of each provider's failure
- [ ] Chain can be disabled (single provider mode)
- [ ] Successful result includes which provider was used

### Prompt Adaptation
- [ ] Core prompt content identical across all providers
- [ ] Gemini prompt uses Gemini SDK format
- [ ] OpenAI prompt uses chat completion format (system + user)
- [ ] Anthropic prompt uses system parameter + user message
- [ ] All versions include JSON schema
- [ ] All versions include Turkish instructions

### Provider Health
- [ ] Health tracking initializes all providers as HEALTHY
- [ ] Failures increment counter within time window
- [ ] Status transitions: HEALTHY → DEGRADED → UNHEALTHY
- [ ] Old failures expire after window period
- [ ] GET /providers/health returns correct status
- [ ] UNHEALTHY providers still tried as last resort

### Response Normalization
- [ ] GPT responses parsed correctly
- [ ] Claude responses parsed correctly
- [ ] Markdown stripping works for all providers
- [ ] Number precision normalized to 2 decimal places
- [ ] Null/empty handling consistent across providers
- [ ] Items array always present (even if empty)

### API Endpoints
- [ ] POST /extract uses fallback chain and returns provider info + attempt details
- [ ] POST /extract/base64 uses fallback chain
- [ ] GET /providers returns provider list with priority and health
- [ ] GET /providers/health returns detailed health per provider
- [ ] POST /providers/{name}/test pings specific provider

### Configuration
- [ ] All new env variables have sensible defaults
- [ ] Chain order customizable via LLM_CHAIN_ORDER
- [ ] Delay customizable via LLM_FALLBACK_DELAY_SECONDS
- [ ] Chain enable/disable via LLM_CHAIN_ENABLED
- [ ] Dockerfile updated with new dependencies
- [ ] requirements.txt updated with openai and anthropic packages

### Tests
- [ ] All OpenAI provider unit tests pass
- [ ] All Anthropic provider unit tests pass
- [ ] All fallback chain unit tests pass
- [ ] All provider health unit tests pass
- [ ] All prompt adaptation tests pass
- [ ] Integration test with mocked fallback flow passes
- [ ] Mock response fixtures are realistic for each provider

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_16_result.md`

Include the following sections:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date and actual time vs estimated (3 days)

### 2. Completed Tasks
Checklist of all tasks with status

### 3. Files Created/Modified
List all new and modified files with full paths

### 4. Provider Implementation Summary
| Provider | SDK | Model | API Format | Status |
|----------|-----|-------|------------|--------|
| Gemini   | google-generativeai | gemini-2.0-flash | Inline content | ✅ Phase 15 |
| GPT      | openai | gpt-5.2 | Chat completion | ✅/❌ |
| Claude   | anthropic | claude-haiku-4-5 | Messages API | ✅/❌ |

### 5. Fallback Chain Test Results
| Scenario | Expected Provider | Actual Provider | Duration | Status |
|----------|-------------------|-----------------|----------|--------|
| All healthy | GEMINI | ... | ... | ✅/❌ |
| Gemini down | GPT | ... | ... | ✅/❌ |
| Gemini + GPT down | CLAUDE | ... | ... | ✅/❌ |
| All down | Error | ... | ... | ✅/❌ |

### 6. Prompt Adaptation Details
- Describe how the master prompt was adapted for each provider
- Note any provider-specific adjustments needed
- Document prompt version for each provider

### 7. Response Normalization Comparison
Show the same invoice extracted by each provider (if manual testing done):
- Compare JSON output differences
- Note which fields each provider handles best/worst
- Document any normalization fixes applied

### 8. Test Results
- Unit test output summary (per test file)
- Integration test output summary
- Total tests passed/failed

### 9. Database Changes
- List any migration files created
- Or confirm "No database changes needed for this phase"
- Document decision on llm_extraction_logs table

### 10. Provider Health Tracking
- Show example health endpoint output
- Describe status transition logic

### 11. Issues Encountered
Problems and their solutions

### 12. Performance Metrics
| Provider | Avg Response Time | Avg Total Time (with retries) |
|----------|-------------------|-------------------------------|
| Gemini   | ... ms            | ... ms                        |
| GPT      | ... ms            | ... ms                        |
| Claude   | ... ms            | ... ms                        |

### 13. Next Steps
- What Phase 17 (Response Validation & Confidence Score) needs from this phase
- Any prompt improvements identified
- Any provider-specific concerns

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 13**: FastAPI service, Docker, env variables for all three API keys
- **Phase 14**: Image preprocessing pipeline (shared by all providers)
- **Phase 15**: Abstract base provider interface, Gemini implementation, prompt manager, response parser, extraction service, custom error hierarchy, InvoiceData model

### Required By
- **Phase 17**: Response Validation & Confidence Score — validates InvoiceData from any provider, uses provider name for analytics
- **Phase 19-A**: RabbitMQ Consumer — uses the fallback chain for async extraction
- **Phase 20**: File Upload Backend — triggers extraction which now uses fallback chain
- **Phase 30-A**: Template Learning — learns from successful extractions across all providers

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ GPT-5.2 provider is implemented, extends base_provider, and can extract invoice data from images
2. ✅ Claude Haiku 4.5 provider is implemented, extends base_provider, and can extract invoice data from images
3. ✅ The same extraction prompt works across all three providers (with structural adaptations)
4. ✅ Fallback chain cascades correctly: Gemini → GPT → Claude
5. ✅ 2-second delay between provider attempts is enforced
6. ✅ Auth errors skip the provider immediately without retries
7. ✅ Parse errors trigger fallback to the next provider
8. ✅ AllProvidersFailedError is raised when all providers fail, with details of each failure
9. ✅ ExtractionResult includes the provider that succeeded and attempt details for all tried providers
10. ✅ Provider health tracking works (HEALTHY / DEGRADED / UNHEALTHY)
11. ✅ GET /providers and GET /providers/health endpoints work
12. ✅ Response normalization handles differences between all three providers
13. ✅ All automated tests pass (with mocked providers)
14. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **Reuse Phase 15 Components**: Do NOT rewrite base_provider, response_parser, or prompt_manager from scratch. Extend and modify them. The abstract interface was designed in Phase 15 specifically for this extension.

2. **Same Prompt, Different Packaging**: The extraction prompt content (JSON schema, Turkish instructions, rules) must be identical. Only the API-specific message structure changes.

3. **Mock Everything in Tests**: Never call real OpenAI or Anthropic APIs in automated tests. Use mocked SDK responses. Manual testing with real APIs is separate.

4. **Check Model Names**: Model names change frequently. Verify the current available model names for OpenAI and Anthropic at implementation time. The prompt references gpt-5.2 and claude-haiku-4-5-20251001 but these may have been updated.

5. **Cost Awareness**: GPT-5.2 and Claude Haiku 4.5 have different pricing than Gemini. The fallback chain should only use them when Gemini fails. Never call multiple providers "just in case."

6. **No Confidence Score Yet**: Phase 17 handles confidence scoring. This phase focuses purely on getting a valid extraction result from any available provider.

7. **Provider Order Matters**: Gemini first because it's cheapest. GPT second because it's widely available. Claude third as the last resort. This order is configurable but the default should follow cost optimization.

8. **Anthropic System Prompt**: Unlike OpenAI, Anthropic's API has a separate "system" parameter. Do not put the system prompt inside the messages array for Claude. This is a common mistake that reduces quality.

9. **No Direct DB Access**: The Python service does not write to the database directly. It returns the ExtractionResult (including provider name) to the Spring Boot backend, which handles persistence. The llm_provider column in the invoices table will be populated by Spring Boot using the provider name from ExtractionResult.

10. **Graceful Degradation**: The system should ALWAYS try to return a result. Even if the first two providers fail, the third should still be attempted. Only fail completely when ALL providers are genuinely unavailable.

---

**Phase 16 Completion Target**: A resilient, three-tier LLM fallback chain that guarantees invoice data extraction as long as at least one provider is available — with clear health tracking, configurable chain ordering, and detailed attempt logging for debugging and monitoring.

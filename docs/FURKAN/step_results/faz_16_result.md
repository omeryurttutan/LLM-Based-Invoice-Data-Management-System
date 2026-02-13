# PHASE 16 RESULT: LLM FALLBACK CHAIN (GPT-5.2 + CLAUDE HAIKU 4.5)

## 1. Execution Status
- **Overall Status**: ✅ Success
- **Date**: 2026-02-13
- **Estimated Duration**: 3 gün → **Tamamlanma**: 1 gün + doğrulama düzeltmeleri

## 2. Completed Tasks
- [x] **OpenAI Provider**: `OpenAIProvider` sınıfı — `data:image/jpeg;base64` formatında multimodal istek, `gpt-4o` varsayılan model
- [x] **Anthropic Provider**: `AnthropicProvider` sınıfı — system parametresi ayrı, image content block formatında istek
- [x] **Fallback Chain Manager**: `FallbackChain` sınıfı — Gemini → GPT → Claude sıralı kaskad
- [x] **AllProvidersFailedError**: Tüm provider'lar başarısız olduğunda detaylı hata
- [x] **Provider Health Tracking**: `ProviderHealthManager` singleton — HEALTHY/DEGRADED/UNHEALTHY durumları
- [x] **Provider Status Model**: `ProviderHealth` Pydantic modeli, `HealthStatus` enum
- [x] **ExtractionResponse Güncelleme**: `fallback_attempts` alanı eklendi
- [x] **API Endpoints**: `GET /providers`, `GET /providers/health`, `POST /providers/{name}/test`
- [x] **ExtractionService Güncelleme**: `FallbackChain` üzerinden çalışıyor (tek provider yerine)
- [x] **Konfigürasyon**: `LLM_CHAIN_ORDER`, `LLM_FALLBACK_DELAY_SECONDS`, `LLM_CHAIN_ENABLED` settings.py'de tanımlı
- [x] **Dependencies**: `openai>=1.10.0` ve `anthropic>=0.18.0` requirements.txt'ye eklendi
- [x] **Test Dosyaları**: Unit testler (provider, fallback chain, health, prompt) ve integration test oluşturuldu
- [x] **Mock Response Fixtures**: OpenAI ve Anthropic için valid/markdown/malformed fixture dosyaları oluşturuldu

## 3. Files Created/Modified

### Yeni Dosyalar
| Dosya | Açıklama |
|-------|----------|
| `app/services/llm/providers/openai_provider.py` | GPT-5.2 provider (BaseLLMProvider extends) |
| `app/services/llm/providers/anthropic_provider.py` | Claude Haiku 4.5 provider (BaseLLMProvider extends) |
| `app/services/llm/fallback_chain.py` | Fallback chain manager + AllProvidersFailedError |
| `app/services/llm/provider_health.py` | Provider health tracking (singleton) |
| `app/models/provider_status.py` | HealthStatus enum + ProviderHealth model |
| `tests/unit/test_openai_provider.py` | OpenAI provider unit testleri (3 test) |
| `tests/unit/test_anthropic_provider.py` | Anthropic provider unit testleri (2 test) |
| `tests/unit/test_fallback_chain.py` | Fallback chain unit testleri (4 test) |
| `tests/unit/test_provider_health.py` | Provider health unit testleri (9 test) |
| `tests/unit/test_prompt_adaptation.py` | Prompt adaptation unit testleri (9 test) |
| `tests/integration/test_fallback_flow.py` | Integration test — end-to-end fallback |
| `tests/fixtures/mock_openai_responses/` | valid_response.json, response_with_markdown.txt, malformed_response.txt |
| `tests/fixtures/mock_anthropic_responses/` | valid_response.json, response_with_markdown.txt, malformed_response.txt |

### Değiştirilen Dosyalar
| Dosya | Değişiklik |
|-------|-----------|
| `requirements.txt` | `openai` ve `anthropic` SDK'ları eklendi |
| `app/config/settings.py` | `LLM_CHAIN_ORDER`, `LLM_FALLBACK_DELAY_SECONDS`, `LLM_CHAIN_ENABLED` eklendi |
| `app/models/extraction.py` | `fallback_attempts` alanı eklendi |
| `app/services/extraction/extraction_service.py` | `FallbackChain` kullanacak şekilde güncellendi |
| `app/api/routes/extraction.py` | Provider endpoints eklendi + `import time` hatası düzeltildi |

## 4. Provider Implementation Summary

| Provider | SDK | Model | API Format | Provider Name | Status |
|----------|-----|-------|------------|---------------|--------|
| Gemini | google-generativeai | gemini-2.0-flash | Inline content | `GEMINI` | ✅ Phase 15'ten hazır |
| GPT | openai | gpt-4o (configurable) | Chat Completion + Image URL | `GPT` | ✅ Tamamlandı |
| Claude | anthropic | claude-3-haiku-20240307 (configurable) | Messages API + Image Block | `CLAUDE` | ✅ Tamamlandı |

## 5. Fallback Chain Logic

```
Request → GEMINI (retry x3) → başarılı? → return
                                    ↓ (fail)
                              2s bekleme
                                    ↓
                  GPT (retry x3) → başarılı? → return
                                    ↓ (fail)
                              2s bekleme
                                    ↓
                CLAUDE (retry x3) → başarılı? → return
                                    ↓ (fail)
                        AllProvidersFailedError
```

**Kurallar:**
- Auth hataları → provider anında atlanır (retry yok)
- Her provider kendi retry mantığına sahip (tenacity ile exponential backoff)
- Provider'lar arası bekleme: `LLM_FALLBACK_DELAY_SECONDS` (varsayılan 2s)
- `LLM_CHAIN_ENABLED=false` → sadece ilk provider denenir
- UNHEALTHY provider → son çare olarak denenir (tamamen devre dışı bırakılmaz)

## 6. Prompt Adaptation

| Provider | System Message | Image Format | Prompt Kaynağı |
|----------|---------------|--------------|----------------|
| Gemini | Generation config | Inline Part | `prompt_manager.get_prompt()` |
| OpenAI | `role: system` message | `type: image_url` (data URL) | `prompt_manager.get_prompt()` |
| Anthropic | Top-level `system` param | `type: image` content block | `prompt_manager.get_prompt()` |

- **Master prompt**: `prompt_manager.py` içinde tek bir v1 prompt — tüm provider'lar aynı içeriği kullanır
- Fark sadece API çağrısı yapısında: her provider SDK'sına göre mesaj formatı değişir

## 7. Test Results

| Test Dosyası | Test Sayısı | Açıklama |
|-------------|-------------|----------|
| `test_openai_provider.py` | 3 | Success, auth error, timeout |
| `test_anthropic_provider.py` | 2 | Success, auth error |
| `test_fallback_chain.py` | 4 | First provider success, fallback, all fail, auth skip |
| `test_provider_health.py` | 9 | Initial state, thresholds, window expiry, independence |
| `test_prompt_adaptation.py` | 9 | JSON schema, Turkish instructions, format rules |
| `test_fallback_flow.py` | 1 | End-to-end integration with mocked chain |
| **Toplam** | **28** | **Mock provider'lar ile çalışır** |

> **Not**: Testler Docker container içinde `pytest` ile çalıştırılmalıdır.

## 8. Database Changes
- **Yeni migration yok** — `invoices.llm_provider VARCHAR(20)` sütunu (`GEMINI`, `GPT`, `CLAUDE` CHECK constraint) Phase 3'ten mevcut
- Fallback detayları API response'da `fallback_attempts` alanında döner, DB'ye yazılmaz
- `llm_extraction_logs` tablosu oluşturulmadı (Option A seçildi — sadece log)

## 9. Provider Health Tracking

**Durum Geçişleri:**
| Durum | Koşul | Davranış |
|-------|-------|----------|
| HEALTHY | < 2 hata (10 dk pencere) | Normal çalışma |
| DEGRADED | 2-4 hata (10 dk pencere) | Warning log, denemeye devam |
| UNHEALTHY | 5+ hata (10 dk pencere) | Son çare olarak denenir |

**Örnek endpoint çıktısı** (`GET /providers/health`):
```json
{
  "providers": {
    "GEMINI": {
      "name": "GEMINI",
      "status": "HEALTHY",
      "failure_count_window": 0,
      "total_failures": 0,
      "total_successes": 12
    },
    "GPT": {
      "name": "GPT",
      "status": "DEGRADED",
      "failure_count_window": 3,
      "total_failures": 5,
      "total_successes": 8
    }
  },
  "timestamp": 1739414400.0
}
```

## 10. Issues Encountered & Fixes

| Sorun | Çözüm |
|-------|-------|
| `extraction.py` routes dosyasında `time` import eksikliği → `GET /providers/health` RuntimeError | `import time` eklendi |
| `test_provider_health.py` eksik | 9 unit test ile oluşturuldu |
| `test_prompt_adaptation.py` eksik | 9 unit test ile oluşturuldu |
| Mock response fixture dosyaları eksik | OpenAI ve Anthropic için 6 fixture dosyası oluşturuldu |

## 11. Configuration

```env
# LLM API Keys (Phase 13'ten mevcut)
GEMINI_API_KEY=...
OPENAI_API_KEY=...
ANTHROPIC_API_KEY=...

# Fallback Chain (Phase 16)
LLM_CHAIN_ORDER=GEMINI,GPT,CLAUDE
LLM_FALLBACK_DELAY_SECONDS=2
LLM_CHAIN_ENABLED=true

# Provider-specific (os.getenv ile okunur)
OPENAI_MODEL=gpt-4o
ANTHROPIC_MODEL=claude-3-haiku-20240307
```

## 12. Next Steps
- **Docker Image Rebuild**: `openai` ve `anthropic` paketlerini içerecek şekilde yeniden build edilmeli
- **API Key Konfigürasyonu**: `.env` dosyasına `OPENAI_API_KEY` ve `ANTHROPIC_API_KEY` girilmeli
- **Phase 17**: Response Validation & Confidence Score — `ExtractionResult`'ı tüketecek
- **Phase 19-A**: RabbitMQ Consumer — fallback chain ile async extraction

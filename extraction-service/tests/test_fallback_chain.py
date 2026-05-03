"""
Unit tests for FallbackChain — Phase 16.
Tests the sequential cascade: Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano.
All providers are mocked.
"""
import pytest
import asyncio
import time
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.base_provider import (
    LLMError,
    LLMTimeoutError,
    LLMRateLimitError,
    LLMServerError,
    LLMAuthenticationError,
    LLMResponseError,
)
from app.services.llm.provider_health import ProviderHealthManager


@pytest.fixture(autouse=True)
def reset_health_singleton():
    """Reset ProviderHealthManager singleton before each test."""
    ProviderHealthManager._instance = None
    yield
    ProviderHealthManager._instance = None


def _make_mock_provider(name, is_async=True):
    """Create a mock provider with the given name."""
    mock = MagicMock()
    mock.provider_name = name
    mock.is_available.return_value = True
    if is_async:
        mock.generate = AsyncMock(return_value=('{"test": true}', {"input_tokens": 100, "output_tokens": 50}))
    return mock


@pytest.fixture
def mock_chain():
    """Create a FallbackChain with mocked providers."""
    with patch("app.services.llm.fallback_chain.GeminiProvider") as MockGemini, \
         patch("app.services.llm.fallback_chain.Gemini25FlashProvider") as MockGemini25, \
         patch("app.services.llm.fallback_chain.OpenAIProvider") as MockOpenAI:

        gemini = _make_mock_provider("GEMINI_3_FLASH")
        gemini25 = _make_mock_provider("GEMINI_2_5_FLASH")
        openai = _make_mock_provider("GPT5_NANO")

        MockGemini.return_value = gemini
        MockGemini25.return_value = gemini25
        MockOpenAI.return_value = openai

        chain = FallbackChain()
        chain.chain_order = ["GEMINI_3_FLASH", "GEMINI_2_5_FLASH", "GPT5_NANO"]
        chain.providers = {
            "GEMINI_3_FLASH": gemini,
            "GEMINI_2_5_FLASH": gemini25,
            "GPT5_NANO": openai,
        }

        yield chain, {"GEMINI_3_FLASH": gemini, "GEMINI_2_5_FLASH": gemini25, "GPT5_NANO": openai}


class TestFallbackChain:

    # ─── Happy Path ─────────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_primary_succeeds(self, mock_chain):
        """If Gemini 3 Flash succeeds, no fallback needed."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(
            return_value=('{"result": "gemini3"}', {"input_tokens": 100, "output_tokens": 50})
        )

        response, provider_name, logs, usage = await chain.generate_with_fallback(b"img", "prompt")

        assert response == '{"result": "gemini3"}'
        assert provider_name == "GEMINI_3_FLASH"
        assert len(logs) == 1
        assert logs[0]["status"] == "SUCCESS"
        providers["GEMINI_2_5_FLASH"].generate.assert_not_called()
        providers["GPT5_NANO"].generate.assert_not_called()

    # ─── Single Fallback ────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_single_fallback_to_gemini_25(self, mock_chain):
        """Gemini 3 Flash fails → Gemini 2.5 Flash succeeds."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMTimeoutError("Timeout"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            return_value=('{"result": "gemini25"}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response, provider_name, logs, usage = await chain.generate_with_fallback(b"img", "prompt")

        assert response == '{"result": "gemini25"}'
        assert provider_name == "GEMINI_2_5_FLASH"
        assert len(logs) == 2
        assert logs[0]["status"] == "FAILED"
        assert logs[0]["error"] == "LLMTimeoutError"
        assert logs[1]["status"] == "SUCCESS"
        providers["GPT5_NANO"].generate.assert_not_called()

    # ─── Double Fallback ────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_double_fallback_to_gpt5_nano(self, mock_chain):
        """Both Gemini providers fail → GPT-5 nano succeeds."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMServerError("Server Error"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(side_effect=LLMRateLimitError("Rate Limit"))
        providers["GPT5_NANO"].generate = AsyncMock(
            return_value=('{"result": "gpt5"}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response, provider_name, logs, usage = await chain.generate_with_fallback(b"img", "prompt")

        assert response == '{"result": "gpt5"}'
        assert provider_name == "GPT5_NANO"
        assert len(logs) == 3
        assert logs[0]["status"] == "FAILED"
        assert logs[1]["status"] == "FAILED"
        assert logs[2]["status"] == "SUCCESS"

    # ─── All Fail ───────────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_all_providers_fail(self, mock_chain):
        """All providers fail → AllProvidersFailedError with attempt details."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("Error 1"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(side_effect=LLMError("Error 2"))
        providers["GPT5_NANO"].generate = AsyncMock(side_effect=LLMError("Error 3"))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            with pytest.raises(AllProvidersFailedError) as exc_info:
                await chain.generate_with_fallback(b"img", "prompt")

        assert exc_info.value.attempts is not None
        assert len(exc_info.value.attempts) == 3
        assert all(a["status"] == "FAILED" for a in exc_info.value.attempts)

    # ─── Auth Error Skip ────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_auth_error_skips_to_next(self, mock_chain):
        """Auth errors cause immediate skip without retries."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(
            side_effect=LLMAuthenticationError("Bad API key")
        )
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            return_value=('{"result": "success"}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response, provider_name, logs, usage = await chain.generate_with_fallback(b"img", "prompt")

        assert provider_name == "GEMINI_2_5_FLASH"
        assert logs[0]["error"] == "LLMAuthenticationError"

    # ─── Parse Error Triggers Fallback ──────────────────────────────────────

    @pytest.mark.asyncio
    async def test_response_error_triggers_fallback(self, mock_chain):
        """LLMResponseError (parse failure) triggers fallback to next provider."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(
            side_effect=LLMResponseError("Invalid JSON")
        )
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            return_value=('{"valid": true}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response, provider_name, logs, usage = await chain.generate_with_fallback(b"img", "prompt")

        assert provider_name == "GEMINI_2_5_FLASH"
        assert logs[0]["error"] == "LLMResponseError"

    # ─── Same Image Passed ──────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_same_image_passed_to_all_providers(self, mock_chain):
        """All providers must receive the exact same image bytes."""
        chain, providers = mock_chain
        test_image = b"test_image_bytes_content"
        test_prompt = "test_prompt"

        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GPT5_NANO"].generate = AsyncMock(side_effect=LLMError("fail"))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            with pytest.raises(AllProvidersFailedError):
                await chain.generate_with_fallback(test_image, test_prompt)

        # All received the same image
        providers["GEMINI_3_FLASH"].generate.assert_called_with(test_image, test_prompt)
        providers["GEMINI_2_5_FLASH"].generate.assert_called_with(test_image, test_prompt)
        providers["GPT5_NANO"].generate.assert_called_with(test_image, test_prompt)

    # ─── Delay Between Providers ────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_delay_between_providers(self, mock_chain):
        """2-second delay should be applied between fallback attempts."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            return_value=('{"ok": true}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
            await chain.generate_with_fallback(b"img", "prompt")

        # Sleep should have been called with the fallback delay
        mock_sleep.assert_called_once()
        delay_arg = mock_sleep.call_args[0][0]
        assert delay_arg == 2  # Default LLM_FALLBACK_DELAY_SECONDS

    # ─── Attempt Tracking ───────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_attempt_logs_contain_required_fields(self, mock_chain):
        """Each attempt log should contain provider, status, duration_ms."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMTimeoutError("timeout"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            return_value=('{"ok": true}', {"input_tokens": 100, "output_tokens": 50})
        )

        with patch("asyncio.sleep", new_callable=AsyncMock):
            _, _, logs, _ = await chain.generate_with_fallback(b"img", "prompt")

        for log in logs:
            assert "provider" in log
            assert "status" in log
            assert "duration_ms" in log

    # ─── Chain Disabled ─────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_chain_disabled_uses_only_primary(self, mock_chain):
        """When chain is disabled, only the primary provider is tried."""
        chain, providers = mock_chain
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))

        with patch("app.services.llm.fallback_chain.settings") as mock_settings:
            mock_settings.LLM_CHAIN_ENABLED = False
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 2
            with pytest.raises(AllProvidersFailedError) as exc_info:
                await chain.generate_with_fallback(b"img", "prompt")

        # Only one attempt should have been made
        assert len(exc_info.value.attempts) == 1
        providers["GEMINI_2_5_FLASH"].generate.assert_not_called()
        providers["GPT5_NANO"].generate.assert_not_called()

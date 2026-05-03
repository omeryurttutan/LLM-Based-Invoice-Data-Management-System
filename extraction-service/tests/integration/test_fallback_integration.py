"""
Integration tests for LLM Fallback Chain — Phase 16.
Tests the full pipeline with mocked LLMs.
"""
import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.base_provider import (
    LLMError,
    LLMTimeoutError,
    LLMAuthenticationError,
)
from app.services.llm.provider_health import ProviderHealthManager


@pytest.fixture(autouse=True)
def reset_singleton():
    ProviderHealthManager._instance = None
    yield
    ProviderHealthManager._instance = None


VALID_INVOICE_JSON = '''{
    "invoice_number": "FTR2024000123",
    "invoice_date": "2024-01-15",
    "due_date": null,
    "supplier_name": "Test Tedarikçi A.Ş.",
    "supplier_tax_number": "1234567890",
    "supplier_address": "İstanbul, Türkiye",
    "buyer_name": "Test Alıcı Ltd.",
    "buyer_tax_number": "0987654321",
    "items": [
        {
            "description": "Test Ürün",
            "quantity": 2,
            "unit": "Adet",
            "unit_price": 100.00,
            "tax_rate": 20,
            "tax_amount": 40.00,
            "line_total": 240.00
        }
    ],
    "subtotal": 200.00,
    "tax_amount": 40.00,
    "total_amount": 240.00,
    "currency": "TRY",
    "notes": null
}'''


class TestFallbackIntegration:
    """Integration tests verifying the full fallback flow with mocked providers."""

    def _create_chain_with_mocks(self):
        """Create a FallbackChain with mock providers."""
        with patch("app.services.llm.fallback_chain.GeminiProvider") as MockGemini, \
             patch("app.services.llm.fallback_chain.Gemini25FlashProvider") as MockGemini25, \
             patch("app.services.llm.fallback_chain.OpenAIProvider") as MockOpenAI:

            gemini = MagicMock()
            gemini.provider_name = "GEMINI_3_FLASH"
            gemini.is_available.return_value = True

            gemini25 = MagicMock()
            gemini25.provider_name = "GEMINI_2_5_FLASH"
            gemini25.is_available.return_value = True

            openai = MagicMock()
            openai.provider_name = "GPT5_NANO"
            openai.is_available.return_value = True

            MockGemini.return_value = gemini
            MockGemini25.return_value = gemini25
            MockOpenAI.return_value = openai

            chain = FallbackChain()
            return chain, {"GEMINI_3_FLASH": gemini, "GEMINI_2_5_FLASH": gemini25, "GPT5_NANO": openai}

    @pytest.mark.asyncio
    async def test_full_pipeline_primary_success(self):
        """End-to-end: primary provider returns valid invoice JSON."""
        chain, providers = self._create_chain_with_mocks()
        usage = {"input_tokens": 500, "output_tokens": 200}
        providers["GEMINI_3_FLASH"].generate = AsyncMock(return_value=(VALID_INVOICE_JSON, usage))

        response_text, provider_name, logs, resp_usage = await chain.generate_with_fallback(
            b"test_image", "extract invoice"
        )

        assert provider_name == "GEMINI_3_FLASH"
        assert "FTR2024000123" in response_text
        assert resp_usage["input_tokens"] == 500
        assert len(logs) == 1
        assert logs[0]["status"] == "SUCCESS"

    @pytest.mark.asyncio
    async def test_full_pipeline_fallback_to_gpt5(self):
        """End-to-end: both Gemini providers fail, GPT-5 nano succeeds."""
        chain, providers = self._create_chain_with_mocks()
        usage = {"input_tokens": 600, "output_tokens": 250}

        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMTimeoutError("Google timeout"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(side_effect=LLMTimeoutError("Google timeout"))
        providers["GPT5_NANO"].generate = AsyncMock(return_value=(VALID_INVOICE_JSON, usage))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response_text, provider_name, logs, resp_usage = await chain.generate_with_fallback(
                b"test_image", "extract invoice"
            )

        assert provider_name == "GPT5_NANO"
        assert resp_usage["input_tokens"] == 600
        assert len(logs) == 3
        assert logs[0]["status"] == "FAILED"
        assert logs[1]["status"] == "FAILED"
        assert logs[2]["status"] == "SUCCESS"

    @pytest.mark.asyncio
    async def test_health_tracking_across_requests(self):
        """Health manager correctly tracks success/failure across multiple requests."""
        chain, providers = self._create_chain_with_mocks()
        usage = {"input_tokens": 100, "output_tokens": 50}

        # First request: primary succeeds
        providers["GEMINI_3_FLASH"].generate = AsyncMock(return_value=('{"ok": true}', usage))
        await chain.generate_with_fallback(b"img", "prompt")

        # Check health
        health = chain.health_manager.get_health("GEMINI_3_FLASH")
        assert health.total_successes == 1

        # Second request: primary fails, fallback succeeds
        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(return_value=('{"ok": true}', usage))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            await chain.generate_with_fallback(b"img", "prompt")

        health = chain.health_manager.get_health("GEMINI_3_FLASH")
        assert health.total_failures == 1

    @pytest.mark.asyncio
    async def test_auth_error_on_gemini_skips_both_gemini(self):
        """
        If Gemini 3 Flash has auth error (bad key), Gemini 2.5 Flash uses the same key
        and will also likely fail with auth — each gets tried independently.
        """
        chain, providers = self._create_chain_with_mocks()
        usage = {"input_tokens": 100, "output_tokens": 50}

        providers["GEMINI_3_FLASH"].generate = AsyncMock(
            side_effect=LLMAuthenticationError("Bad Gemini key")
        )
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(
            side_effect=LLMAuthenticationError("Bad Gemini key")
        )
        providers["GPT5_NANO"].generate = AsyncMock(return_value=('{"ok": true}', usage))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            response_text, provider_name, logs, _ = await chain.generate_with_fallback(b"img", "prompt")

        assert provider_name == "GPT5_NANO"
        assert logs[0]["error"] == "LLMAuthenticationError"
        assert logs[1]["error"] == "LLMAuthenticationError"
        assert logs[2]["status"] == "SUCCESS"

    @pytest.mark.asyncio
    async def test_consistent_image_across_all_providers(self):
        """Same preprocessed image must be passed to every provider in the chain."""
        chain, providers = self._create_chain_with_mocks()
        test_image = b"consistent_image_bytes_for_all"

        providers["GEMINI_3_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GEMINI_2_5_FLASH"].generate = AsyncMock(side_effect=LLMError("fail"))
        providers["GPT5_NANO"].generate = AsyncMock(side_effect=LLMError("fail"))

        with patch("asyncio.sleep", new_callable=AsyncMock):
            with pytest.raises(AllProvidersFailedError):
                await chain.generate_with_fallback(test_image, "prompt")

        # Verify all received the same image
        for name, prov in providers.items():
            call_args = prov.generate.call_args
            assert call_args[0][0] == test_image, f"{name} received different image"

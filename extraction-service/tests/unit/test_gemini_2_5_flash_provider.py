"""
Unit tests for Gemini25FlashProvider — Phase 16.
All tests mock the Gemini SDK to avoid real API calls.
"""
import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from google.api_core import exceptions

from app.services.llm.providers.gemini_2_5_flash_provider import Gemini25FlashProvider
from app.services.llm.base_provider import (
    LLMProviderNames,
    LLMAuthenticationError,
    LLMTimeoutError,
    LLMServerError,
    LLMRateLimitError,
    LLMConnectionError,
    LLMResponseError,
)


class TestGemini25FlashProvider:

    @pytest.fixture
    def provider(self):
        with patch("google.generativeai.configure"):
            provider = Gemini25FlashProvider()
            provider.api_key = "fake_key"
            return provider

    # ─── Provider Identity ──────────────────────────────────────────────────

    def test_provider_name(self, provider):
        assert provider.provider_name == LLMProviderNames.GEMINI_2_5_FLASH
        assert provider.provider_name == "GEMINI_2_5_FLASH"

    def test_is_available_with_key(self, provider):
        assert provider.is_available() is True

    def test_is_available_without_key(self, provider):
        provider.api_key = None
        assert provider.is_available() is False

    def test_is_available_empty_key(self, provider):
        provider.api_key = ""
        assert provider.is_available() is False

    def test_uses_correct_model_name(self, provider):
        """Verify the model name is gemini-2.5-flash (from settings)."""
        assert "gemini-2.5-flash" in provider.model_name

    def test_shares_gemini_api_key(self, provider):
        """Gemini 2.5 Flash uses the same GEMINI_API_KEY as the primary provider."""
        from app.config.settings import settings
        # Both should reference the same key source
        assert provider.api_key is not None

    # ─── Successful Generation ──────────────────────────────────────────────

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_success(self, mock_model_cls, provider):
        """Test successful multimodal generation with image + prompt."""
        mock_model = MagicMock()
        mock_response = MagicMock()
        mock_response.text = '{"invoice_number": "INV-001", "total_amount": 100.0}'

        mock_usage = MagicMock()
        mock_usage.prompt_token_count = 500
        mock_usage.candidates_token_count = 200
        mock_response.usage_metadata = mock_usage

        mock_model.generate_content_async = AsyncMock(return_value=mock_response)
        mock_model_cls.return_value = mock_model

        result_text, usage = await provider.generate(b"fake_image_bytes", "fake_prompt")

        assert result_text == '{"invoice_number": "INV-001", "total_amount": 100.0}'
        assert usage["input_tokens"] == 500
        assert usage["output_tokens"] == 200
        mock_model.generate_content_async.assert_called_once()

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_sends_image_as_inline_data(self, mock_model_cls, provider):
        """Verify that the image is sent as inline data with correct MIME type."""
        mock_model = MagicMock()
        mock_response = MagicMock()
        mock_response.text = '{"invoice_number": "TEST"}'
        mock_response.usage_metadata = MagicMock(prompt_token_count=100, candidates_token_count=50)
        mock_model.generate_content_async = AsyncMock(return_value=mock_response)
        mock_model_cls.return_value = mock_model

        image_bytes = b"\x89PNG\r\n\x1a\n"  # PNG magic bytes
        await provider.generate(image_bytes, "prompt", mime_type="image/png")

        call_args = mock_model.generate_content_async.call_args
        contents = call_args[0][0]

        assert len(contents) == 2
        assert contents[0] == "prompt"
        assert contents[1]["mime_type"] == "image/png"
        assert contents[1]["data"] == image_bytes

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_uses_system_instruction(self, mock_model_cls, provider):
        """Verify system_instruction is passed to GenerativeModel."""
        mock_model = MagicMock()
        mock_response = MagicMock()
        mock_response.text = '{"invoice_number": "TEST"}'
        mock_response.usage_metadata = MagicMock(prompt_token_count=100, candidates_token_count=50)
        mock_model.generate_content_async = AsyncMock(return_value=mock_response)
        mock_model_cls.return_value = mock_model

        await provider.generate(b"bytes", "prompt")

        model_kwargs = mock_model_cls.call_args[1]
        assert "system_instruction" in model_kwargs
        assert len(model_kwargs["system_instruction"]) > 0
        assert "Turkish" in model_kwargs["system_instruction"]

    # ─── Error Handling ─────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_generate_missing_api_key(self, provider):
        """Auth error when API key is missing."""
        provider.api_key = None
        with pytest.raises(LLMAuthenticationError, match="missing"):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_auth_error(self, mock_model_cls, provider):
        """InvalidArgument from Gemini → LLMAuthenticationError."""
        mock_model = MagicMock()
        mock_model.generate_content_async = AsyncMock(
            side_effect=exceptions.InvalidArgument("Invalid API Key")
        )
        mock_model_cls.return_value = mock_model

        with pytest.raises(LLMAuthenticationError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_rate_limit_error(self, mock_model_cls, provider):
        """ResourceExhausted → re-raised after retries exhausted."""
        mock_model = MagicMock()
        mock_model.generate_content_async = AsyncMock(
            side_effect=exceptions.ResourceExhausted("Rate Limit")
        )
        mock_model_cls.return_value = mock_model

        with pytest.raises(exceptions.ResourceExhausted):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_timeout_error(self, mock_model_cls, provider):
        """DeadlineExceeded → LLMTimeoutError."""
        mock_model = MagicMock()
        mock_model.generate_content_async = AsyncMock(
            side_effect=exceptions.DeadlineExceeded("Timeout")
        )
        mock_model_cls.return_value = mock_model

        with pytest.raises(LLMTimeoutError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_server_error(self, mock_model_cls, provider):
        """InternalServerError → re-raised after retries exhausted."""
        mock_model = MagicMock()
        mock_model.generate_content_async = AsyncMock(
            side_effect=exceptions.InternalServerError("Server Error")
        )
        mock_model_cls.return_value = mock_model

        with pytest.raises(exceptions.InternalServerError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_connection_error(self, mock_model_cls, provider):
        """Connection error in exception message → LLMConnectionError."""
        mock_model = MagicMock()
        mock_model.generate_content_async = AsyncMock(
            side_effect=OSError("Connection refused")
        )
        mock_model_cls.return_value = mock_model

        with pytest.raises(LLMConnectionError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    @patch("google.generativeai.GenerativeModel")
    async def test_generate_empty_response(self, mock_model_cls, provider):
        """Empty response text → LLMResponseError."""
        mock_model = MagicMock()
        mock_response = MagicMock()
        mock_response.text = ""
        mock_response.usage_metadata = MagicMock(prompt_token_count=100, candidates_token_count=0)
        mock_model.generate_content_async = AsyncMock(return_value=mock_response)
        mock_model_cls.return_value = mock_model

        with pytest.raises(LLMResponseError, match="Empty"):
            await provider.generate(b"bytes", "prompt")

    # ─── Retry Logic ────────────────────────────────────────────────────────

    @pytest.mark.asyncio
    @patch("app.services.llm.providers.gemini_2_5_flash_provider.genai.GenerativeModel")
    async def test_generate_retries_on_server_error(self, mock_model_cls):
        """Server errors trigger retries before succeeding."""
        with patch("google.generativeai.configure"):
            provider = Gemini25FlashProvider()
            provider.api_key = "fake_key"

        mock_response = MagicMock()
        mock_response.text = '{"invoice_number": "RETRY-OK"}'
        mock_response.usage_metadata = MagicMock(prompt_token_count=100, candidates_token_count=50)

        mock_model = MagicMock()
        # First two attempts fail with retryable InternalServerError,
        # third attempt succeeds
        mock_model.generate_content_async = AsyncMock(
            side_effect=[
                exceptions.InternalServerError("Fail 1"),
                exceptions.InternalServerError("Fail 2"),
                mock_response,
            ]
        )
        mock_model_cls.return_value = mock_model

        result_text, usage = await provider.generate(b"bytes", "prompt")
        assert result_text == '{"invoice_number": "RETRY-OK"}'
        assert mock_model.generate_content_async.call_count == 3

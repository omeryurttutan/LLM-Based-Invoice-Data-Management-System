"""
Unit tests for OpenAIProvider (GPT-5 nano) — Phase 16.
All tests mock the OpenAI SDK to avoid real API calls.
"""
import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from openai import APIError, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError

from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.base_provider import (
    LLMProviderNames,
    LLMAuthenticationError,
    LLMTimeoutError,
    LLMServerError,
    LLMRateLimitError,
    LLMConnectionError,
    LLMResponseError,
)


class TestOpenAIProvider:

    @pytest.fixture
    def provider(self):
        with patch("app.services.llm.providers.openai_provider.AsyncOpenAI"):
            provider = OpenAIProvider()
            provider.api_key = "fake_key"
            provider.client = AsyncMock()
            return provider

    # ─── Provider Identity ──────────────────────────────────────────────────

    def test_provider_name(self, provider):
        """Provider name should be GPT5_NANO."""
        assert provider.provider_name == LLMProviderNames.GPT5_NANO
        assert provider.provider_name == "GPT5_NANO"

    def test_is_available_with_key(self, provider):
        assert provider.is_available() is True

    def test_is_available_without_key(self, provider):
        provider.api_key = None
        assert provider.is_available() is False

    def test_is_available_empty_key(self, provider):
        provider.api_key = ""
        assert provider.is_available() is False

    def test_uses_correct_model_name(self, provider):
        """Should use gpt-5-nano model from settings."""
        assert provider.model_name == "gpt-5-nano"

    # ─── Successful Generation ──────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_generate_success(self, provider):
        """Test successful multimodal generation."""
        mock_response = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = '{"invoice_number": "INV-001", "total_amount": 100.0}'
        mock_response.choices = [mock_choice]
        mock_response.usage.prompt_tokens = 500
        mock_response.usage.completion_tokens = 200

        provider.client.chat.completions.create = AsyncMock(return_value=mock_response)

        result_text, usage = await provider.generate(b"fake_image_bytes", "fake_prompt")

        assert result_text == '{"invoice_number": "INV-001", "total_amount": 100.0}'
        assert usage["input_tokens"] == 500
        assert usage["output_tokens"] == 200
        provider.client.chat.completions.create.assert_called_once()

    @pytest.mark.asyncio
    async def test_generate_sends_image_as_data_url(self, provider):
        """Verify image is sent as base64 data URL in user message."""
        mock_response = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = '{"invoice_number": "TEST"}'
        mock_response.choices = [mock_choice]
        mock_response.usage.prompt_tokens = 100
        mock_response.usage.completion_tokens = 50

        provider.client.chat.completions.create = AsyncMock(return_value=mock_response)

        await provider.generate(b"fake_image", "prompt", mime_type="image/png")

        call_kwargs = provider.client.chat.completions.create.call_args.kwargs
        messages = call_kwargs["messages"]
        # System message
        assert messages[0]["role"] == "system"
        assert "Turkish" in messages[0]["content"]
        # User message with image
        assert messages[1]["role"] == "user"
        content = messages[1]["content"]
        assert content[0]["type"] == "text"
        assert content[1]["type"] == "image_url"
        assert content[1]["image_url"]["url"].startswith("data:image/png;base64,")

    @pytest.mark.asyncio
    async def test_generate_uses_prompt_manager_system_instruction(self, provider):
        """System instruction should come from PromptManager, not hardcoded."""
        mock_response = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = '{"invoice_number": "TEST"}'
        mock_response.choices = [mock_choice]
        mock_response.usage.prompt_tokens = 100
        mock_response.usage.completion_tokens = 50

        provider.client.chat.completions.create = AsyncMock(return_value=mock_response)

        await provider.generate(b"bytes", "prompt")

        call_kwargs = provider.client.chat.completions.create.call_args.kwargs
        system_msg = call_kwargs["messages"][0]
        assert system_msg["role"] == "system"
        # Should contain Turkish invoice-related content from PromptManager
        assert "Turkish" in system_msg["content"]
        assert "invoice" in system_msg["content"].lower()

    @pytest.mark.asyncio
    async def test_generate_passes_mime_type_correctly(self, provider):
        """Different MIME types should be reflected in the data URL."""
        mock_response = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = '{"test": true}'
        mock_response.choices = [mock_choice]
        mock_response.usage.prompt_tokens = 100
        mock_response.usage.completion_tokens = 50

        provider.client.chat.completions.create = AsyncMock(return_value=mock_response)

        # Test with JPEG
        await provider.generate(b"bytes", "prompt", mime_type="image/jpeg")
        call_kwargs = provider.client.chat.completions.create.call_args.kwargs
        url = call_kwargs["messages"][1]["content"][1]["image_url"]["url"]
        assert url.startswith("data:image/jpeg;base64,")

    # ─── Error Handling ─────────────────────────────────────────────────────

    @pytest.mark.asyncio
    async def test_generate_missing_api_key(self, provider):
        """Auth error when API key is missing."""
        provider.api_key = None
        with pytest.raises(LLMAuthenticationError, match="missing"):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_auth_error(self, provider):
        """AuthenticationError → LLMAuthenticationError."""
        provider.client.chat.completions.create = AsyncMock(
            side_effect=AuthenticationError("Auth failed", response=MagicMock(), body={})
        )
        with pytest.raises(LLMAuthenticationError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_rate_limit_error(self, provider):
        """RateLimitError → LLMRateLimitError."""
        provider.client.chat.completions.create = AsyncMock(
            side_effect=RateLimitError("Rate limit exceeded", response=MagicMock(), body={})
        )
        with pytest.raises(LLMRateLimitError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_timeout_error(self, provider):
        """APITimeoutError → LLMTimeoutError."""
        provider.client.chat.completions.create = AsyncMock(
            side_effect=APITimeoutError(request=MagicMock())
        )
        with pytest.raises(LLMTimeoutError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_connection_error(self, provider):
        """APIConnectionError → LLMConnectionError."""
        provider.client.chat.completions.create = AsyncMock(
            side_effect=APIConnectionError(request=MagicMock())
        )
        with pytest.raises(LLMConnectionError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_server_error(self, provider):
        """APIError → LLMServerError."""
        provider.client.chat.completions.create = AsyncMock(
            side_effect=APIError(message="Internal Server Error", request=MagicMock(), body={})
        )
        with pytest.raises(LLMServerError):
            await provider.generate(b"bytes", "prompt")

    @pytest.mark.asyncio
    async def test_generate_empty_response(self, provider):
        """Empty response → LLMResponseError."""
        mock_response = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = ""
        mock_response.choices = [mock_choice]
        mock_response.usage.prompt_tokens = 100
        mock_response.usage.completion_tokens = 0

        provider.client.chat.completions.create = AsyncMock(return_value=mock_response)

        with pytest.raises(LLMResponseError, match="Empty"):
            await provider.generate(b"bytes", "prompt")

import pytest
from unittest.mock import MagicMock, patch
from openai import APIError, AuthenticationError, APITimeoutError, RateLimitError, APIConnectionError
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.base_provider import (
    LLMAuthenticationError, LLMTimeoutError, LLMRateLimitError,
    LLMServerError, LLMConnectionError
)


class TestOpenAIProvider:
    
    @pytest.fixture
    def provider(self):
        with patch("app.services.llm.providers.openai_provider.OpenAI"):
            provider = OpenAIProvider()
            provider.api_key = "fake_key"
            provider.client = MagicMock()
            return provider

    def test_generate_success(self, provider):
        # Mock response
        mock_response = MagicMock()
        mock_response.choices[0].message.content = '{"invoice_number": "GPT123"}'
        provider.client.chat.completions.create.return_value = mock_response
        
        # Execute
        result = provider.generate(b"fake_bytes", "prompt")
        
        # Verify
        assert result == '{"invoice_number": "GPT123"}'
        provider.client.chat.completions.create.assert_called_once()
        
        # Verify image format in call args
        call_args = provider.client.chat.completions.create.call_args
        messages = call_args.kwargs['messages']
        user_content = messages[1]['content']
        assert user_content[1]['type'] == 'image_url'
        assert 'data:image/jpeg;base64,' in user_content[1]['image_url']['url']

    def test_generate_auth_error(self, provider):
        provider.client.chat.completions.create.side_effect = AuthenticationError("Bad Key", response=MagicMock(), body={})
        
        with pytest.raises(LLMAuthenticationError):
            provider.generate(b"bytes", "prompt")

    def test_generate_timeout_error(self, provider):
        provider.client.chat.completions.create.side_effect = APITimeoutError("Timeout")
        
        with pytest.raises(LLMTimeoutError):
            provider.generate(b"bytes", "prompt")

    def test_rate_limit_handling(self, provider):
        """Rate limit error should raise LLMRateLimitError."""
        provider.client.chat.completions.create.side_effect = RateLimitError(
            "Rate limit exceeded", response=MagicMock(), body={}
        )
        
        with pytest.raises(LLMRateLimitError):
            provider.generate(b"bytes", "prompt")

    def test_server_error_handling(self, provider):
        """5xx server error should raise LLMServerError."""
        mock_request = MagicMock()
        error = APIError("Internal Server Error", mock_request, body=None)
        provider.client.chat.completions.create.side_effect = error
        
        with pytest.raises(LLMServerError):
            provider.generate(b"bytes", "prompt")

    def test_connection_error_handling(self, provider):
        """Connection error should raise LLMConnectionError."""
        provider.client.chat.completions.create.side_effect = APIConnectionError(request=MagicMock())
        
        with pytest.raises(LLMConnectionError):
            provider.generate(b"bytes", "prompt")

    def test_provider_name_returns_gpt(self, provider):
        """Provider name must return 'GPT' for the database column."""
        assert provider.provider_name == "GPT"

    def test_is_available_with_key(self, provider):
        """Provider with API key should be available."""
        provider.api_key = "valid_key"
        assert provider.is_available() is True

    def test_is_available_without_key(self, provider):
        """Provider without API key should not be available."""
        provider.api_key = None
        assert provider.is_available() is False

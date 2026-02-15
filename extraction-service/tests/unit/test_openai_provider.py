import pytest
from unittest.mock import MagicMock, patch
from openai import APIError, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.base_provider import (
    LLMAuthenticationError, 
    LLMTimeoutError, 
    LLMServerError, 
    LLMRateLimitError, 
    LLMConnectionError
)

class TestOpenAIProvider:
    
    @pytest.fixture
    def provider(self):
        with patch("app.services.llm.providers.openai_provider.OpenAI"):
            # Mock settings if needed
            provider = OpenAIProvider()
            provider.api_key = "fake_key"
            provider.client = MagicMock()
            return provider

    def test_generate_success(self, provider, mock_gpt_response):
        # Setup mock
        mock_response = MagicMock()
        mock_response.choices[0].message.content = mock_gpt_response
        provider.client.chat.completions.create.return_value = mock_response
        
        # Execute
        result = provider.generate(b"fake_image_bytes", "fake_prompt")
        
        # Verify
        assert result == mock_gpt_response
        provider.client.chat.completions.create.assert_called_once()
        # Verify image format in call args
        call_kwargs = provider.client.chat.completions.create.call_args.kwargs
        messages = call_kwargs['messages']
        assert messages[1]['role'] == 'user'
        content = messages[1]['content']
        assert content[1]['type'] == 'image_url'
        assert content[1]['image_url']['url'].startswith("data:image/jpeg;base64,")

    def test_generate_auth_error(self, provider):
        # Setup mock
        provider.client.chat.completions.create.side_effect = AuthenticationError("Auth failed", response=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMAuthenticationError):
            provider.generate(b"bytes", "prompt")

    def test_generate_rate_limit_error(self, provider):
        # Setup mock
        provider.client.chat.completions.create.side_effect = RateLimitError("Rate limit exceeded", response=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMRateLimitError):
            provider.generate(b"bytes", "prompt")

    def test_generate_timeout_error(self, provider):
        # Setup mock
        provider.client.chat.completions.create.side_effect = APITimeoutError("Request timed out")
        
        # Execute & Verify
        with pytest.raises(LLMTimeoutError):
            provider.generate(b"bytes", "prompt")

    def test_generate_connection_error(self, provider):
        # Setup mock
        provider.client.chat.completions.create.side_effect = APIConnectionError(message="Connection failed")
        
        # Execute & Verify
        with pytest.raises(LLMConnectionError):
            provider.generate(b"bytes", "prompt")

    def test_generate_server_error(self, provider):
         # Setup mock
        provider.client.chat.completions.create.side_effect = APIError(message="Internal Server Error", request=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMServerError):
            provider.generate(b"bytes", "prompt")

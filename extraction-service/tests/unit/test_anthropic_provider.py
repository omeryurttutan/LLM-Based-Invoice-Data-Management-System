import pytest
from unittest.mock import MagicMock, patch
from anthropic import APIError, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError
from app.services.llm.providers.anthropic_provider import AnthropicProvider
from app.services.llm.base_provider import (
    LLMAuthenticationError, 
    LLMTimeoutError, 
    LLMServerError, 
    LLMRateLimitError, 
    LLMConnectionError
)

class TestAnthropicProvider:
    
    @pytest.fixture
    def provider(self):
        with patch("app.services.llm.providers.anthropic_provider.Anthropic"):
            mock_client_cls = MagicMock()
            provider = AnthropicProvider()
            provider.api_key = "fake_key"
            provider.client = MagicMock()
            return provider

    def test_generate_success(self, provider, mock_claude_response):
        # Setup mock
        mock_msg = MagicMock()
        mock_msg.content = [MagicMock(text=mock_claude_response)]
        provider.client.messages.create.return_value = mock_msg
        
        # Execute
        result = provider.generate(b"fake_image_bytes", "fake_prompt")
        
        # Verify
        assert result == mock_claude_response
        provider.client.messages.create.assert_called_once()
        # Verify format
        call_kwargs = provider.client.messages.create.call_args.kwargs
        messages = call_kwargs['messages']
        content = messages[0]['content']
        assert content[0]['type'] == 'image'
        assert content[0]['source']['type'] == 'base64'

    def test_generate_auth_error(self, provider):
        # Setup mock
        provider.client.messages.create.side_effect = AuthenticationError("Auth failed", response=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMAuthenticationError):
            provider.generate(b"bytes", "prompt")

    def test_generate_rate_limit_error(self, provider):
        # Setup mock
        provider.client.messages.create.side_effect = RateLimitError("Rate limit", response=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMRateLimitError):
            provider.generate(b"bytes", "prompt")

    def test_generate_timeout_error(self, provider):
        # Setup mock
        provider.client.messages.create.side_effect = APITimeoutError("Timout")
        
        # Execute & Verify
        with pytest.raises(LLMTimeoutError):
            provider.generate(b"bytes", "prompt")

    def test_generate_server_error(self, provider):
         # Setup mock
        provider.client.messages.create.side_effect = APIError(message="Server Error", request=MagicMock(), body={})
        
        # Execute & Verify
        with pytest.raises(LLMServerError):
            provider.generate(b"bytes", "prompt")

import pytest
from unittest.mock import MagicMock, patch
from anthropic import APIError, AuthenticationError, APITimeoutError, RateLimitError
from app.services.llm.providers.anthropic_provider import AnthropicProvider
from app.services.llm.base_provider import LLMAuthenticationError, LLMTimeoutError

class TestAnthropicProvider:
    
    @pytest.fixture
    def provider(self):
        with patch("app.services.llm.providers.anthropic_provider.Anthropic"):
            provider = AnthropicProvider()
            provider.api_key = "fake_key"
            provider.client = MagicMock()
            return provider

    def test_generate_success(self, provider):
        # Mock response
        mock_response = MagicMock()
        mock_response.content = [MagicMock(text='{"invoice_number": "CLAUDE123"}')]
        provider.client.messages.create.return_value = mock_response
        
        # Execute
        result = provider.generate(b"fake_bytes", "prompt")
        
        # Verify
        assert result == '{"invoice_number": "CLAUDE123"}'
        
        # Verify call args
        call_args = provider.client.messages.create.call_args
        assert call_args.kwargs['system'] == "You are a helpful assistant. Output strict JSON."
        messages = call_args.kwargs['messages']
        assert messages[0]['content'][0]['type'] == 'image'
        assert messages[0]['content'][0]['source']['media_type'] == 'image/jpeg'

    def test_generate_auth_error(self, provider):
        provider.client.messages.create.side_effect = AuthenticationError("Bad Key", response=MagicMock(), body={})
        
        with pytest.raises(LLMAuthenticationError):
            provider.generate(b"bytes", "prompt")

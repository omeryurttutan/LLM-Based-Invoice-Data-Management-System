import pytest
from unittest.mock import MagicMock, patch
from openai import APIError, AuthenticationError, APITimeoutError, RateLimitError
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.base_provider import LLMAuthenticationError, LLMTimeoutError, LLMRateLimitError

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

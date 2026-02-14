import pytest
from unittest.mock import MagicMock, patch
from google.api_core import exceptions
from app.services.llm.providers.gemini_provider import GeminiProvider
from app.services.llm.base_provider import LLMAuthenticationError, LLMTimeoutError, LLMServerError

class TestGeminiProvider:
    
    @pytest.fixture
    def provider(self):
        with patch("google.generativeai.configure"):
            # Mock settings if needed, or assume default/env
            provider = GeminiProvider()
            provider.api_key = "fake_key" 
            return provider

    @patch("google.generativeai.GenerativeModel")
    def test_generate_success(self, mock_model_cls, provider):
        # Setup mock
        mock_model = MagicMock()
        mock_response = MagicMock()
        mock_response.text = '{"invoice_number": "123"}'
        mock_model.generate_content.return_value = mock_response
        mock_model_cls.return_value = mock_model
        
        # Execute
        result = provider.generate(b"fake_image_bytes", "fake_prompt")
        
        # Verify
        assert result == '{"invoice_number": "123"}'
        mock_model.generate_content.assert_called_once()

    @patch("google.generativeai.GenerativeModel")
    def test_generate_auth_error(self, mock_model_cls, provider):
        # Setup mock to raise InvalidArgument (which maps to Auth error in our provider)
        mock_model = MagicMock()
        mock_model.generate_content.side_effect = exceptions.InvalidArgument("Invalid API Key")
        mock_model_cls.return_value = mock_model
        
        # Execute & Verify
        with pytest.raises(LLMAuthenticationError):
            provider.generate(b"bytes", "prompt")

    @patch("google.generativeai.GenerativeModel")
    def test_generate_timeout_error(self, mock_model_cls, provider):
        # Setup mock
        mock_model = MagicMock()
        mock_model.generate_content.side_effect = exceptions.DeadlineExceeded("Timeout")
        mock_model_cls.return_value = mock_model
        
        # Execute & Verify
        with pytest.raises(LLMTimeoutError):
            provider.generate(b"bytes", "prompt")

    @patch("google.generativeai.GenerativeModel")
    def test_generate_server_error_retry(self, mock_model_cls, provider):
        # Setup mock to fail twice then succeed (to test retry)
        mock_model = MagicMock()
        # Side effect sequence: Fail, Fail, Success
        mock_response = MagicMock()
        mock_response.text = "Success"
        
        mock_model.generate_content.side_effect = [
            exceptions.InternalServerError("Server Error 1"),
            exceptions.InternalServerError("Server Error 2"),
            mock_response
        ]
        mock_model_cls.return_value = mock_model
        
        # Execute
        # Note: Retry logic handles the first two errors
        result = provider.generate(b"bytes", "prompt")
        
        # Verify
        assert result == "Success"
        assert mock_model.generate_content.call_count == 3

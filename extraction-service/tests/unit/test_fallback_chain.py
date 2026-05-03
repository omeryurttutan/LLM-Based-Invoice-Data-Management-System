import pytest
from unittest.mock import MagicMock, patch, call
from app.services.fallback_chain import FallbackChain
from app.models.response import InvoiceData
from app.models.enums import LLMProvider

class TestFallbackChain:
    
    @pytest.fixture
    def mock_gemini(self):
        with patch('app.services.fallback_chain.GeminiService') as mock:
            yield mock

    @pytest.fixture
    def mock_openai(self):
        with patch('app.services.fallback_chain.OpenAIService') as mock:
            yield mock

    @pytest.fixture
    def mock_anthropic(self):
        with patch('app.services.fallback_chain.AnthropicService') as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_chain_primary_success(self, mock_gemini, mock_openai, mock_anthropic, sample_image_bytes, mock_invoice_data):
        """Test that if Gemini succeeds, others are not called."""
        # Setup Gemini to succeed
        gemini_instance = mock_gemini.return_value
        gemini_instance.extract.return_value = mock_invoice_data
        
        chain = FallbackChain()
        result = await chain.execute(sample_image_bytes)
        
        assert result == mock_invoice_data
        # Verify call counts
        gemini_instance.extract.assert_called_once()
        mock_openai.return_value.extract.assert_not_called()
        mock_anthropic.return_value.extract.assert_not_called()

    @pytest.mark.asyncio
    async def test_chain_secondary_success(self, mock_gemini, mock_openai, mock_anthropic, sample_image_bytes, mock_invoice_data):
        """Test that if Gemini fails, OpenAI is called."""
        # Setup Gemini to fail, OpenAI to succeed
        gemini_instance = mock_gemini.return_value
        gemini_instance.extract.side_effect = Exception("Gemini Failed")
        
        openai_instance = mock_openai.return_value
        openai_instance.extract.return_value = mock_invoice_data
        
        chain = FallbackChain()
        result = await chain.execute(sample_image_bytes)
        
        assert result == mock_invoice_data
        # Verify call counts
        gemini_instance.extract.assert_called_once()
        openai_instance.extract.assert_called_once()
        mock_anthropic.return_value.extract.assert_not_called()

    @pytest.mark.asyncio
    async def test_chain_tertiary_success(self, mock_gemini, mock_openai, mock_anthropic, sample_image_bytes, mock_invoice_data):
        """Test that if Gemini and OpenAI fail, Anthropic is called."""
        # Setup Gemini and OpenAI to fail
        gemini_instance = mock_gemini.return_value
        gemini_instance.extract.side_effect = Exception("Gemini Failed")
        
        openai_instance = mock_openai.return_value
        openai_instance.extract.side_effect = Exception("OpenAI Failed")
        
        anthropic_instance = mock_anthropic.return_value
        anthropic_instance.extract.return_value = mock_invoice_data

        chain = FallbackChain()
        result = await chain.execute(sample_image_bytes)
        
        assert result == mock_invoice_data
        # Verify call counts
        gemini_instance.extract.assert_called_once()
        openai_instance.extract.assert_called_once()
        anthropic_instance.extract.assert_called_once()

    @pytest.mark.asyncio
    async def test_chain_all_fail(self, mock_gemini, mock_openai, mock_anthropic, sample_image_bytes):
        """Test exception raised when all providers fail."""
        # Setup all to fail
        mock_gemini.return_value.extract.side_effect = Exception("Gemini Failed")
        mock_openai.return_value.extract.side_effect = Exception("OpenAI Failed")
        mock_anthropic.return_value.extract.side_effect = Exception("Anthropic Failed")
        
        chain = FallbackChain()
        with pytest.raises(Exception) as excinfo:
            await chain.execute(sample_image_bytes)
        
        assert "All LLM providers failed" in str(excinfo.value)

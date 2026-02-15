import pytest
from unittest.mock import MagicMock, patch
import json
from app.services.anthropic_service import AnthropicService
from app.models.response import InvoiceData

class TestAnthropicService:
    
    @pytest.fixture
    def mock_anthropic_client(self):
        with patch('app.services.anthropic_service.AsyncAnthropic') as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_extract_success(self, mock_anthropic_client, sample_image_bytes, mock_claude_response_json):
        """Test successful extraction with Anthropic."""
        # Setup mock response
        mock_completion = MagicMock()
        mock_completion.content[0].text = mock_claude_response_json
        
        # Async mock setup
        mock_client_instance = mock_anthropic_client.return_value
        mock_client_instance.messages.create.return_value = mock_completion
        
        service = AnthropicService()
        result = await service.extract(sample_image_bytes)
        
        assert isinstance(result, InvoiceData)
        assert result.fatura_no == "GIB2024000000003"
        assert result.para_birimi == "EUR"

    @pytest.mark.asyncio
    async def test_extract_api_error(self, mock_anthropic_client, sample_image_bytes):
        """Test handling of API errors."""
        mock_client_instance = mock_anthropic_client.return_value
        mock_client_instance.messages.create.side_effect = Exception("Anthropic API Error")
        
        service = AnthropicService()
        with pytest.raises(Exception) as excinfo:
            await service.extract(sample_image_bytes)
        assert "Anthropic extraction failed" in str(excinfo.value)

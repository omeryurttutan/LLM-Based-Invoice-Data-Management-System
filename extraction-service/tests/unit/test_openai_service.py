import pytest
from unittest.mock import MagicMock, patch
import json
from app.services.openai_service import OpenAIService
from app.models.response import InvoiceData

class TestOpenAIService:
    
    @pytest.fixture
    def mock_openai_client(self):
        with patch('app.services.openai_service.AsyncOpenAI') as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_extract_success(self, mock_openai_client, sample_image_bytes, mock_gpt_response_json):
        """Test successful extraction with OpenAI."""
        # Setup mock response
        mock_completion = MagicMock()
        mock_completion.choices[0].message.content = mock_gpt_response_json
        
        # Async mock setup
        mock_client_instance = mock_openai_client.return_value
        mock_client_instance.chat.completions.create.return_value = mock_completion
        
        service = OpenAIService()
        result = await service.extract(sample_image_bytes)
        
        assert isinstance(result, InvoiceData)
        assert result.fatura_no == "GIB2024000000002"
        assert result.genel_toplam == 1200.00
        assert result.para_birimi == "USD"

    @pytest.mark.asyncio
    async def test_extract_api_error(self, mock_openai_client, sample_image_bytes):
        """Test handling of API errors."""
        mock_client_instance = mock_openai_client.return_value
        mock_client_instance.chat.completions.create.side_effect = Exception("OpenAI API Error")
        
        service = OpenAIService()
        with pytest.raises(Exception) as excinfo:
            await service.extract(sample_image_bytes)
        assert "OpenAI extraction failed" in str(excinfo.value)

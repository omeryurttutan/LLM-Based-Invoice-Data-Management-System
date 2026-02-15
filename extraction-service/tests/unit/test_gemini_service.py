import pytest
from unittest.mock import MagicMock, patch
import json
from app.services.gemini_service import GeminiService
from app.models.response import InvoiceData

class TestGeminiService:
    
    @pytest.fixture
    def mock_genai(self):
        with patch('app.services.gemini_service.genai') as mock:
            yield mock

    def test_extract_success(self, mock_genai, sample_image_bytes, mock_gemini_response_json):
        """Test successful extraction with Gemini."""
        # Setup mock response
        mock_response = MagicMock()
        mock_response.text = mock_gemini_response_json
        mock_genai.GenerativeModel.return_value.generate_content.return_value = mock_response
        
        service = GeminiService()
        result = service.extract(sample_image_bytes)
        
        assert isinstance(result, InvoiceData)
        assert result.fatura_no == "GIB2024000000001"
        assert result.genel_toplam == 1200.00
        assert len(result.kalemler) == 1

    def test_extract_api_error(self, mock_genai, sample_image_bytes):
        """Test handling of API errors."""
        mock_genai.GenerativeModel.return_value.generate_content.side_effect = Exception("API Error")
        
        service = GeminiService()
        with pytest.raises(Exception) as excinfo:
            service.extract(sample_image_bytes)
        assert "Gemini extraction failed" in str(excinfo.value)

    def test_extract_invalid_json(self, mock_genai, sample_image_bytes):
        """Test handling of invalid JSON response."""
        mock_response = MagicMock()
        mock_response.text = "Not JSON"
        mock_genai.GenerativeModel.return_value.generate_content.return_value = mock_response
        
        service = GeminiService()
        with pytest.raises(ValueError):
            service.extract(sample_image_bytes)

    def test_extract_rate_limit(self, mock_genai, sample_image_bytes):
        """Test handling of 429 Too Many Requests (simulated by exception)."""
        # Note: Actual Google API might raise specific ResourceExhausted exception
        # Here we check generic exception handling or specific if we knew the implementation details
        mock_genai.GenerativeModel.return_value.generate_content.side_effect = Exception("429 Resource Exhausted")
        
        service = GeminiService()
        with pytest.raises(Exception):
            service.extract(sample_image_bytes)


import pytest
import os
from unittest.mock import MagicMock, patch, AsyncMock
from fastapi import UploadFile
from app.services.extraction.extraction_service import ExtractionService
from app.models.invoice_data import InvoiceData

@pytest.mark.asyncio
class TestExtractionFlow:
    
    @pytest.fixture
    def mock_file(self):
        file = MagicMock(spec=UploadFile)
        file.filename = "test_invoice.jpg"
        file.read = AsyncMock(return_value=b"fake_image_bytes")
        return file

    @patch("app.services.extraction.extraction_service.PreprocessingPipeline")
    @patch("app.services.llm.fallback_chain.GeminiProvider") 
    # Note: FallbackChain instantiates providers inside __init__. We need to mock them.
    # A better way is to mock FallbackChain.generate_with_fallback directly if we just want to test orchestration.
    # But if we want to test integration of components, we should mock the provider.
    
    @patch("app.services.llm.fallback_chain.FallbackChain.generate_with_fallback")
    async def test_extract_from_file_success(self, mock_generate, mock_pipeline_cls, mock_file):
        # Setup mocks
        mock_pipeline = mock_pipeline_cls.return_value
        mock_pipeline.process.return_value.data = b"processed_bytes"
        
        # Mock LLM response
        mock_llm_response = '{"invoice_number": "INV-001", "total_amount": 150.00, "currency": "TRY"}'
        mock_generate.return_value = (mock_llm_response, "GEMINI", [])
        
        service = ExtractionService()
        
        # Execute
        result = await service.extract_from_file(mock_file)
        
        # Verify
        assert result.provider == "GEMINI"
        assert result.data.invoice_number == "INV-001"
        assert result.data.total_amount == 150.00
        assert result.processing_time_ms > 0
        
        mock_pipeline.process.assert_called_once()
        mock_generate.assert_called_once()

    @patch("app.services.extraction.extraction_service.PreprocessingPipeline")
    @patch("app.services.llm.fallback_chain.FallbackChain.generate_with_fallback")
    async def test_extract_from_base64_success(self, mock_generate, mock_pipeline_cls):
        # Setup mocks
        mock_pipeline = mock_pipeline_cls.return_value
        mock_pipeline.process.return_value.data = b"processed_bytes"
        
        mock_llm_response = '{"invoice_number": "B64-INV", "total_amount": 99.99}'
        mock_generate.return_value = (mock_llm_response, "GEMINI", [])
        
        service = ExtractionService()
        
        # Execute
        result = await service.extract_from_base64("data:image/jpeg;base64,ZmFrZQ==")
        
        # Verify
        assert result.data.invoice_number == "B64-INV"
        mock_pipeline.process.assert_called_once()

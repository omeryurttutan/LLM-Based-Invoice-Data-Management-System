import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from fastapi import UploadFile
from app.services.extraction.extraction_service import ExtractionService
from app.services.llm.providers.gemini_provider import GeminiProvider
from app.models.extraction import ExtractionResponse

@pytest.fixture
def mock_fallback_chain():
    with patch("app.services.extraction.extraction_service.FallbackChain") as MockChain:
        chain_instance = MockChain.return_value
        # Mock generate_with_fallback to return (response_text, provider_name, logs)
        valid_json = '{"invoice_number": "MOCK-123", "total_amount": 120.00, "invoice_date": "2023-10-10", "supplier_name": "Mock Supplier", "supplier_tax_number": "123", "subtotal": 100.0, "tax_amount": 20.0, "currency": "TRY", "items": []}'
        chain_instance.generate_with_fallback = AsyncMock(return_value=(valid_json, "GEMINI", []))
        yield chain_instance

@pytest.fixture
def mock_preprocessing():
    with patch("app.services.extraction.extraction_service.PreprocessingPipeline") as MockPipeline:
        pipeline_instance = MockPipeline.return_value
        pipeline_instance.process.return_value = MagicMock(data=b"processed_image")
        yield pipeline_instance

@pytest.mark.asyncio
async def test_extraction_flow(mock_fallback_chain, mock_preprocessing):
    service = ExtractionService()
    mock_file = MagicMock(spec=UploadFile)
    mock_file.filename = "test.jpg"
    mock_file.read = AsyncMock(return_value=b"raw_bytes")
    
    result = await service.extract_from_file(mock_file)
    
    assert isinstance(result, ExtractionResponse)
    assert result.provider == "GEMINI"
    
    mock_preprocessing.process.assert_called_once()
    mock_fallback_chain.generate_with_fallback.assert_called_once()

@pytest.mark.asyncio
async def test_extraction_base64_flow(mock_fallback_chain, mock_preprocessing):
    service = ExtractionService()
    base64_string = "data:image/jpeg;base64,raw_bytes"
    
    result = await service.extract_from_base64(base64_string)
    
    assert isinstance(result, ExtractionResponse)
    assert result.provider == "GEMINI"
    
    mock_preprocessing.process.assert_called_once()
    mock_fallback_chain.generate_with_fallback.assert_called_once()

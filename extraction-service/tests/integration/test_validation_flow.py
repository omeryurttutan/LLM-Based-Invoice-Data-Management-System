import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from fastapi import UploadFile
from app.services.extraction.extraction_service import ExtractionService
from app.models.extraction import ExtractionResponse

@pytest.fixture
def mock_fallback_chain_valid():
    with patch("app.services.extraction.extraction_service.FallbackChain") as MockChain:
        chain_instance = MockChain.return_value
        # Return a JSON with some issues to test validation
        # Missing total_amount (Critical), Invoice Date in future (Warning)
        valid_json = '''
        {
            "invoice_number": "INV-001",
            "invoice_date": "2099-01-01", 
            "supplier_name": "Test Supplier",
            "supplier_tax_number": "1234567890",
            "subtotal": 100.0,
            "tax_amount": 20.0,
            "currency": "TRY",
            "items": []
        }
        '''
        chain_instance.generate_with_fallback = AsyncMock(return_value=(valid_json, "GEMINI", []))
        yield chain_instance

@pytest.fixture
def mock_preprocessing():
    with patch("app.services.extraction.extraction_service.PreprocessingPipeline") as MockPipeline:
        pipeline_instance = MockPipeline.return_value
        pipeline_instance.process.return_value = MagicMock(data=b"processed_image")
        yield pipeline_instance

@pytest.mark.asyncio
async def test_validation_integration(mock_fallback_chain_valid, mock_preprocessing):
    service = ExtractionService()
    mock_file = MagicMock(spec=UploadFile)
    mock_file.filename = "test.jpg"
    mock_file.read = AsyncMock(return_value=b"raw_bytes")
    
    result = await service.extract_from_file(mock_file)
    
    assert isinstance(result, ExtractionResponse)
    assert result.validation_result is not None
    
    # Check that validation actually ran
    # Missing total_amount is CRITICAL (-25)
    # Future date is WARNING (-20)
    # Empty items is WARNING (Field: -10, CrossField: -10)
    # Score should be well below 100
    assert result.confidence_score < 100.0
    
    # Check suggested status
    assert result.suggested_status in ["NEEDS_REVIEW", "LOW_CONFIDENCE"]
    
    # Check issues list
    issues = result.validation_result.issues
    assert len(issues) > 0
    
    # Verify we have at least one critical issue (missing total_amount)
    # Actually total_amount defaults to None if missing in JSON? 
    # ResponseParser might default it. 
    # Let's check if total_amount missing triggered FieldValidator critical error
    
    # In JSON above, total_amount is NOT present.
    # ResponseParser normalize might set it to None or 0.
    # FieldValidator checks "if not value". 0 is falsy but we allowed 0 in FieldValidator for numeric?
    # Wait, FieldValidator: if not value -> score -= 25.
    # If total_amount is None, score -= 25.
    
    critical_issues = [i for i in issues if i.severity == "CRITICAL"]
    assert len(critical_issues) > 0
    
    print(f"Final Score: {result.confidence_score}")
    print(f"Status: {result.suggested_status}")

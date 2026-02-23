"""
Unit tests for ExtractionService — Phase 15.
Tests the full pipeline: preprocess → LLM → parse → validate.
"""
import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from fastapi import UploadFile

from app.services.extraction.extraction_service import ExtractionService
from app.models.extraction import ExtractionResponse
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationResult, ValidationSeverity, ValidationCategory
from app.services.llm.base_provider import LLMProviderNames


VALID_LLM_JSON = '{"invoice_number": "MOCK-123", "total_amount": 120.00, "invoice_date": "2024-10-10", "supplier_name": "Mock Supplier", "supplier_tax_number": "1234567890", "subtotal": 100.0, "tax_amount": 20.0, "currency": "TRY", "items": []}'


def _make_validation_result(**overrides):
    defaults = {
        "confidence_score": 85.0,
        "suggested_status": "NEEDS_REVIEW",
        "category_scores": {"A": 90.0, "B": 80.0, "C": 85.0, "D": 90.0, "E": 80.0},
        "issues": [],
        "summary": "Test validation",
    }
    defaults.update(overrides)
    return ValidationResult(**defaults)


@pytest.fixture
def mock_dependencies():
    """Mock all external dependencies of ExtractionService."""
    with patch("app.services.extraction.extraction_service.PreprocessingPipeline") as MockPipeline, \
         patch("app.services.extraction.extraction_service.FallbackChain") as MockChain, \
         patch("app.services.extraction.extraction_service.Validator") as MockValidator, \
         patch("app.services.parsers.xml_parser.XMLParser") as MockXML, \
         patch("app.services.parsers.file_type_detector.FileTypeDetector") as MockDetector:

        # Preprocessing mock
        pipeline_instance = MockPipeline.return_value
        mock_processed = MagicMock()
        mock_processed.image_data = "aW1hZ2VfZGF0YQ=="  # base64 of "image_data"
        mock_processed.mime_type = "image/jpeg"
        pipeline_instance.process = AsyncMock(return_value=mock_processed)

        # Fallback chain mock
        chain_instance = MockChain.return_value
        usage = {"input_tokens": 500, "output_tokens": 200}
        chain_instance.generate_with_fallback = AsyncMock(
            return_value=(VALID_LLM_JSON, LLMProviderNames.GEMINI_3_FLASH, [], usage)
        )

        # Validator mock — return a real Pydantic model
        validator_instance = MockValidator.return_value
        validator_instance.validate.return_value = _make_validation_result()

        # File type detector mock
        detector_instance = MockDetector.return_value
        detector_instance.detect.return_value = "IMAGE"

        # XML parser mock
        xml_instance = MockXML.return_value

        yield {
            "pipeline": pipeline_instance,
            "chain": chain_instance,
            "validator": validator_instance,
            "detector": detector_instance,
            "xml": xml_instance,
        }


class TestExtractionService:

    @pytest.mark.asyncio
    async def test_extract_from_file(self, mock_dependencies):
        """Test full extraction flow from uploaded file."""
        service = ExtractionService()

        mock_file = MagicMock(spec=UploadFile)
        mock_file.filename = "test_invoice.jpg"
        mock_file.content_type = "image/jpeg"
        mock_file.read = AsyncMock(return_value=b"raw_image_bytes")

        result = await service.extract_from_file(mock_file)

        assert isinstance(result, ExtractionResponse)
        assert result.provider == LLMProviderNames.GEMINI_3_FLASH
        assert result.data.invoice_number == "MOCK-123"
        assert result.data.total_amount == 120.0
        assert result.prompt_version == "v1"
        assert result.processing_time_ms > 0
        assert result.llm_processing_time_ms is not None

        mock_dependencies["pipeline"].process.assert_called_once()
        mock_dependencies["chain"].generate_with_fallback.assert_called_once()

    @pytest.mark.asyncio
    async def test_extract_from_base64(self, mock_dependencies):
        """Test extraction from base64-encoded image."""
        service = ExtractionService()

        # base64 of "test_data"
        result = await service.extract_from_base64("data:image/jpeg;base64,dGVzdF9kYXRh")

        assert isinstance(result, ExtractionResponse)
        assert result.provider == LLMProviderNames.GEMINI_3_FLASH
        assert result.data.invoice_number == "MOCK-123"

    @pytest.mark.asyncio
    async def test_extraction_result_metadata(self, mock_dependencies):
        """Verify all metadata fields are populated in ExtractionResult."""
        service = ExtractionService()

        mock_file = MagicMock(spec=UploadFile)
        mock_file.filename = "invoice.pdf"
        mock_file.content_type = "application/pdf"
        mock_file.read = AsyncMock(return_value=b"pdf_bytes")

        result = await service.extract_from_file(mock_file)

        # Check metadata
        assert result.prompt_version == "v1"
        assert result.raw_response is not None
        assert result.input_tokens == 500
        assert result.output_tokens == 200
        assert result.confidence_score == 85.0
        assert result.suggested_status == "NEEDS_REVIEW"

    @pytest.mark.asyncio
    async def test_extraction_xml_routing(self, mock_dependencies):
        """XML files should be routed to XML parser, not LLM."""
        mock_dependencies["detector"].detect.return_value = "XML"

        # Mock XML parser to return a real InvoiceData instance
        mock_invoice = InvoiceData(invoice_number="XML-001")
        mock_dependencies["xml"].parse.return_value = mock_invoice

        service = ExtractionService()

        mock_file = MagicMock(spec=UploadFile)
        mock_file.filename = "invoice.xml"
        mock_file.content_type = "text/xml"
        mock_file.read = AsyncMock(return_value=b"<xml>data</xml>")

        result = await service.extract_from_file(mock_file)
        assert result.provider == "XML_PARSER"

        # LLM should NOT have been called
        mock_dependencies["chain"].generate_with_fallback.assert_not_called()

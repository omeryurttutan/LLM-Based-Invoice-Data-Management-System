import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from app.services.extraction_service import ExtractionService
from app.models.response import InvoiceData
from app.models.request import ExtractionRequest
from app.models.enums import FileType

class TestExtractionService:
    
    @pytest.fixture
    def mock_components(self):
        with patch('app.services.extraction_service.PreprocessingService') as mock_prep, \
             patch('app.services.extraction_service.FallbackChain') as mock_chain, \
             patch('app.services.extraction_service.ValidationService') as mock_val, \
             patch('app.services.extraction_service.ConfidenceScorer') as mock_score, \
             patch('app.services.extraction_service.XMLParserService') as mock_xml, \
             patch('app.services.extraction_service.ResponseNormalizer') as mock_norm:
            yield {
                'prep': mock_prep,
                'chain': mock_chain,
                'val': mock_val,
                'score': mock_score,
                'xml': mock_xml,
                'norm': mock_norm
            }

    @pytest.mark.asyncio
    async def test_extract_image_flow(self, mock_components, sample_image_bytes, mock_invoice_data):
        """Test full extraction flow for an image."""
        # Setup mocks
        mock_components['prep'].preprocess_image.return_value = sample_image_bytes
        # FallbackChain is likely instantiated inside the service or injected
        # Assuming FallbackChain().execute() is called
        mock_chain_instance = mock_components['chain'].return_value
        mock_chain_instance.execute.return_value = mock_invoice_data
        
        mock_components['val'].validate.return_value = [] # No validation issues
        mock_components['score'].calculate_score.return_value = 95.0
        
        service = ExtractionService()
        result = await service.process_extraction(sample_image_bytes, file_type=FileType.IMAGE)
        
        assert isinstance(result, InvoiceData)
        assert result.confidence_score == 95.0
        assert len(result.validation_issues) == 0
        
        # Verify calls
        mock_components['prep'].preprocess_image.assert_called_once()
        mock_chain_instance.execute.assert_called_once()

    @pytest.mark.asyncio
    async def test_extract_xml_flow(self, mock_components, sample_xml_content, mock_invoice_data):
        """Test full extraction flow for XML."""
        # Setup mocks
        mock_components['xml'].parse.return_value = mock_invoice_data
        mock_components['val'].validate.return_value = []
        mock_components['score'].calculate_score.return_value = 100.0 # XML usually high confidence
        
        service = ExtractionService()
        result = await service.process_extraction(sample_xml_content.encode('utf-8'), file_type=FileType.XML)
        
        assert isinstance(result, InvoiceData)
        assert result.confidence_score == 100.0
        
        # Verify calls
        mock_components['prep'].preprocess_image.assert_not_called()
        mock_components['chain'].return_value.execute.assert_not_called()
        mock_components['xml'].parse.assert_called_once()

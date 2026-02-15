from app.models.invoice_data import InvoiceData
from app.models.extraction import ExtractionResponse
from app.models.validation import ValidationResult
from app.services.llm.base_provider import LLMError

class TestExtractionService:

    @pytest.fixture
    def service(self):
        with patch("app.services.extraction.extraction_service.PreprocessingPipeline") as MockPipeline, \
             patch("app.services.extraction.extraction_service.FallbackChain") as MockChain, \
             patch("app.services.extraction.extraction_service.ResponseParser") as MockParser, \
             patch("app.services.extraction.extraction_service.Validator") as MockValidator, \
             patch("app.services.parsers.xml_parser.XMLParser") as MockXMLParser, \
             patch("app.services.parsers.ubl_field_extractor.UBLFieldExtractor") as MockExtractor, \
             patch("app.services.parsers.file_type_detector.FileTypeDetector") as MockDetector, \
             patch("app.services.extraction.extraction_service.PromptManager") as MockPromptManager:
            
            service = ExtractionService()
            service.pipeline = MockPipeline.return_value
            service.chain = MockChain.return_value
            service.validator = MockValidator.return_value
            service.xml_parser = MockXMLParser.return_value
            service.file_type_detector = MockDetector.return_value
            service.prompt_manager = MockPromptManager.return_value
            
            return service

    @pytest.mark.asyncio
    async def test_extract_invoice_image_success(self, service):
        # Setup
        request = ExtractionRequest(file_content=b"image", filename="invoice.jpg")
        
        # Mock file type
        service.file_type_detector.detect.return_value = "image/jpeg"
        
        # Mock preprocessing
        mock_processed = MagicMock()
        mock_processed.processed_content = b"processed"
        mock_processed.mime_type = "image/jpeg"
        service.pipeline.process.return_value = mock_processed
        
        # Mock chain (AsyncMock for async method)
        service.chain.generate_with_fallback = AsyncMock(return_value=('{"invoice_number": "123"}', "GEMINI", []))
        
        # Mock parser - Use REAL InvoiceData object for Pydantic validation
        mock_data = InvoiceData(invoice_number="123")
        with patch("app.services.extraction.extraction_service.ResponseParser.parse", return_value=mock_data):
             # Mock validator (Real object needed for Pydantic)
             validation_res = ValidationResult(
                 confidence_score=95.0,
                 suggested_status="AUTO_VERIFIED",
                 category_scores={},
                 issues=[],
                 summary="Success"
             )
             service.validator.validate.return_value = validation_res

             # Execute
             # process_file_content takes bytes, filename, content_type
             result = await service.process_file_content(b"image", "invoice.jpg", "image/jpeg")
             
             # Verify
             assert isinstance(result, ExtractionResponse)
             assert result.data.invoice_number == "123"
             assert result.provider == "GEMINI"
             assert result.validation_result.confidence_score == 95.0
             service.chain.generate_with_fallback.assert_called_once()

    @pytest.mark.asyncio
    async def test_extract_invoice_xml_success(self, service):
        # Setup
        # request = ExtractionRequest(file_content=b"<Invoice>...</Invoice>", filename="invoice.xml")
        
        # Mock file type
        service.file_type_detector.detect.return_value = "XML" # Must be XML for routing
        
        # Mock XML parser - Use REAL InvoiceData object
        mock_data = InvoiceData(invoice_number="XML-123")
        service.xml_parser.parse.return_value = mock_data
        
        # Mock validator
        validation_res = ValidationResult(
             confidence_score=100.0,
             suggested_status="AUTO_VERIFIED",
             category_scores={},
             issues=[],
             summary="Success"
        )
        service.validator.validate.return_value = validation_res
        
        # Execute
        result = await service.process_file_content(b"<Invoice>...</Invoice>", "invoice.xml", "application/xml")
        
        # Verify
        assert result.data.invoice_number == "XML-123"
        assert result.provider == "XML_PARSER"
        # LLM chain should NOT be called
        service.chain.generate_with_fallback.assert_not_called()

    @pytest.mark.asyncio
    async def test_extract_invoice_llm_failure(self, service):
        # Setup
        service.file_type_detector.detect.return_value = "IMAGE"
        service.pipeline.process.return_value = MagicMock(processed_content=b"proc", mime_type="image/jpeg")
        
        # Raise error
        service.chain.generate_with_fallback = AsyncMock(side_effect=LLMError("All failed"))
        
        # Execute & Verify
        with pytest.raises(LLMError):
            await service.process_file_content(b"image", "invoice.jpg", "image/jpeg")


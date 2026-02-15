import time
import base64
from typing import Optional, Union, BinaryIO
from fastapi import UploadFile

from app.services.preprocessing.pipeline import PreprocessingPipeline
from app.services.llm.fallback_chain import FallbackChain
from app.services.llm.prompt_manager import PromptManager
from app.services.llm.response_parser import ResponseParser
from app.models.extraction import ExtractionResponse
from app.api.routes import preprocessing
from app.core.logging import logger
from app.services.validation.validator import Validator

class ExtractionService:
    """
    Orchestrates the invoice extraction process:
    1. Preprocessing (Phase 14)
    2. Prompt Generation
    3. LLM Generation (Fallback Chain)
    4. Response Parsing
    """
    
    def __init__(self):
        self.preprocessing_pipeline = PreprocessingPipeline()
        self.fallback_chain = FallbackChain()
        self.prompt_manager = PromptManager()
        self.parser = ResponseParser()
        self.validator = Validator()
        # Phase 18: XML Parsing
        from app.services.parsers.xml_parser import XMLParser
        from app.services.parsers.file_type_detector import FileTypeDetector
        self.xml_parser = XMLParser()
        self.file_detector = FileTypeDetector()

    async def extract_from_file(self, file: UploadFile) -> ExtractionResponse:
        """
        Extract data from an uploaded file (PDF/Image OR XML).
        Smart routing based on file type.
        """
        # Read file content
        file_bytes = await file.read()
        return await self.process_file_content(file_bytes, file.filename or "", file.content_type)

    async def process_file_content(self, file_bytes: bytes, filename: str, content_type: str = None) -> ExtractionResponse:
        """
        Process file content (bytes) directly. 
        Detects file type and routes to appropriate processor.
        """
        start_time = time.time()
        
        # Detect File Type
        try:
            file_type = self.file_detector.detect(filename, file_bytes, content_type)
        except Exception as e:
            logger.warning("file_type_detection_failed", error=str(e))
            file_type = "IMAGE" # Default to image path if detection fails

        if file_type == "XML":
            return await self._process_xml(file_bytes, start_time)
        else:
            # 1. Preprocess (Image/PDF)
            processed_image = await self.preprocessing_pipeline.process(
                file_bytes, 
                filename=filename
            )
            
            preprocess_duration = (time.time() - start_time) * 1000
            logger.info("preprocessing_completed", duration_ms=preprocess_duration)
            
            # processed_image.image_data is base64 string, we need bytes for LLM
            image_bytes_processed = base64.b64decode(processed_image.image_data)
            return await self._process_image(image_bytes_processed, start_time)

    async def extract_from_base64(self, base64_string: str) -> ExtractionResponse:
        """
        Extract data from a base64 string (Image OR XML).
        """
        start_time = time.time()
        
        # Decode base64
        if "," in base64_string:
            base64_string = base64_string.split(",")[1]
            
        file_bytes = base64.b64decode(base64_string)
        
        # Detect File Type
        try:
            # No filename or content-type available for base64 usually
            file_type = self.file_detector.detect("base64_upload", file_bytes)
        except Exception:
             file_type = "IMAGE"

        if file_type == "XML":
             return await self._process_xml(file_bytes, start_time)
        else:
            processed_image = await self.preprocessing_pipeline.process(
                file_bytes,
                filename="base64_upload.jpg" 
            )
            # processed_image.image_data is base64 string
            image_bytes_processed = base64.b64decode(processed_image.image_data)
            return await self._process_image(image_bytes_processed, start_time)

    async def extract_xml(self, file: UploadFile) -> ExtractionResponse:
        """
        Extract data explicitly from an XML file.
        Raises error if file is not XML.
        """
        start_time = time.time()
        file_bytes = await file.read()
        
        try:
            file_type = self.file_detector.detect(file.filename or "", file_bytes, file.content_type)
        except Exception:
            file_type = "UNKNOWN"
            
        if file_type != "XML":
            # Just to be safe, check if it looks like XML even if detector was unsure?
            # But detector is the authority.
            # If explicit XML parse is requested, we strictly require XML.
            from app.core.exceptions import UnsupportedFormatError
            raise UnsupportedFormatError(f"File is not detected as XML (detected: {file_type})", format=file_type)

        return await self._process_xml(file_bytes, start_time)

    async def _process_xml(self, xml_bytes: bytes, start_time: float) -> ExtractionResponse:
        """
        Internal method to handle XML e-Invoice parsing.
        """
        logger.info("processing_xml_invoice")
        
        # 1. Parse XML
        invoice_data = self.xml_parser.parse(xml_bytes)
        
        # 2. Validation
        validation_result = self.validator.validate(invoice_data)
        
        processing_time = (time.time() - start_time) * 1000
        
        logger.info("xml_parsing_completed", invoice_number=invoice_data.invoice_number, duration_ms=processing_time)
        
        return ExtractionResponse(
            data=invoice_data,
            provider="XML_PARSER",
            processing_time_ms=processing_time,
            confidence_score=validation_result.confidence_score,
            suggested_status=validation_result.suggested_status,
            validation_result=validation_result,
            raw_response=None, # Or truncate xml_bytes.decode() if needed
            fallback_attempts=None
        )

    async def _process_image(self, image_data: bytes, start_time: float) -> ExtractionResponse:
        """
        Internal method to handle LLM interaction after image is ready.
        """
        # 1. LLM Extraction with Fallback
        prompt = self.prompt_manager.get_prompt()
        llm_response_text, provider_name, fallback_logs = await self.fallback_chain.generate_with_fallback(image_data, prompt)
        
        # 2. Parse Response
        try:
            invoice_data = self.parser.parse(llm_response_text)
        except Exception as e:
             raise e
        
        # 3. Validation
        validation_result = self.validator.validate(invoice_data)
        
        processing_time = (time.time() - start_time) * 1000
        
        return ExtractionResponse(
            data=invoice_data,
            provider=provider_name,
            processing_time_ms=processing_time,
            fallback_attempts=fallback_logs,
            validation_result=validation_result,
            confidence_score=validation_result.confidence_score,
            suggested_status=validation_result.suggested_status
        )

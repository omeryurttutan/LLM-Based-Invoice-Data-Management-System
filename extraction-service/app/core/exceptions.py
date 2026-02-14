class ExtractionServiceException(Exception):
    """Base exception for the extraction service"""
    def __init__(self, message: str, code: str = "INTERNAL_ERROR", details: dict = None):
        self.message = message
        self.code = code
        self.details = details or {}
        super().__init__(self.message)

class FileValidationError(ExtractionServiceException):
    """Raised when file validation fails"""
    def __init__(self, message: str, details: dict = None):
        super().__init__(message, code="FILE_VALIDATION_ERROR", details=details)

class LLMProviderError(ExtractionServiceException):
    """Raised when LLM provider call fails"""
    def __init__(self, message: str, provider: str, details: dict = None):
        details = details or {}
        details["provider"] = provider
        super().__init__(message, code="LLM_PROVIDER_ERROR", details=details)

class ConfigurationError(ExtractionServiceException):
    """Raised when configuration is invalid or missing"""
    def __init__(self, message: str):
        super().__init__(message, code="CONFIGURATION_ERROR")

class PreprocessingError(ExtractionServiceException):
    """Base exception for preprocessing errors"""
    def __init__(self, message: str, code: str = "PREPROCESSING_ERROR"):
        super().__init__(message, code=code)

class UnsupportedFormatError(PreprocessingError):
    """Raised when file format is not supported"""
    def __init__(self, message: str, format: str = None):
        super().__init__(message, code="UNSUPPORTED_FORMAT_ERROR")
        if format:
            self.details = {"format": format}

class CorruptedFileError(PreprocessingError):
    """Raised when file is corrupted"""
    def __init__(self, message: str):
        super().__init__(message, code="CORRUPTED_FILE_ERROR")

class FileTooLargeError(PreprocessingError):
    """Raised when file exceeds size limit"""
    def __init__(self, message: str):
        super().__init__(message, code="FILE_TOO_LARGE_ERROR")

class PDFConversionError(PreprocessingError):
    """Raised when PDF conversion fails"""
    def __init__(self, message: str):
        super().__init__(message, code="PDF_CONVERSION_ERROR")

class ImageProcessingError(PreprocessingError):
    """Raised when image processing operation fails"""
    def __init__(self, message: str):
        super().__init__(message, code="IMAGE_PROCESSING_ERROR")

class ParserError(ExtractionServiceException):
    """Base exception for parser errors"""
    def __init__(self, message: str, code: str = "PARSER_ERROR"):
        super().__init__(message, code=code)

class XMLParseError(ParserError):
    """Raised when XML parsing fails (malformed XML)"""
    def __init__(self, message: str):
        super().__init__(message, code="XML_PARSE_ERROR")

class NotEInvoiceError(ParserError):
    """Raised when file is valid XML but not a UBL-TR e-Invoice"""
    def __init__(self, message: str):
        super().__init__(message, code="NOT_E_INVOICE_ERROR")

class MissingRequiredFieldError(ParserError):
    """Raised when a mandatory field is missing"""
    def __init__(self, field_name: str):
        super().__init__(f"Missing required field: {field_name}", code="MISSING_REQUIRED_FIELD_ERROR")

class NamespaceError(ParserError):
    """Raised when XML namespace resolution fails"""
    def __init__(self, message: str):
        super().__init__(message, code="NAMESPACE_ERROR")

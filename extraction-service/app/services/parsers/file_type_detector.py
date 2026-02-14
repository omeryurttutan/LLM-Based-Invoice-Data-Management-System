import mimetypes
from typing import Optional, BinaryIO

class FileTypeDetector:
    """
    Detects whether a file is an XML (e-Invoice) or an Image/PDF.
    """
    
    XML_MIME_TYPES = {'application/xml', 'text/xml'}
    IMAGE_MIME_TYPES = {'image/jpeg', 'image/png', 'image/jpg', 'application/pdf', 'image/tiff', 'image/webp', 'image/bmp'}
    
    @staticmethod
    def detect(filename: str, content: bytes, content_type: Optional[str] = None) -> str:
        """
        Detects the file type based on filename, content, and provided content_type.
        Returns 'XML' or 'IMAGE'.
        Raises ValueError if type cannot be determined or is unsupported.
        """
        
        # 1. Check Magic Bytes (Most reliable)
        if content.lstrip().startswith(b'<'):
            # Potentially XML (or HTML, SVG, etc.)
            # Check for <?xml or specific root element logic could go here, 
            # but for now, starting with < is a strong indicator of XML-like.
            # E-Invoices start with <Invoice ... or <?xml ...
            
            # Let's be slightly more specific to avoid false positives with other markups if needed
            # but simple check is usually enough given the domain.
            # We can also check if it contains "Invoice" or "UBL" later in parser, 
            # here we just want to decide specific routing.
            return 'XML'
            
        # 2. Check content_type header if available and trustworthy?
        # Often ignored in favor of magic bytes, but good as fallback.
        if content_type:
            if content_type in FileTypeDetector.XML_MIME_TYPES:
                return 'XML'
            if content_type in FileTypeDetector.IMAGE_MIME_TYPES:
                return 'IMAGE'

        # 3. Check Extension
        guess_type, _ = mimetypes.guess_type(filename)
        if guess_type:
            if guess_type in FileTypeDetector.XML_MIME_TYPES:
                return 'XML'
            if guess_type in FileTypeDetector.IMAGE_MIME_TYPES:
                return 'IMAGE'
                
        # If magic bytes didn't start with <, it's likely binary (Image/PDF)
        # Check standard binary signatures
        if content.startswith(b'%PDF'):
            return 'IMAGE' # PDF
        if content.startswith(b'\xff\xd8\xff'):
            return 'IMAGE' # JPEG
        if content.startswith(b'\x89PNG\r\n\x1a\n'):
            return 'IMAGE' # PNG
            
        # Default fallback: 
        # If it looks like text but not XML? -> Error
        # If it looks binary? -> Assume Image and let preprocessing fail if invalid
        
        # For now, strict approach:
        raise ValueError(f"Unsupported file type for file: {filename}")

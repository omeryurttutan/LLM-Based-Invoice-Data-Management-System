from typing import Tuple, Optional, BinaryIO
import io
from PIL import Image
from app.core.exceptions import UnsupportedFormatError, CorruptedFileError
from app.core.logging import logger

class ImageLoader:
    """Handles loading of images from bytes/files and format (magic byte) detection."""
    
    # Magic bytes for supported formats
    MAGIC_BYTES = {
        b'\xFF\xD8\xFF': "image/jpeg",
        b'\x89\x50\x4E\x47': "image/png",
        b'%PDF': "application/pdf",
        b'\x49\x49\x2A\x00': "image/tiff",
        b'\x4D\x4D\x00\x2A': "image/tiff",
        b'RIFF': "image/webp", # Partial check, WEBP is at offset 8
        b'\x42\x4D': "image/bmp"
    }

    @classmethod
    def detect_format(cls, content: bytes) -> Optional[str]:
        """Detect MIME type based on magic bytes."""
        if content.startswith(b'RIFF') and content[8:12] == b'WEBP':
             return "image/webp"
             
        for magic, mime in cls.MAGIC_BYTES.items():
            if content.startswith(magic):
                return mime
        return None

    @classmethod
    def load_image(cls, file_content: bytes) -> Tuple[Image.Image, str]:
        """
        Load an image from bytes.
        Returns Tuple[Pillow Image, detected format].
        Raises UnsupportedFormatError or CorruptedFileError.
        """
        detected_format = cls.detect_format(file_content)
        
        if not detected_format:
            # Fallback: try letting Pillow identify it, but be strict about supported types
            try:
                with Image.open(io.BytesIO(file_content)) as img:
                    fmt = img.format.lower() if img.format else "unknown"
                    if fmt in ["jpeg", "jpg"]: return img.copy(), "image/jpeg"
                    if fmt == "png": return img.copy(), "image/png"
                    if fmt == "webp": return img.copy(), "image/webp"
                    if fmt in ["tiff", "tif"]: return img.copy(), "image/tiff"
                    if fmt == "bmp": return img.copy(), "image/bmp"
                    # PDF is handled separately usually, but Pillow can open some PDFs (ghostscript)
                    # We prefer PyMuPDF for PDFs, so if it's a PDF passed here it might fail or return just one page
            except Exception:
                pass
            
            logger.warning("unsupported_format_detected", format=detected_format)
            raise UnsupportedFormatError(f"Unsupported or unrecognized file format")

        if detected_format == "application/pdf":
             raise UnsupportedFormatError("PDF files should be processed via PdfConverter", format="application/pdf")

        try:
            img = Image.open(io.BytesIO(file_content))
            img.load() # Force load pixel data
            return img, detected_format
        except Exception as e:
            logger.error("image_load_failed", error=str(e))
            raise CorruptedFileError(f"Failed to load image: {str(e)}")

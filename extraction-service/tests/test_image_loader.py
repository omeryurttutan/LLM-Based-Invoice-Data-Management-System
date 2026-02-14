import pytest
import io
from PIL import Image
from app.services.preprocessing.image_loader import ImageLoader
from app.core.exceptions import UnsupportedFormatError

class TestImageLoader:
    
    def test_detect_jpeg(self, test_image_bytes):
        fmt = ImageLoader.detect_format(test_image_bytes)
        assert fmt == "image/jpeg"

    def test_detect_png(self, test_png_bytes):
        fmt = ImageLoader.detect_format(test_png_bytes)
        assert fmt == "image/png"

    def test_detect_pdf(self, test_pdf_bytes):
        fmt = ImageLoader.detect_format(test_pdf_bytes)
        assert fmt == "application/pdf"

    def test_load_jpeg(self, test_image_bytes):
        img, fmt = ImageLoader.load_image(test_image_bytes)
        assert isinstance(img, Image.Image)
        assert fmt == "image/jpeg"
        assert img.format == "JPEG"

    def test_load_png(self, test_png_bytes):
        img, fmt = ImageLoader.load_image(test_png_bytes)
        assert isinstance(img, Image.Image)
        assert fmt == "image/png"

    def test_unsupported_format(self):
        # Text file masquerading as image
        content = b"This is not an image"
        with pytest.raises(UnsupportedFormatError):
            ImageLoader.load_image(content)

    def test_load_pdf_raises_error(self, test_pdf_bytes):
        # Loader should reject PDF and suggest PdfConverter
        with pytest.raises(UnsupportedFormatError) as exc:
            ImageLoader.load_image(test_pdf_bytes)
        assert "PdfConverter" in str(exc.value)

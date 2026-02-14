import pytest
from PIL import Image
from app.services.preprocessing.pdf_converter import PdfConverter
from app.core.exceptions import PDFConversionError

class TestPdfConverter:

    def test_convert_single_page(self, test_pdf_bytes):
        images = PdfConverter.convert_to_images(test_pdf_bytes)
        assert len(images) == 1
        assert isinstance(images[0], Image.Image)

    def test_convert_multipage_default(self, test_multipage_pdf_bytes):
        # Default selects first page
        images = PdfConverter.convert_to_images(test_multipage_pdf_bytes)
        assert len(images) == 1

    def test_convert_multipage_all(self, test_multipage_pdf_bytes):
        images = PdfConverter.convert_to_images(test_multipage_pdf_bytes, page_selection="all")
        assert len(images) == 3

    def test_convert_multipage_selection(self, test_multipage_pdf_bytes):
        images = PdfConverter.convert_to_images(test_multipage_pdf_bytes, page_selection="1,3")
        assert len(images) == 2

    def test_invalid_pdf(self):
        with pytest.raises(PDFConversionError):
            PdfConverter.convert_to_images(b"not a pdf")
            
    def test_dpi_scaling(self, test_pdf_bytes):
        # Default dpi=200
        imgs_default = PdfConverter.convert_to_images(test_pdf_bytes, dpi=72)
        imgs_high = PdfConverter.convert_to_images(test_pdf_bytes, dpi=144)
        
        # 144 dpi should be approx 2x size of 72 dpi
        assert imgs_high[0].width >= imgs_default[0].width * 1.9

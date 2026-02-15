import pytest
from unittest.mock import MagicMock, patch
from PIL import Image, ImageChops
import io
from app.services.preprocessing.image_enhancer import ImageEnhancer
from app.services.preprocessing.image_optimizer import ImageOptimizer
from app.services.preprocessing.image_loader import ImageLoader
from app.services.preprocessing.pipeline import PreprocessingPipeline
from app.models.preprocessing import ProcessingOptions
from app.core.exceptions import PreprocessingError, FileTooLargeError

class TestImageEnhancer:
    def test_fix_orientation_no_exif(self):
        # Create a simple image without EXIF
        img = Image.new('RGB', (100, 50), color='red')
        result_img, rotated, degrees = ImageEnhancer.fix_orientation(img)
        assert not rotated
        assert degrees == 0
        assert result_img == img

    def test_fix_orientation_with_rotation(self, rotated_image_bytes):
        # Load the rotated image from fixture
        img = Image.open(io.BytesIO(rotated_image_bytes))
        result_img, rotated, degrees = ImageEnhancer.fix_orientation(img)
        
        # Depending on how PIL handles it, check if it was actually rotated
        # value 6 means 90 CW.
        assert rotated
        assert degrees == 90
        # The result image should have swapped dimensions if rotated 90 deg
        # Original was (100, 50) -> Rotated should be (50, 100)
        assert result_img.size == (50, 100)

    def test_deskew_image_placeholder(self):
        img = Image.new('RGB', (100, 100), color='blue')
        result_img, deskewed, angle = ImageEnhancer.deskew_image(img)
        assert not deskewed
        assert angle == 0.0
        assert result_img == img

    def test_enhance_image_contrast(self):
        # Create a low contrast image (gray)
        img = Image.new('L', (100, 100), color=100) 
        options = ProcessingOptions(enhance_contrast=True, enhance_sharpness=False)
        
        # Mock ImageStat to force low contrast/brightness detection
        with patch('app.services.preprocessing.image_enhancer.ImageStat.Stat') as mock_stat:
            mock_stat.return_value.mean = [50] # Low brightness
            mock_stat.return_value.stddev = [10] # Low contrast
            
            result_img, enhanced = ImageEnhancer.enhance_image(img, options)
            assert enhanced
            # We can't easily assert pixel values without exact math, but we know it should have changed

    def test_enhance_image_no_change_needed(self):
         # Create a "good" image
        img = Image.new('L', (100, 100), color=150)
        options = ProcessingOptions(enhance_contrast=True, enhance_sharpness=False)
        
        with patch('app.services.preprocessing.image_enhancer.ImageStat.Stat') as mock_stat:
            mock_stat.return_value.mean = [150] # Good brightness
            mock_stat.return_value.stddev = [100] # Good contrast
            
            result_img, enhanced = ImageEnhancer.enhance_image(img, options)
            # Depending on logic, it might not enhance. 
            # In current logic: if contrast < 40 it enhances. 100 > 40.
            # Brightness 150 is between 100 and 220.
            assert not enhanced
            assert result_img == img

class TestImageOptimizer:
    def test_resize_image_larger(self):
        img = Image.new('RGB', (2000, 1000), color='white')
        max_dim = 1000
        resized_img, was_resized = ImageOptimizer.resize_image(img, max_dim)
        
        assert was_resized
        assert resized_img.width == 1000
        assert resized_img.height == 500

    def test_resize_image_smaller(self):
        img = Image.new('RGB', (500, 500), color='white')
        max_dim = 1000
        resized_img, was_resized = ImageOptimizer.resize_image(img, max_dim)
        
        assert not was_resized
        assert resized_img.size == (500, 500)

class TestImageLoader:
    def test_detect_format_jpeg(self, test_image_bytes):
        fmt = ImageLoader.detect_format(test_image_bytes)
        assert fmt == "image/jpeg"

    def test_detect_format_png(self, test_png_bytes):
        fmt = ImageLoader.detect_format(test_png_bytes)
        assert fmt == "image/png"

    def test_detect_format_pdf(self, test_pdf_bytes):
        fmt = ImageLoader.detect_format(test_pdf_bytes)
        assert fmt == "application/pdf"

    def test_load_image_valid(self, test_image_bytes):
        img, fmt = ImageLoader.load_image(test_image_bytes)
        assert isinstance(img, Image.Image)
        assert fmt == "image/jpeg"

    def test_load_image_invalid(self):
        with pytest.raises(PreprocessingError):
            ImageLoader.load_image(b"invalid_data")

class TestPreprocessingPipeline:
    @pytest.mark.async_api
    async def test_process_end_to_end_image(self, test_image_bytes):
        pipeline = PreprocessingPipeline()
        options = ProcessingOptions(
            auto_rotate=False, # simplfy
            enhance_contrast=False,
            enhance_sharpness=False
        )
        
        result = await pipeline.process(test_image_bytes, "test.jpg", options)
        
        assert result.mime_type == "image/jpeg" # Default target
        assert result.metadata.original_dimensions == (100, 100)
        assert len(result.image_data) > 0 # Base64 string
        assert result.processing_time_ms >= 0

    @pytest.mark.async_api
    async def test_process_pdf(self, test_pdf_bytes):
        pipeline = PreprocessingPipeline()
        options = ProcessingOptions()
        
        # Mock PdfConverter because we might not have poppler installed in test env
        with patch('app.services.preprocessing.pipeline.PdfConverter') as mock_converter:
             mock_img = Image.new('RGB', (100, 100), color='white')
             mock_converter.convert_to_images.return_value = [mock_img]
             
             result = await pipeline.process(test_pdf_bytes, "test.pdf", options)
             
             assert result.metadata.was_pdf
             assert result.metadata.pdf_page_count == 1
             mock_converter.convert_to_images.assert_called_once()

    @pytest.mark.async_api
    async def test_file_too_large(self):
        pipeline = PreprocessingPipeline()
        large_content = b"0" * (21 * 1024 * 1024) # 21MB
        
        with pytest.raises(FileTooLargeError):
            await pipeline.process(large_content, "large.jpg")

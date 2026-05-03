import pytest
from PIL import Image
import io
import sys
import os

# Ensure app modules are importable
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

from app.services.preprocessing_service import PreprocessingService

class TestPreprocessingService:
    
    def test_preprocess_image_resize(self, sample_image_bytes):
        """Test that large images are resized."""
        # Create a large image
        large_img = Image.new('RGB', (2000, 2000), color='white')
        buf = io.BytesIO()
        large_img.save(buf, format='JPEG')
        large_bytes = buf.getvalue()
        
        processed_bytes = PreprocessingService.preprocess_image(large_bytes)
        processed_img = Image.open(io.BytesIO(processed_bytes))
        
        # Default max dimension is usually 1024 or similar, verify it's smaller than original
        assert processed_img.width <= 1024
        assert processed_img.height <= 1024

    def test_preprocess_image_format(self, sample_image_bytes):
        """Test that images are converted to RGB/JPEG."""
        # Create PNG
        png_img = Image.new('RGBA', (100, 100), color=(255, 0, 0, 128))
        buf = io.BytesIO()
        png_img.save(buf, format='PNG')
        png_bytes = buf.getvalue()
        
        processed_bytes = PreprocessingService.preprocess_image(png_bytes)
        processed_img = Image.open(io.BytesIO(processed_bytes))
        
        assert processed_img.format == 'JPEG'
        assert processed_img.mode == 'RGB'

    def test_preprocess_image_grayscale(self):
        """Test handling of grayscale images."""
        gray_img = Image.new('L', (100, 100), color=128)
        buf = io.BytesIO()
        gray_img.save(buf, format='JPEG')
        gray_bytes = buf.getvalue()
        
        processed_bytes = PreprocessingService.preprocess_image(gray_bytes)
        processed_img = Image.open(io.BytesIO(processed_bytes))
        
        assert processed_img.mode == 'RGB'

    def test_preprocess_image_invalid_input(self):
        """Test handling of invalid input."""
        with pytest.raises(ValueError):
            PreprocessingService.preprocess_image(b"not an image")
        
        with pytest.raises(ValueError):
            PreprocessingService.preprocess_image(None)

    def test_preprocess_image_exif_rotation(self):
        """Test that EXIF rotation is applied."""
        # Create image with EXIF orientation 6 (90 CW)
        img = Image.new('RGB', (100, 50), color='red')
        exif = img.getexif()
        exif[0x0112] = 6 
        buf = io.BytesIO()
        img.save(buf, format='JPEG', exif=exif)
        rotated_bytes = buf.getvalue()
        
        processed_bytes = PreprocessingService.preprocess_image(rotated_bytes)
        processed_img = Image.open(io.BytesIO(processed_bytes))
        
        # After rotation correction, dimensions should be swapped (50x100) or similar logic 
        # depending on implementation. 
        # If preprocessor rotates it upright, width/height should reflect that.
        # Assuming original 100x50 rotated 90deg becomes 50x100.
        assert processed_img.width == 50
        assert processed_img.height == 100


import pytest
from PIL import Image, ImageEnhance
from app.services.preprocessing.image_enhancer import ImageEnhancer
from app.models.preprocessing import ProcessingOptions

class TestImageEnhancer:

    def test_fix_orientation(self, rotated_image_bytes):
        # Load raw without ops
        img = Image.open(io.BytesIO(rotated_image_bytes))
        
        fixed_img, rotated, degrees = ImageEnhancer.fix_orientation(img)
        assert rotated is True
        assert degrees == 90
        # Original was 100x50, rotated 90 deg should be 50x100
        assert fixed_img.size == (50, 100)

    def test_fix_orientation_no_exif(self, test_image_bytes):
        img = Image.open(io.BytesIO(test_image_bytes))
        fixed_img, rotated, degrees = ImageEnhancer.fix_orientation(img)
        assert rotated is False
        assert degrees == 0

    def test_enhancement_applied(self, test_image_bytes):
        img = Image.open(io.BytesIO(test_image_bytes))
        options = ProcessingOptions(enhance_contrast=True, enhance_sharpness=True)
        
        enhanced_img, applied = ImageEnhancer.enhance_image(img, options)
        # Verify it runs without error and returns image
        assert isinstance(enhanced_img, Image.Image)
        # Note: 'applied' depends on heuristics (histogram), so for a uniform color image 
        # it might not apply anything depending on logic.
        # Our logic checks brightness < 100 or > 220, or contrast < 40.
        # Red image (255,0,0) has L=76, so it's dark? RGB->L: 0.299*R + ... ~ 76.
        # So brightness < 100 -> enhance(1.2).
        assert applied is True
import io

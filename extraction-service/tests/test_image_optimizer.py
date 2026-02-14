import pytest
from PIL import Image
from app.services.preprocessing.image_optimizer import ImageOptimizer

class TestImageOptimizer:

    def test_resize_image(self):
        img = Image.new('RGB', (2000, 1000), color='white')
        resized, applied = ImageOptimizer.resize_image(img, max_dimension=1000)
        
        assert applied is True
        assert resized.width == 1000
        assert resized.height == 500 # Aspect ratio preserved

    def test_resize_not_needed(self):
        img = Image.new('RGB', (500, 500), color='white')
        resized, applied = ImageOptimizer.resize_image(img, max_dimension=1000)
        
        assert applied is False
        assert resized.size == (500, 500)

    def test_optimize_jpeg(self):
        img = Image.new('RGB', (500, 500), color='white')
        # Draw some noise to make it compressible
        from PIL import ImageDraw
        d = ImageDraw.Draw(img)
        d.line((0,0) + (500,500), fill=128)
        
        data, ratio = ImageOptimizer.optimize_image(img, target_format="image/jpeg", max_size_kb=50)
        assert len(data) > 0
        assert data.startswith(b'\xFF\xD8')

    def test_optimize_png(self):
        img = Image.new('RGB', (100, 100), color='red')
        data, ratio = ImageOptimizer.optimize_image(img, target_format="image/png", max_size_kb=500)
        assert data.startswith(b'\x89PNG')

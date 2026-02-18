from typing import Tuple, Optional
from PIL import Image
import io
from app.models.preprocessing import ProcessingOptions
from app.core.logging import logger

class ImageOptimizer:
    """Handles resizing and file format optimization."""

    @staticmethod
    def resize_image(img: Image.Image, max_dimension: int) -> Tuple[Image.Image, bool]:
        """
        Resize image if it exceeds max dimensions, maintaining aspect ratio.
        """
        if max(img.width, img.height) <= max_dimension:
            return img, False

        try:
            # Use thumbnail for faster, aspect-ratio preserving resize
            # Thumbnail modifies in-place, so we copy first if we want to be safe, 
            # though in this pipeline we own the image.
            # But let's be safe and copy to avoid side effects if the caller reuses the original image.
            img = img.copy()
            img.thumbnail((max_dimension, max_dimension), Image.Resampling.LANCZOS)
            return img, True
        except Exception as e:
            logger.warning("resize_failed", error=str(e))
            return img, False

    @staticmethod
    def optimize_image(
        img: Image.Image, 
        target_format: str, 
        max_size_kb: int, 
        initial_quality: int = 85
    ) -> Tuple[bytes, float]:
        """
        Compress image to target size.
        Returns (image_bytes, compression_ratio).
        """
        original_size_approx = len(img.tobytes()) # Very rough approx for raw pixels
        
        fmt = target_format.split('/')[-1].upper()
        if fmt == "JPG": fmt = "JPEG"
        if fmt == "PDF": fmt = "JPEG" # Fallback to JPEG for simple image representation if requested
        
        quality = initial_quality
        min_quality = 50
        step = 10
        
        buffer = io.BytesIO()
        
        # Convert RGBA to RGB if saving as JPEG
        if fmt == "JPEG" and img.mode == "RGBA":
            img = img.convert("RGB")
            
        try:
            # First attempt
            save_args = {"quality": quality, "optimize": True} if fmt == "JPEG" else {"optimize": True}
            img.save(buffer, format=fmt, **save_args)
            size_kb = buffer.tell() / 1024
            
            # Iterative optimization for JPEG
            while size_kb > max_size_kb and fmt == "JPEG" and quality > min_quality:
                quality -= step
                buffer.seek(0)
                buffer.truncate()
                img.save(buffer, format=fmt, quality=quality, optimize=True)
                size_kb = buffer.tell() / 1024
            
            # If still too large, we might need to verify or accept it (or resize further, but handled separately)
            final_bytes = buffer.getvalue()
            
            # Ratio: Original (raw uncompressed RAM usage) vs Final compressed
            # A better "compression ratio" is usually input file size vs output file size, 
            # but we might not have input size here easily without passing it.
            # Using raw pixel size as baseline.
            raw_size = img.width * img.height * (3 if img.mode == 'RGB' else 4)
            ratio = raw_size / len(final_bytes) if len(final_bytes) > 0 else 0
            
            return final_bytes, round(ratio, 2)
            
        except Exception as e:
             logger.error("optimization_failed", error=str(e))
             # Fallback to simple save
             buffer = io.BytesIO()
             img.save(buffer, format=fmt)
             return buffer.getvalue(), 1.0
